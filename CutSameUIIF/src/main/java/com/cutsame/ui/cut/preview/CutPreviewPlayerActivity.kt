package com.cutsame.ui.cut.preview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceView
import android.view.View
import com.bytedance.ies.cutsame.cut_android.TemplateError
import com.bytedance.ies.cutsame.util.SizeUtil
import com.cutsame.solution.player.BasePlayer
import com.cutsame.solution.player.GetImageListener
import com.cutsame.ui.CutSameUiIF
import com.cutsame.ui.R
import com.cutsame.ui.customview.GlobalDebounceOnClickListener
import com.cutsame.ui.customview.OnFloatSliderChangeListener
import com.cutsame.ui.customview.SpacesItemDecoration
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.cut.textedit.PlayerAnimateHelper
import com.cutsame.ui.cut.textedit.PlayerTextBoxData
import com.cutsame.ui.cut.textedit.PlayerTextEditItemData
import com.cutsame.ui.cut.textedit.controller.PlayerTextEditController
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditListenerAdapter
import com.cutsame.ui.cut.textedit.listener.PlayerTextItemThumbBitmapListener
import com.cutsame.ui.cut.videoedit.customview.TipDialog
import com.cutsame.ui.utils.runOnUiThread
import com.cutsame.ui.utils.showErrorTipToast
import com.ss.android.ugc.cut_log.LogUtil
import com.ss.android.ugc.cut_ui.MediaItem
import com.ss.android.ugc.cut_ui.MediaItem.Companion.TYPE_VIDEO
import kotlinx.android.synthetic.main.activity_cut_player.*
import kotlinx.android.synthetic.main.fragment_cut_video_list.*
import kotlinx.android.synthetic.main.fragment_cut_video_play.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*

private const val TAG = "cut.PreviewPlayActivity"

@ExperimentalStdlibApi
class CutPreviewPlayerActivity : CutPlayerActivity() {
    private var clickPosition = -1
    private var clickProgress = -1L
    private var selectMediaItem: MediaItem? = null
    private var hasInitUi = false
    private var playerTextEditController: PlayerTextEditController = PlayerTextEditController()
    private var moveProgress = false
    private var volumeBefore = Int.MAX_VALUE
    private var isPlaying = true
        set(value) {
            field = value
            changePlayIcon(value)
        }

    private var editorMoreTip: TipDialog? = null
    private val editorMoreTipOperationListener = object : TipDialog.DialogOperationListener {
        override fun onClickSure() {
            val data = cutSamePlayer?.getModelData()
            data?.let {
                val createMoreEditorUIIntent =
                    CutSameUiIF.createMoreEditorUIIntent(this@CutPreviewPlayerActivity)
                if (createMoreEditorUIIntent == null) {
                    showTipToast(resources.getString(R.string.cutsame_edit_tip_not_find_edit_more))
                } else {
                    val writeFilePath = writeJsonFile(data)
                    if (TextUtils.isEmpty(writeFilePath)) {
                        showTipToast(resources.getString(R.string.cutsame_edit_tip_create_json_file_failure))
                        return
                    }
                    createMoreEditorUIIntent.putExtra("data", writeFilePath)
                    startActivity(createMoreEditorUIIntent)
                }
            }
        }

        override fun onClickCancel() {
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    override fun onDestroy() {
        super.onDestroy()
        playerTextEditController.release()
    }

    @SuppressLint("CI_ByteDanceKotlinRules_Not_Allow_findViewById_Invoked_In_UI")
    override fun onPlayerDataOk() {
        super.onPlayerDataOk()
        LogUtil.d(TAG, "onPlayerDataOk hasInitUi $hasInitUi")
        if (hasInitUi) {
            return
        }
        hasInitUi = true

        setContentView(R.layout.activity_cut_player)
        editorMoreTip = TipDialog.Builder(this)
            .setDialogOperationListener(editorMoreTipOperationListener)
            .setTitleText(getString(R.string.cutsame_edit_tip_more_edit))
            .setSureText(getString(R.string.cutsame_common_sure))
            .create()

        if (CutSameUiIF.hasEditUIIntent(this)) {
            LogUtil.d(TAG, "hasEditUIIntent")
            editMoreLayout.visibility = View.VISIBLE
        } else {
            LogUtil.d(TAG, "not hasEditUIIntent")
            editMoreLayout.visibility = View.GONE
        }

        val adapter = VideoListAdapter(mutableMediaItemList)
        videoRecyclerView.layoutManager =
            object : androidx.recyclerview.widget.LinearLayoutManager(this, HORIZONTAL, false) {
                override fun smoothScrollToPosition(
                    recyclerView: androidx.recyclerview.widget.RecyclerView?,
                    state: androidx.recyclerview.widget.RecyclerView.State?,
                    position: Int
                ) {
                    val linearSmoothScroller = object :
                        androidx.recyclerview.widget.LinearSmoothScroller(recyclerView!!.context) {
                        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                            return super.calculateSpeedPerPixel(displayMetrics) * 2
                        }

                        override fun calculateDxToMakeVisible(
                            view: View?,
                            snapPreference: Int
                        ): Int {
                            val layoutManager = this.layoutManager
                            return if (layoutManager != null && layoutManager.canScrollHorizontally()) {
                                val params =
                                    view!!.layoutParams as androidx.recyclerview.widget.RecyclerView.LayoutParams
                                val left = layoutManager.getDecoratedLeft(view) - params.leftMargin
                                val right =
                                    layoutManager.getDecoratedRight(view) + params.rightMargin
                                val start = layoutManager.paddingLeft
                                val end = layoutManager.width - layoutManager.paddingRight
                                return start + (end - start) / 2 - (right - left) / 2 - left
                            } else {
                                0
                            }
                        }
                    }
                    linearSmoothScroller.targetPosition = position
                    startSmoothScroll(linearSmoothScroller)
                }
            }
        videoRecyclerView.setHasFixedSize(true)
        videoRecyclerView.addItemDecoration(
            SpacesItemDecoration(
                0,
                SizeUtil.dp2px(16f),
                rowCountLimit = 1
            )
        )
        videoRecyclerView.adapter = adapter

        playSliderView.setOnSliderChangeListener(object : OnFloatSliderChangeListener() {
            var isCurrentPlay = false // 记忆拖动前的播放状态，因为seek时总会先暂停播放
            override fun onChange(value: Int) {
                cutSamePlayer?.seeking(value)
                currentPlayTime.text = formatTime(value)
            }

            override fun onBegin(value: Int) {
                isCurrentPlay = isPlaying
                moveProgress = true
                cutSamePlayer?.pause()
            }

            override fun onFreeze(value: Int) {
                runOnUiThread(200L) {
                    moveProgress = false
                }
                if (isForeground) {
                    cutSamePlayer?.seekTo(value, isCurrentPlay)
                }
            }
        })

        adapter.setItemClickListener(object : VideoListAdapter.ItemClickListener {
            override fun onItemClick(item: MediaItem, position: Int) {
                cutSamePlayer?.pause()
                cutSamePlayer?.seekTo(item.targetStartTime.toInt(), false)
                clickPosition = position
                clickProgress = item.targetStartTime
                selectMediaItem = item
                if (adapter.isSelect(position)) {
                    LogUtil.d(
                        TAG,
                        "onItemClick selectMediaItem=${selectMediaItem?.source}, type=${selectMediaItem?.type}"
                    )
                    changeVolumeLayoutState(selectMediaItem?.type == TYPE_VIDEO)
                    showSlotEditLayout()
                }
            }
        })

        adapter.setCurrentIndexChangeListener(object : VideoListAdapter.CurrentIndexChangeListener {
            override fun onCurrentIndexChange(item: MediaItem, index: Int) {
                selectMediaItem = item
                hideSlotEditLayout()
                hideSlotVolumeLayout()
            }
        })
        initListener()
    }

    private fun changeCurrentMaterialVolume(value: Int) {
        selectMediaItem?.let {
            cutSamePlayer?.setVolume(it.materialId, value.toFloat() / 100)
        }
    }

    private fun initListener() {
        editMoreLayout.setOnClickListener(object :
            GlobalDebounceOnClickListener(delayInterval = 1500) {
            override fun doClick(v: View) {
                editorMoreTip?.show()
            }
        })

        playStatusBtn.setGlobalDebounceOnClickListener {
            LogUtil.d(TAG, "playStatusBtn isPlaying=${isPlaying}")
            if (isPlaying) {
                cutSamePlayer?.pause()
            } else {
                cutSamePlayer?.start()
            }
        }

        closeView.setGlobalDebounceOnClickListener {
            finish()
        }

        exportView.setGlobalDebounceOnClickListener {
            launchCompile()
        }

        editLayoutBackIv.setGlobalDebounceOnClickListener {
            //退出slot编辑界面
            hideSlotEditLayout()
        }

//        editVolumeLayout.setGlobalDebounceOnClickListener {
//            //调整音量
//            LogUtil.d(
//                TAG,
//                "editVolumeLayout selectMediaItem=${selectMediaItem?.source}, type=${selectMediaItem?.type}"
//            )
//            selectMediaItem?.let {
//                if (it.type == TYPE_VIDEO) {
//                    showSlotVolumeLayout()
//                }
//            }
//        }

        editClipLayout.setGlobalDebounceOnClickListener {
            //裁剪
            LogUtil.d(TAG, "editClipLayout selectMediaItem=$selectMediaItem")
            selectMediaItem?.let {
                launchClip(it)
            }
        }

        editReplaceLayout.setGlobalDebounceOnClickListener {
            //替换
            LogUtil.d(TAG, "editReplaceLayout selectMediaItem=$selectMediaItem")
            selectMediaItem?.let {
                launchMediaReplace(it)
            }
        }

        sureVolumeEditIv.setGlobalDebounceOnClickListener {
            hideSlotVolumeLayout()
        }

        closeVolumeEditIv.setGlobalDebounceOnClickListener {
            if (volumeBefore != Int.MAX_VALUE) {
                changeCurrentMaterialVolume(volumeBefore)
                volumeBefore = Int.MAX_VALUE
            }
            hideSlotVolumeLayout()
        }
    }

    private fun showSlotEditLayout() {
        if (slotEditLayout.visibility == View.GONE) {
            slotEditLayout.visibility = View.VISIBLE
        }
    }

    private fun hideSlotEditLayout() {
        slotEditLayout.visibility = View.GONE
    }
//
//    private fun showSlotVolumeLayout() {
//        LogUtil.d(TAG, "showSlotVolumeLayout volumeLayout=${volumeLayout.visibility}")
//        if (volumeLayout.visibility == View.GONE) {
//            selectMediaItem?.let {
//                val volume = cutSamePlayer?.getVolume(it.materialId)?.times(100)?.toInt() ?: 0
//                volumeBefore = volume//记录之前的音量，放弃时需要恢复
//                volumeSliderView.currPosition = volume.toFloat()
//            }
//            volumeLayout.visibility = View.VISIBLE
//        }
//    }

    private fun hideSlotVolumeLayout() {
        volumeLayout.visibility = View.GONE
    }

    private fun changeVolumeLayoutState(enable: Boolean) {
        LogUtil.d(TAG, "changeVolumeLayoutState enable=$enable")
        if (enable) {
            editVolumeIconView.imageAlpha = 255
            editVolumeView.setTextColor(Color.WHITE)
        } else {
            editVolumeIconView.imageAlpha = 100
            editVolumeView.setTextColor(Color.GRAY)
        }
    }

    private fun changePlayIcon(value: Boolean) = runOnUiThread {
        if (playerTextEditController.isShowing()) {
            return@runOnUiThread
        }
        if (value) {
            playStatusBtn.setImageResource(R.drawable.icon_video_stop)
        } else {
            playStatusBtn.setImageResource(R.drawable.icon_video_play)
        }
    }

    private fun writeJsonFile(data: String): String {
        val jsonFile = File(this.filesDir, "model_data.txt")
        if (jsonFile.exists()) {
            jsonFile.delete()
        }
        try {
            jsonFile.createNewFile()
        } catch (e: IOException) {
            LogUtil.d(TAG, "writeJsonFile IOException ${e.message}")
            return ""
        }
        LogUtil.d(TAG, "writeJsonFile ${jsonFile.absolutePath}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.write(jsonFile.toPath(), data.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING)
        } else {
            var fileOutputStream: FileOutputStream? = null
            try {
                fileOutputStream = FileOutputStream(jsonFile)
                fileOutputStream.write(data.toByteArray())
            } catch (e: FileNotFoundException) {
            } catch (e: IOException) {
            } finally {
                try {
                    fileOutputStream?.close()
                } catch (e: IOException) {
                }
            }
        }
        LogUtil.d(TAG, "writeJsonFile finish${jsonFile.absolutePath}")
        return jsonFile.absolutePath
    }

    override fun onPlayerPlaying(isPlaying: Boolean) {
        super.onPlayerPlaying(isPlaying)
        runOnUiThread {
            this.isPlaying = isPlaying
            playerTextEditController.onPlaying(isPlaying)
        }
    }

    override fun onPlayerPrepareOk() {
        super.onPlayerPrepareOk()
        runOnUiThread {
            exportView.isEnabled = true
            //初始化文字功能
            initTextEditController()
        }
    }

    override fun onPlayerError(errorCode: Int, errorMessage: String?) {
        super.onPlayerError(errorCode, errorMessage)
        runOnUiThread {
            videoError.visibility = View.VISIBLE
            videoError.text = getString(R.string.cutsame_common_video_error, errorCode)
        }
    }

    override fun onPlayerFirstFrameOk() {
        super.onPlayerFirstFrameOk()
        runOnUiThread {
            val duration = cutSamePlayer?.getDuration()?.toInt() ?: 0
            endPlayTime.text = formatTime(duration)
            playSliderView.setRange(0, duration)
        }
    }

    override fun onPlayerProgress(progress: Long) {
        super.onPlayerProgress(progress)
        runOnUiThread {
            currentPlayTime.text = formatTime(progress.toInt())
            if (!moveProgress) {
                Log.i(TAG, " | onPlayerProgress: " + progress + " - " + progress.toFloat())
                //视频播放时动态更新全屏状态下的进度条
//                val position: Float = 100F * progress / (cutSamePlayer?.getDuration()?.toFloat()!!)
                playSliderView.currPosition = progress.toFloat()
            }

            videoRecyclerView.adapter?.apply {
                this as VideoListAdapter
                val indexSet = if (clickPosition >= 0 && clickProgress == progress) {
                    val position = clickPosition
                    clickPosition = -1
                    updatePosition(position)
                } else {
                    update(progress)
                }
                indexSet?.maxOrNull()?.let { max ->
                    if (!videoRecyclerView.isPressed
                        && videoRecyclerView.scrollState != androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
                        && videoRecyclerView.scrollState != androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING
                    ) {
                        videoRecyclerView.smoothScrollToPosition(Math.min(itemCount, max))
                    }
                }
            }
        }
    }

    override fun onPlayerDestroy() {
        LogUtil.d(TAG, "onPlayerDestroy")
        runOnUiThread {
            exportView?.isEnabled = false
        }
        playSliderView.currPosition = 0F
        super.onPlayerDestroy()
    }

    override fun onPlayerMediaItemUpdate(item: MediaItem) {
        super.onPlayerMediaItemUpdate(item)
        // update the list of video thumb in the bottom
        videoRecyclerView?.adapter?.apply { // 数据变更，不代表view已经准备好，可能为 null
            this as VideoListAdapter
            updateOne(item)
        }
    }

    private fun initTextEditController() {
        bottom_layout.post {
            PlayerAnimateHelper.setViewHeight(videoSurface)
        }

        playerTextEditController.init(this, root_view, object : PlayerTextEditListenerAdapter() {
            override fun clickPlay(): Boolean {
                val playStatus = cutSamePlayer?.getState() == BasePlayer.PlayState.PLAYING
                return if (playStatus) {
                    cutSamePlayer?.pause()
                    false
                } else {
                    cutSamePlayer?.start()
                    true
                }
            }

            override fun isPlaying(): Boolean {
                return cutSamePlayer?.getState() == BasePlayer.PlayState.PLAYING
            }

            override fun controlVideoPlaying(play: Boolean) {
                if (play) cutSamePlayer?.getState() else cutSamePlayer?.pause()
            }

            override fun getCurPlayPosition(): Long {
                return if (cutSamePlayer?.getCurrentPosition() == null) 0 else cutSamePlayer?.getCurrentPosition()!!
            }

            override fun inputTextChange(itemData: PlayerTextEditItemData?, text: String?) {
                if (itemData == null || text == null) {
                    return
                }
                itemData.saltId?.apply {
                    updateTextItem(this, text)
                }
            }

            override fun getCurItemTextBoxData(data: PlayerTextEditItemData?): PlayerTextBoxData? {
                if (data == null || TextUtils.isEmpty(data.saltId)) {
                    return null
                }
                val rectF = RectF()
                cutSamePlayer?.getTextSegment(data.saltId ?: "", rectF)
                return PlayerTextBoxData.createData(rectF, data.getTextAngle())
            }

            override fun getCanvasSize(): IntArray? {
                if (cutSamePlayer?.getConfigCanvasSize() == null) {
                    return null
                }

                val canvasW = cutSamePlayer?.getConfigCanvasSize()!!.width
                val canvasH = cutSamePlayer?.getConfigCanvasSize()!!.height
                val surfaceW = videoSurface.width
                val surfaceH = videoSurface.height
                val surfaceRatio = surfaceW.toFloat() / surfaceH
                val canvasRatio = canvasW.toFloat() / canvasH
                val size = IntArray(2)
                if (surfaceRatio > canvasRatio) {
                    size[1] = surfaceH
                    size[0] = (surfaceH * canvasRatio).toInt()
                } else {
                    size[0] = surfaceW
                    size[1] = (surfaceW / canvasRatio).toInt()
                }
                return size
            }

            override fun selectTextItem(
                data: PlayerTextEditItemData?,
                pos: Int,
                seekDone: ((ret: Int) -> Unit)?
            ) {
                if (data == null) {
                    return
                }
                val frameStartTime = data.getFrameTime()
                if (frameStartTime >= 0) {
                    //seek 到文字那帧，并且暂停
                    cutSamePlayer?.seekTo(
                        frameStartTime.toInt(), false
                    ) {
                        seekDone?.invoke(it)
                    }
                }
            }

            override fun controlTextAnimate(slatId: String?, enable: Boolean) {
                if (!TextUtils.isEmpty(slatId)) {
                    cutSamePlayer?.setTextAnimSwitch(slatId, enable)
                }
            }

            override fun getItemFrameBitmap(
                times: IntArray?,
                width: Int,
                height: Int,
                listener: PlayerTextItemThumbBitmapListener?
            ) {
                if (times == null || times.isEmpty()) {
                    listener?.frameBitmap("", null)
                    return
                }
                cutSamePlayer?.getVideoFrameWithTime(
                    times,
                    width,
                    height,
                    object : GetImageListener {
                        override fun onGetImageData(
                            bytes: ByteArray?,
                            pts: Int,
                            width: Int,
                            height: Int,
                            score: Float
                        ) {
                            if (bytes != null) {
                                val bmp =
                                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                bmp.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
                                listener?.frameBitmap(pts.toString(), bmp)
                            } else {
                                cutSamePlayer?.cancelGetVideoFrames() // 取帧结束后需要cancel，不然会停留在取帧状态
                                listener?.frameBitmap("", null)
                            }
                        }

                    })
            }
        })

        playerTextEditController.setScaleListener(object :
            PlayerAnimateHelper.PlayerSurfaceScaleListener {
            override fun scaleEnd(
                targetScaleW: Float,
                targetScaleH: Float,
                targetTranx: Int,
                targetTranY: Int,
                isScaleDown: Boolean
            ) {
                if (!isScaleDown) {
                    cutSamePlayer?.start()
                }
            }

            override fun scale(
                scaleW: Float,
                scaleH: Float,
                tranX: Int,
                tranY: Int,
                faction: Float,
                isScaleDown: Boolean
            ) {
            }
        })

        val mutableTextSegments = cutSamePlayer?.getTextItems()?.filter { it.mutable }
        if (mutableTextSegments.isNullOrEmpty()) {
            editTextIconView.setImageResource(R.drawable.ic_edit_text_disable)
            editTextView.setTextColor(Color.GRAY)
        } else {
            editTextIconView.setImageResource(R.drawable.ic_edit_text)
            editTextView.setTextColor(Color.WHITE)

        }
        playerTextEditController.updateDataList(mutableTextSegments)

        editTextLayout.setGlobalDebounceOnClickListener {
            if (templatePlayerErrorCode == TemplateError.SUCCESS) {
                if (cutSamePlayer?.getTextItems()?.filter { it.mutable }.isNullOrEmpty()) {
                    showTipToast(resources.getString(R.string.cutsame_edit_tip_text_no_editable))
                } else {
                    playerTextEditController.showTextEditView()
                }
            }
        }
    }

    override fun onClipFinish(item: MediaItem?) {
        super.onClipFinish(item)
        selectMediaItem = item
    }

    override fun onReplaceFinish(item: MediaItem?) {
        super.onReplaceFinish(item)
        changeVolumeLayoutState(item?.type == TYPE_VIDEO)
        selectMediaItem = item
    }

    private fun showTipToast(msg: String) {
        showErrorTipToast(this, msg)
    }

    override fun getPlayerSurfaceView(): SurfaceView = videoSurface

    override fun onBackPressed() {
        if (playerTextEditController.isShowing()) {
            playerTextEditController.clickCancel()
            return
        }

        super.onBackPressed()
    }

    override fun finish() {
        videoSurfaceCover?.visibility = View.VISIBLE
        super.finish()
    }

    private fun formatTime(timeInMillis: Int) =
        String.format(
            Locale.getDefault(),
            "%02d:%02d", timeInMillis / 1000 / 60, timeInMillis / 1000 % 60
        )
}

