package com.cutsame.ui.cut.preview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceView
import android.view.View
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.bytedance.ies.cutsame.cut_android.TemplateError
import com.cutsame.solution.player.BasePlayer
import com.cutsame.solution.player.GetImageListener
import com.cutsame.ui.R
import com.cutsame.ui.customview.OnFloatSliderChangeListener
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.cut.adapter.MaterialPagerAdapter
import com.cutsame.ui.cut.lyrics.PlayerMaterialLyricsView
import com.cutsame.ui.cut.textedit.PlayerAnimateHelper
import com.cutsame.ui.cut.textedit.PlayerTextBoxData
import com.cutsame.ui.cut.textedit.PlayerTextEditItemData
import com.cutsame.ui.cut.textedit.controller.PlayerTextEditController
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditListenerAdapter
import com.cutsame.ui.cut.textedit.listener.PlayerTextItemThumbBitmapListener
import com.cutsame.ui.cut.textedit.view.PlayerMaterialTextEditView
import com.cutsame.ui.cut.videoedit.customview.PlayerMaterialVideoView
import com.cutsame.ui.utils.showErrorTipToast
import com.ola.chat.picker.album.model.MaterialEditType
import com.ss.android.ugc.cut_log.LogUtil
import com.ss.android.ugc.cut_ui.MediaItem
import kotlinx.android.synthetic.main.activity_cut_player.*
import kotlinx.android.synthetic.main.fragment_cut_video_list.*
import kotlinx.android.synthetic.main.fragment_cut_video_play.*
import kotlinx.android.synthetic.main.layout_materail_video_progress.*
import kotlinx.android.synthetic.main.layout_material_edit.*
import java.nio.ByteBuffer
import java.util.*

private const val TAG = "OlaCut.PreviewPlayActivity"

@ExperimentalStdlibApi
class CutPreviewPlayerActivity : CutPlayerActivity() {
    private var clickPosition = -1
    private var clickProgress = -1L
    private var selectMediaItem: MediaItem? = null
    private var hasInitUi = false
    private var moveProgress = false
    private var isPlaying = true
        set(value) {
            field = value
            changePlayIcon(value)
        }


    private lateinit var materialPagerAdapter: MaterialPagerAdapter

    private val playerMaterialVideoView: PlayerMaterialVideoView by lazy {
        PlayerMaterialVideoView(this)
    }
    private val playerMaterialTextEditView: PlayerMaterialTextEditView by lazy {
        PlayerMaterialTextEditView(this)
    }

    private val playerMaterialLyricsView: PlayerMaterialLyricsView by lazy {
        PlayerMaterialLyricsView(this)
    }

    private val playerTextEditController: PlayerTextEditController by lazy {
        PlayerTextEditController(playerMaterialTextEditView)
    }


    private val editTypeArray = MaterialEditType.values()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    override fun onDestroy() {
        super.onDestroy()
        playerTextEditController.release()
        materialEditViewPager?.removeOnPageChangeListener(mPageListener)
    }

    override fun onPlayerDataOk() {
        super.onPlayerDataOk()
        LogUtil.d(TAG, "onPlayerDataOk hasInitUi $hasInitUi")
        if (hasInitUi) {
            return
        }
        hasInitUi = true

        setContentView(R.layout.activity_cut_player)
        initPaper()
        playerMaterialVideoView.initData(mutableMediaItemList, { item, position, isSelect ->
            cutSamePlayer?.pause()
            cutSamePlayer?.seekTo(item.targetStartTime.toInt(), false)
            clickPosition = position
            clickProgress = item.targetStartTime
            selectMediaItem = item
            if (isSelect) {
                showSlotEditLayout()
            }
        }, { item, _ ->
            selectMediaItem = item
            hideSlotEditLayout()
        })
        floatSliderView.setOnSliderChangeListener(onFloatSliderChangeListener())
        initListener()
    }

    override fun onPlayerInitOk() {
        //关闭原声音效
        closeCutSameOriVolume()
    }

    private fun onFloatSliderChangeListener() = object : OnFloatSliderChangeListener() {
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
            com.cutsame.ui.utils.runOnUiThread(200L) {
                moveProgress = false
            }
            if (isForeground) {
                cutSamePlayer?.seekTo(value, isCurrentPlay)
            }
        }
    }

    /**
     * 初始化底部编辑视图
     */
    private fun initPaper() {
        materialPagerAdapter = MaterialPagerAdapter(
            applicationContext,
            editTypeArray,
            playerMaterialVideoView,
            playerMaterialTextEditView,
            playerMaterialLyricsView
        )
        materialEditViewPager.adapter = materialPagerAdapter
        materialEditViewPager.offscreenPageLimit = 3
        editTypeTab.setupWithViewPager(materialEditViewPager)
        materialEditViewPager.addOnPageChangeListener(mPageListener)
    }

    private fun initListener() {
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
    }

    private fun showSlotEditLayout() {
        if (slotEditLayout.visibility == View.GONE) {
            slotEditLayout.visibility = View.VISIBLE
        }
    }

    private fun hideSlotEditLayout() {
        slotEditLayout.visibility = View.GONE
    }

    private fun changePlayIcon(value: Boolean) = runOnUiThread {
        if (playerTextEditController.isShowing()) {
            return@runOnUiThread
        }
        playStatusBtn.setImageResource(if (value) R.drawable.icon_video_stop else R.drawable.icon_video_play)
    }

    override fun onPlayerPlaying(isPlaying: Boolean) {
        super.onPlayerPlaying(isPlaying)
        runOnUiThread {
            this.isPlaying = isPlaying
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
            floatSliderView.setRange(0, duration)
        }
    }

    override fun onPlayerProgress(progress: Long) {
        super.onPlayerProgress(progress)
        runOnUiThread {
            currentPlayTime.text = formatTime(progress.toInt())
            if (!moveProgress) {
                floatSliderView.currPosition = progress.toFloat()
            }
            // TODO: 2022/10/14 移动到视频组件内部
            playerMaterialVideoView.onPlayerProgress(progress)
        }
    }

    override fun onPlayerDestroy() {
        LogUtil.d(TAG, "onPlayerDestroy")
        runOnUiThread {
            exportView?.isEnabled = false
        }
        floatSliderView.currPosition = 0F
        super.onPlayerDestroy()
    }

    override fun onPlayerMediaItemUpdate(item: MediaItem) {
        super.onPlayerMediaItemUpdate(item)
        playerMaterialVideoView.onPlayerMediaItemUpdate(item)
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
                    cutSamePlayer?.seekTo(frameStartTime.toInt(), false) {
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
        playerTextEditController.updateDataList(mutableTextSegments)
    }

    /**
     * ViewPager滑动监听
     */
    var mPageListener: OnPageChangeListener = object : OnPageChangeListener {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
        }

        @SuppressLint("LongLogTag")
        override fun onPageSelected(currentPosition: Int) {
            Log.i("jackyang_onPageSelected", "当前页面是=$currentPosition")
            if (currentPosition == 1) {
                if (templatePlayerErrorCode == TemplateError.SUCCESS) {
                    if (cutSamePlayer?.getTextItems()?.filter { it.mutable }.isNullOrEmpty()) {
                        showTipToast(resources.getString(R.string.cutsame_edit_tip_text_no_editable))
                    } else {
                        playerTextEditController.showTextEditView()
                    }
                }
            }
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    override fun onClipFinish(item: MediaItem?) {
        super.onClipFinish(item)
        selectMediaItem = item
    }

    override fun onReplaceFinish(item: MediaItem?) {
        super.onReplaceFinish(item)
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

    private fun formatTime(timeInMillis: Int) = String.format(
        Locale.getDefault(),
        "%02d:%02d",
        timeInMillis / 1000 / 60,
        timeInMillis / 1000 % 60
    )
}

