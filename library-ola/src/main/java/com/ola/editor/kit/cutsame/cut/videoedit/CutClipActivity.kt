package com.ola.editor.kit.cutsame.cut.videoedit

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bytedance.ies.cutsame.util.MediaUtil
import com.bytedance.ies.cutsame.util.VideoMetaDataInfo
import com.cutsame.solution.CutSameSolution
import com.cutsame.solution.player.BasePlayer
import com.cutsame.solution.player.MediaPlayer
import com.cutsame.solution.player.PlayerStateListener
import com.ola.editor.kit.cutsame.CutSameUiIF
import com.ola.editor.kit.R
import com.ola.editor.kit.cutsame.customview.setGlobalDebounceOnClickListener
import com.ola.editor.kit.cutsame.cut.videoedit.customview.SingleSelectFrameView
import com.ola.editor.kit.cutsame.cut.videoedit.customview.TipDialog
import com.ola.editor.kit.cutsame.exten.FastMain
import com.ola.editor.kit.cutsame.utils.SizeUtil
import com.ss.android.ugc.cut_log.LogUtil
import com.ss.android.ugc.cut_ui.MediaItem
import kotlinx.android.synthetic.main.activity_cut_clip.*
import kotlinx.coroutines.*
import java.util.*

@ExperimentalCoroutinesApi
class CutClipActivity : AppCompatActivity(), CoroutineScope {
    private val TAG = "cut.CutClipActivity"
    override val coroutineContext = SupervisorJob() + FastMain

    private var videoInfo: VideoMetaDataInfo? = null
    private var curMediaItem: MediaItem? = null
    private var sourceStartTime = 0
    private var canvasSize = Point(0, 0)
    private var isPlaying = false
    private var isViewPrepared = false
    private var isTimeClipChanged = false
    private var isAreaClipChanged = false
    var mediaPlayer: MediaPlayer? = null

    private val areaClipHelper = CutClipAreaHelper().apply {
        var isCurPlaying = isPlaying
        var isTouchMoved = false
        onDown = {
            isCurPlaying = isPlaying
            isTouchMoved = false
            pausePlay()
        }
        onMove = { isTouch, scale, transX, transY ->
            isTouchMoved = isTouch || isTouchMoved
            if (isTouchMoved) {
                isAreaClipChanged = true
            }
            moveVideo(scale, transX, transY)
        }
        onUp = {
            if (isCurPlaying) {
                if (isTouchMoved)
                    startPlay()
                else {
                    pausePlay()
                }
            } else {
                if (isTouchMoved)
                    pausePlay()
                else {
                    startPlay()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_cut_clip)
        val mediaItem =
            intent.getParcelableExtra<MediaItem>(CutSameUiIF.ARG_DATA_CLIP_MEDIA_ITEM)
        mediaItem ?: return
        init(mediaItem)
    }

    private fun init(mediaItem: MediaItem) {
        initData(mediaItem)
        curMediaItem?.let {
            playIv.visibility = View.GONE
            initPlayer(it, canvasSize)
            initBtnClickListener()
            initViews(mediaItem)
            clipTimeTv.text = resources.getString(
                R.string.cutsame_edit_multi_video_time_select,
                String.format(Locale.getDefault(), "%.1f", it.duration.toFloat() / 1000)
            )
        }

        clipSurface.setGlobalDebounceOnClickListener {
            if (mediaPlayer?.getState() == BasePlayer.PlayState.PLAYING) {
                pausePlay()
            } else {
                startPlay()
            }
        }
    }

    private fun initViews(mediaItem: MediaItem) {
        initTimeClip(mediaItem)
        initBottomTitle(mediaItem.type)
    }

    private fun initData(mediaItem: MediaItem) {
        curMediaItem = mediaItem
        this.videoInfo = MediaUtil.getRealVideoMetaDataInfo(this@CutClipActivity, mediaItem.source)
        sourceStartTime = mediaItem.sourceStartTime.toInt()
        this.canvasSize = initSurfaceSize(mediaItem)
    }

    private fun initBtnClickListener() {
        backBtn.setGlobalDebounceOnClickListener {
            onBackPressed()
        }

        sureBtn.setGlobalDebounceOnClickListener {
            curMediaItem?.let {
                setResult(
                    Activity.RESULT_OK,
                    Intent().apply {
                        this.putExtra(
                            CutSameUiIF.ARG_DATA_CLIP_MEDIA_ITEM,
                            genEditedMediaItem(it)
                        )
                    })
                finish()
            }
        }
    }

    private fun initPlayer(mediaItem: MediaItem, canvasSize: Point) {
        isViewPrepared = false
        mediaPlayer = CutSameSolution.createMediaPlayer(clipSurface)
        mediaPlayer?.preparePlayBySingleMedia(mediaItem, canvasSize, object : PlayerStateListener {
            override fun onFirstFrameRendered() {
            }

            override fun onChanged(state: Int) {
                when (state) {
                    PlayerStateListener.PLAYER_STATE_PREPARED -> {
                        launch {
                            onPlayerPrepared()
                        }
                    }
                    PlayerStateListener.PLAYER_STATE_PLAYING -> {
                        isPlaying = true
                    }
                    else -> isPlaying = false
                }
            }

            override fun onPlayProgress(process: Long) {
                onPlayProgress(process, mediaItem.duration)
            }

            override fun onPlayEof() {
                val progress: Long = videoInfo?.duration?.toLong() ?: 0
                onPlayProgress(progress, mediaItem.duration)
            }

            override fun onPlayError(what: Int, extra: String) {
            }
        })

    }

    /**
     * player prepared，first frame already show on screen
     * this func will be invoke repeat
     */
    private fun onPlayerPrepared() {
        if (isViewPrepared) return
        isViewPrepared = true

        val mediaItem = curMediaItem
        mediaItem ?: return

        seekAndPlay(mediaItem.sourceStartTime.toInt())
        initAreaClip(mediaItem, canvasSize)
    }

    private fun onPlayProgress(progress: Long, clipDuration: Long) {
        launch {
            timeSelectView.changeProgress((progress - sourceStartTime) / clipDuration.toFloat())
            if (progress >= sourceStartTime + clipDuration) {
                seekAndPlay(sourceStartTime)
            }
        }
    }

    private fun initBottomTitle(mediaType: String) {
        bottomTitleTv.setText(
            if (mediaType == MediaItem.TYPE_PHOTO) {
                R.string.cutsame_edit_title_picture_cut
            } else {
                R.string.cutsame_edit_title_video_cut
            }
        )
    }

    private fun initAreaClip(mediaItem: MediaItem, canvasSize: Point) {
        val videoInfo = this.videoInfo
        videoInfo ?: return

        areaClipHelper.canvasSize = canvasSize
        val targetWidth: Float
        val targetHeight: Float
        // align canvas, area clip disable
        LogUtil.d(TAG, "mediaItem.alignMode=${mediaItem.alignMode}")
        if (mediaItem.alignMode == MediaItem.ALIGN_MODE_CANVAS) {
            clipMask.visibility = View.GONE
        } else { // align video, area clip enable
            clipMask.visibility = View.VISIBLE
            val boxMargin =
                if (mediaItem.type == MediaItem.TYPE_VIDEO)
                    Point(32, 24) else Point(32, 108)
            val maskBox = clipMask.selectBoxScaleFactor(
                mediaItem.width, mediaItem.height,
                boxMargin.x, boxMargin.y
            )
            targetWidth = maskBox.second
            targetHeight = maskBox.third
            areaClipHelper.calculateInnerScaleFactor(
                mediaItem.alignMode, targetWidth, targetHeight, mediaItem.crop, videoInfo
            )
            editorGestureLayout.setOnGestureListener(areaClipHelper.onGestureListener)
        }
    }

    /**
     * init time clip view and data
     */
    private fun initTimeClip(mediaItem: MediaItem) {
        if (mediaItem.type == MediaItem.TYPE_PHOTO) { // photo time clip disable
            timeClipLayout.visibility = View.GONE
        } else {
            timeClipLayout.visibility = View.VISIBLE
            val videoInfo = this.videoInfo
            videoInfo ?: return

            timeSelectView.initFrames(mediaItem, videoInfo.duration.toLong(), object :
                SingleSelectFrameView.ScrollListener {
                var isPlayBeforeSeek = true
                var hasTouched = false
                override fun onTouched() {
                    isPlayBeforeSeek = isPlaying
                    hasTouched = true
                }

                override fun onScroll(timePos: Int) {
                    mediaPlayer?.seekTo(timePos, true)
                    refreshSourceStartTime(timePos, mediaItem, videoInfo)
                    isTimeClipChanged = true
                }

                override fun onScrollIdle(timePos: Int) {
                    if (!hasTouched) {
                        return
                    }

                    hasTouched = false
                    seekAndPlay(timePos, isPlayBeforeSeek)
                    refreshSourceStartTime(timePos, mediaItem, videoInfo)
                }
            })

            timeSelectView.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (timeSelectView.width != 0) {
                        val duration: Float =
                            mediaPlayer?.getDuration()?.toFloat() ?: videoInfo.duration.toFloat()
                        timeSelectView.moveTo(mediaItem.sourceStartTime / duration)
                        timeSelectView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            })
        }
    }

    private fun refreshSourceStartTime(
        timePos: Int,
        mediaItem: MediaItem,
        videoInfo: VideoMetaDataInfo
    ) {
        sourceStartTime =
            if (timePos + mediaItem.duration > videoInfo.duration)
                (videoInfo.duration - mediaItem.duration).toInt() else timePos
    }

    private fun initSurfaceSize(mediaItem: MediaItem): Point {
        val surfaceSize = Point()
        windowManager.defaultDisplay.getSize(surfaceSize)
        LogUtil.d(TAG, "initSurfaceSize Display.x=${surfaceSize.x}, Display.y=${surfaceSize.y}")
        LogUtil.d(
            TAG,
            "initSurfaceSize mediaItem.width=${mediaItem.width}, mediaItem.height=${mediaItem.height}"
        )
        val marginLayoutParams = controlPanel.layoutParams as ConstraintLayout.LayoutParams
        val height = marginLayoutParams.height
        val surfaceLp = clipSurface.layoutParams as ViewGroup.MarginLayoutParams
        surfaceLp.topMargin = SizeUtil.dp2px(30f)
        surfaceSize.x = surfaceSize.x - SizeUtil.dp2px(10f)
        if (mediaItem.type == MediaItem.TYPE_PHOTO) {
            surfaceSize.y = surfaceSize.y - surfaceLp.topMargin - SizeUtil.dp2px(100f)
        } else {
            surfaceSize.y = surfaceSize.y - surfaceLp.topMargin - height
        }
        LogUtil.d(
            TAG,
            "initSurfaceSize surfaceSize.x=${surfaceSize.x}, surfaceSize.y=${surfaceSize.y}"
        )
        surfaceLp.height = surfaceSize.y
        return surfaceSize
    }

    /**
     * get already time and area modify mediaItem
     */
    private fun genEditedMediaItem(oriMediaItem: MediaItem): MediaItem {
        return oriMediaItem.copy(
            sourceStartTime = if (oriMediaItem.type == MediaItem.TYPE_VIDEO)
                this.sourceStartTime.toLong() else oriMediaItem.sourceStartTime,
            crop = if (oriMediaItem.alignMode == MediaItem.ALIGN_MODE_VIDEO)
                areaClipHelper.getEditedCrop() else oriMediaItem.crop
        )
    }

    private fun seekAndPlay(position: Int, isAutoPlay: Boolean = false) {
        mediaPlayer?.seekTo(position, isAutoPlay)
        if (isAutoPlay) playIv.visibility = View.GONE
    }

    private fun pausePlay() {
        mediaPlayer?.pause()
        curMediaItem?.type.let {
            if (it == MediaItem.TYPE_VIDEO) {
                playIv.visibility = View.VISIBLE
            } else {
                playIv.visibility = View.GONE
            }
        }
    }

    private fun startPlay() {
        mediaPlayer?.start()
        curMediaItem?.type.let {
            playIv.visibility = View.GONE
        }
    }

    private fun moveVideo(scale: Float, translateX: Float, translateY: Float) {
        mediaPlayer?.updateVideoTransform(
            1.0F, scale, 0F, translateX, translateY,
            false, ""
        )
        mediaPlayer?.refreshCurrentFrame()
    }

    override fun onBackPressed() {
        if (!isMaterialModify()) {
            finish()
            return
        }
        showQuitDialog()
    }

    private fun showQuitDialog() {
        TipDialog.Builder(this)
            .setDialogOperationListener(object : TipDialog.DialogOperationListener {
                override fun onClickSure() {
                    finish()
                }

                override fun onClickCancel() {

                }
            })
            .setTitleText(getString(R.string.cutsame_edit_abandon_edit))
            .create()
            .show()
    }


    private fun isMaterialModify(): Boolean {
        if (isAreaClipChanged || isTimeClipChanged) return true
        return false
    }

    override fun onPause() {
        pausePlay()
        super.onPause()
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        coroutineContext.cancel()
        super.onDestroy()
    }
}
