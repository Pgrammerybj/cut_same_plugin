package com.bytedance.ugc.window

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.ss.android.ugc.effectdemo.util.*
import com.ss.android.vesdk.VEGetFrameSettings
import com.ss.android.vesdk.VERecorder
import com.ss.android.vesdk.VESize
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt


/**
 * Create by caoliangzhao on 2021/3/29.
 * Describe:
 *
 **/
class SmallWindowView(
    private val veRecorder: VERecorder,
    lifecycle: Lifecycle,
    sfView: SurfaceView,
    val container: ViewGroup
) : CoroutineScope by MainScope() {

    companion object {
        const val CLICK_TIME = 500L // DONW -> UP 小于CLICK_TIME 认为是一次点击
        const val STOP_DELAY = 300L // 为stopRender加一个delay，因为fold的时候，画面停止刷新，会导致缩小后，画面尺寸不匹配
        const val FOLD_BTN_SHOW_DELAY = 100L
        const val CLICK_DISTANCE = 10F
        const val TAG = "SmallWindowView"
    }

    /**
     * 小视频
     */
    private val smallWindowView: ViewGroup = LayoutInflater.from(container.context)
        .inflate(R.layout.layout_small_window, container, false) as ViewGroup

    /**
     * 拍摄小窗
     */
    private val videoSurfaceView =
        smallWindowView.findViewById<SurfaceView>(R.id.small_window_surface)

    /**
     * 拍摄View Container
     */
    private val cameraSurfaceContainer = sfView.parent as ViewGroup
    private val cameraSurfaceView = sfView

    // 小窗的折叠和展开按钮
    private val smallWindowSwitchBtn =
        smallWindowView.findViewById<View>(R.id.small_window_switch_btn)

    private val frontImageView: ImageView = smallWindowView.findViewById(R.id.front_img)

    // 拍同款视频小窗状态：默认是非折叠态小窗
    private var curVideoWindowStatus: VideoWindowStatus = VideoWindowStatus.SMALL_WINDOW_UN_FOLD

    // 拍摄窗口的状态：默认全屏
    private var curCameraWindowStatus: CameraWindowStatus =
        CameraWindowStatus.CAMERA_WINDOW_FULL_SCREEN

    private val animationHelper = SmallWindowAnimationHelper(smallWindowView)

    private var isOnStop = false

    /**
     * 初始化SmallWindowUI
     */
    init {
        smallWindowView.hide()
        smallWindowSwitchBtn.show()
        container.post {
            smallWindowView.apply {
                val lp = FrameLayout.LayoutParams(
                    SmallWindowConfig.smallWindowWidth,
                    SmallWindowConfig.smallWindowHeight
                )
                lp.leftMargin =
                    container.measuredWidth - SmallWindowConfig.horizontalPadding - SmallWindowConfig.smallWindowWidth
                lp.topMargin = SmallWindowConfig.verticalPadding
                lp.gravity = Gravity.START or Gravity.TOP
                container.addView(this, lp)
            }
            smallWindowSwitchBtn?.apply {
                layoutParams.width = SmallWindowConfig.foldIconWidth
                layoutParams.height = SmallWindowConfig.foldIconWidth
                (layoutParams as ConstraintLayout.LayoutParams).also {
                    it.marginEnd = SmallWindowConfig.foldIconMargin
                    it.bottomMargin = SmallWindowConfig.foldIconMargin
                }
            }
            setOnTouch()
        }
        lifecycle.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onStop() {
                Log.d(TAG, "onStop")
                isOnStop = true
            }


            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onStart() {
                Log.d(TAG, "onStart")
                if (isOnStop) {
                    for (i in 0 until cameraSurfaceContainer.childCount) {
                        val child = cameraSurfaceContainer.getChildAt(i)
                        if (child is SurfaceView) {
                            val lp = child.layoutParams
                            cameraSurfaceContainer.removeView(child)
                            cameraSurfaceContainer.addView(child, i, lp)
                        }
                    }
                    for (i in 0 until smallWindowView.childCount) {
                        val child = smallWindowView.getChildAt(i)
                        if (child is SurfaceView) {
                            val lp = child.layoutParams
                            smallWindowView.removeView(child)
                            smallWindowView.addView(child, i, lp)
                            frontImageView.bringToFront()
                            smallWindowSwitchBtn.bringToFront()
                        }
                    }
                    isOnStop = false
                }
            }
        })
        videoSurfaceView.setZOrderMediaOverlay(true)

        videoSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                // setSurface
                veRecorder.setFollowShotSurface(surfaceHolder.surface)
                if (curVideoWindowStatus != VideoWindowStatus.SMALL_WINDOW_FOLD) {
                    veRecorder.stopFollowShowRender(false)
                    if (curVideoWindowStatus == VideoWindowStatus.SMALL_WINDOW_UN_FOLD) {
                        setSmallWindowBackground(0x282828)
                    }
                }
            }

            override fun surfaceChanged(
                surfaceHolder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                veRecorder.setFollowShotSurface(surfaceHolder.surface)
                veRecorder.notifyFollowShotSurfaceChanged(format, width, height, false)
                if (isAnimPlaying.not()) {
                    if (curVideoWindowStatus == VideoWindowStatus.SMALL_WINDOW_FOLD) {
                        postOnUiThread(STOP_DELAY) {
                            veRecorder.stopFollowShowRender(true)
                        }
                    } else {
                        veRecorder.stopFollowShowRender(false)
                    }
                    if (curCameraWindowStatus == CameraWindowStatus.CAMERA_WINDOW_FOLD) {
                        postOnUiThread(STOP_DELAY) {
                            veRecorder.stopRender(true)
                        }
                    } else {
                        veRecorder.stopRender(false)
                    }
                }
            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                veRecorder.stopFollowShowRender(true)
            }
        })

        smallWindowView.clickWithTrigger {
            if (curVideoWindowStatus == VideoWindowStatus.SMALL_WINDOW_UN_FOLD || curCameraWindowStatus == CameraWindowStatus.CAMERA_WINDOW_UN_FOLD) {
                switchWindow()
            } else if (curVideoWindowStatus == VideoWindowStatus.SMALL_WINDOW_FOLD || curCameraWindowStatus == CameraWindowStatus.CAMERA_WINDOW_FOLD) {
                changeSmallWindowStatus(fold = false)
            }
        }

        // 折叠/展开 按钮
        smallWindowSwitchBtn.clickWithTrigger {
            if (curVideoWindowStatus == VideoWindowStatus.SMALL_WINDOW_FOLD || curCameraWindowStatus == CameraWindowStatus.CAMERA_WINDOW_FOLD) {
                // 展开
                changeSmallWindowStatus(false)
            } else if (curVideoWindowStatus == VideoWindowStatus.SMALL_WINDOW_UN_FOLD || curCameraWindowStatus == CameraWindowStatus.CAMERA_WINDOW_UN_FOLD) {
                // 折叠
                changeSmallWindowStatus(true)
            }
        }
    }

    @Volatile
    private var isAnimPlaying = false

    private fun changeSmallWindowStatus(fold: Boolean) {
        launch {
            if (isAnimPlaying) return@launch
            if (curVideoWindowStatus == VideoWindowStatus.SMALL_WINDOW_FULL_SCREEN) {
                veRecorder.stopRender(false)
            } else if (curCameraWindowStatus == CameraWindowStatus.CAMERA_WINDOW_FULL_SCREEN) {
                veRecorder.stopFollowShowRender(false)
            }
            isAnimPlaying = true
            val curImage = getCurPreviewImage(
                SmallWindowConfig.smallWindowWidth, SmallWindowConfig.smallWindowHeight,
                curCameraWindowStatus == CameraWindowStatus.CAMERA_WINDOW_FULL_SCREEN
            )
            if (curImage != null) {
                frontImageView.setImageBitmap(curImage)
                frontImageView.show()
                frontImageView.bringToFront()
                smallWindowSwitchBtn.bringToFront()
            }
            val surfaceView =
                if (curVideoWindowStatus == VideoWindowStatus.SMALL_WINDOW_FULL_SCREEN) {
                    cameraSurfaceView
                } else {
                    videoSurfaceView
                }
            surfaceView.hide()
            if (fold) {
                animationHelper.startFoldAnim {
                    surfaceView.show()
                    if (curVideoWindowStatus == VideoWindowStatus.SMALL_WINDOW_FULL_SCREEN) {
                        curCameraWindowStatus = CameraWindowStatus.CAMERA_WINDOW_FOLD
                        postOnUiThread(STOP_DELAY) {
                            veRecorder.stopRender(true)
                        }
                    } else if (curCameraWindowStatus == CameraWindowStatus.CAMERA_WINDOW_FULL_SCREEN) {
                        curVideoWindowStatus = VideoWindowStatus.SMALL_WINDOW_FOLD
                        postOnUiThread(STOP_DELAY) {
                            veRecorder.stopFollowShowRender(true)
                        }
                    }
                    isAnimPlaying = false
                    container.requestLayout()
                }
            } else {
                animationHelper.startUnFoldAnim {
                    surfaceView.show()
                    if (curVideoWindowStatus == VideoWindowStatus.SMALL_WINDOW_FULL_SCREEN) {
                        curCameraWindowStatus = CameraWindowStatus.CAMERA_WINDOW_UN_FOLD
                        postOnUiThread(STOP_DELAY) {
                            frontImageView.gone()
                            veRecorder.stopRender(false)
                        }
                    } else if (curCameraWindowStatus == CameraWindowStatus.CAMERA_WINDOW_FULL_SCREEN) {
                        curVideoWindowStatus = VideoWindowStatus.SMALL_WINDOW_UN_FOLD
                        postOnUiThread(STOP_DELAY) {
                            frontImageView.gone()
                            veRecorder.stopFollowShowRender(false)
                        }
                    }
                    isAnimPlaying = false
                    container.requestLayout()
                }
            }
        }
    }

    private var isDragging = false
    private var downX = 0F
    private var downY = 0F
    private var downTime = 0L

    private val myOnTouchListener = object : View.OnTouchListener {
        override fun onTouch(view: View?, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "ACTION_DOWN")
                    downTime = System.currentTimeMillis()
                    downX = event.x
                    downY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
//                    Log.d(TAG, "ACTION_MOVE ${System.currentTimeMillis() - downTime}")
                    if (System.currentTimeMillis() - downTime < CLICK_TIME &&
                        abs(event.x - downX) < SizeUtil.dp2px(CLICK_DISTANCE) &&
                        abs(event.y - downY) < SizeUtil.dp2px(CLICK_DISTANCE)
                    ) {
//                        Log.d(TAG, "ACTION_MOVE return")
                        return true
                    }
                    val dx = event.x - downX
                    val dy = event.y - downY
                    (smallWindowView.layoutParams as? FrameLayout.LayoutParams)?.let {
                        val targetLeftMargin = (it.leftMargin + dx).toInt()
                        val targetTopMargin = (it.topMargin + dy).toInt()
                        if (targetLeftMargin >= SmallWindowConfig.horizontalPadding &&
                            targetLeftMargin <= container.measuredWidth - SmallWindowConfig.horizontalPadding - smallWindowView.measuredWidth
                        ) {
                            it.leftMargin = targetLeftMargin
                        }
                        if (targetTopMargin >= SmallWindowConfig.verticalPadding &&
                            targetTopMargin <= container.measuredHeight - SmallWindowConfig.verticalPadding - smallWindowView.measuredHeight
                        ) {
                            it.topMargin = targetTopMargin
                        }
                        smallWindowView.layoutParams = it
                    }
                    isDragging = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Log.d(TAG, "ACTION_UP ${System.currentTimeMillis() - downTime}")
                    if (isDragging.not() && System.currentTimeMillis() - downTime < CLICK_TIME &&
                        abs(event.x - downX) < SizeUtil.dp2px(CLICK_DISTANCE) &&
                        abs(event.y - downY) < SizeUtil.dp2px(CLICK_DISTANCE)
                    ) {
                        view?.performClick()
                        Log.d(TAG, "ACTION_UP click")
                    } else {
                        moveToSide()
                        Log.d(TAG, "ACTION_UP mode side")
                    }
                    isDragging = false
                    downTime = 0L
                    downX = 0F
                    downY = 0F
                }
            }
            return true
        }
    }

    private fun setOnTouch() {
        smallWindowView.setOnTouchListener(myOnTouchListener)
        smallWindowSwitchBtn.setOnTouchListener(myOnTouchListener)
    }

    private fun moveToSide() { // 贴边动画
        val centerX = (smallWindowView.left + smallWindowView.right) / 2
        val targetMarginLeft = if (centerX < container.measuredWidth / 2) {
            SmallWindowConfig.horizontalPadding
        } else {
            val windowWidth = if (curVideoWindowStatus == VideoWindowStatus.SMALL_WINDOW_UN_FOLD ||
                curCameraWindowStatus == CameraWindowStatus.CAMERA_WINDOW_UN_FOLD
            ) {
                SmallWindowConfig.smallWindowWidth
            } else {
                SmallWindowConfig.smallWindowFoldWidth
            }
            container.measuredWidth - SmallWindowConfig.horizontalPadding - windowWidth
        }
        val startMarginLeft =
            (smallWindowView.layoutParams as ViewGroup.MarginLayoutParams).leftMargin
        val dx = targetMarginLeft - startMarginLeft
        val animator = ValueAnimator.ofFloat(0F, 1F)
        animator.duration = 200L
        animator.addUpdateListener {
            val cur = it.animatedValue as Float
            (smallWindowView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                this.leftMargin = (startMarginLeft + dx * cur).toInt()
                smallWindowView.layoutParams = this
            }
        }
        animator.start()
    }

    private fun switchWindow() { // 切换窗口
        if (curCameraWindowStatus == CameraWindowStatus.CAMERA_WINDOW_FULL_SCREEN) { // 拍摄全屏状态
            val indexOfVideo = smallWindowView.indexOfChild(videoSurfaceView).coerceAtLeast(0)
            val indexOfCamera =
                cameraSurfaceContainer.indexOfChild(cameraSurfaceView).coerceAtLeast(0)
            smallWindowView.removeView(videoSurfaceView)
            cameraSurfaceContainer.removeView(cameraSurfaceView)
            curCameraWindowStatus = CameraWindowStatus.CAMERA_WINDOW_UN_FOLD
            curVideoWindowStatus = VideoWindowStatus.SMALL_WINDOW_FULL_SCREEN
            cameraSurfaceContainer.addView(
                videoSurfaceView, indexOfCamera, ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )
            )
            smallWindowView.addView(
                cameraSurfaceView, indexOfVideo, ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )
            )
            setSmallWindowBackground(0x000000)
            setBackground(0x282828)
            cameraSurfaceView.setZOrderMediaOverlay(true)
            videoSurfaceView.setZOrderMediaOverlay(false)
        } else if (curVideoWindowStatus == VideoWindowStatus.SMALL_WINDOW_FULL_SCREEN) {
            val indexOfVideo = smallWindowView.indexOfChild(videoSurfaceView).coerceAtLeast(0)
            val indexOfCamera =
                cameraSurfaceContainer.indexOfChild(cameraSurfaceView).coerceAtLeast(0)
            smallWindowView.removeView(cameraSurfaceView)
            cameraSurfaceContainer.removeView(videoSurfaceView)
            curCameraWindowStatus = CameraWindowStatus.CAMERA_WINDOW_FULL_SCREEN
            curVideoWindowStatus = VideoWindowStatus.SMALL_WINDOW_UN_FOLD

            cameraSurfaceContainer.addView(
                cameraSurfaceView, indexOfVideo, ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )
            )
            smallWindowView.addView(
                videoSurfaceView, indexOfCamera, ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )
            )
            setSmallWindowBackground(0x282828)
            setBackground(0x000000)
            cameraSurfaceView.setZOrderMediaOverlay(false)
            videoSurfaceView.setZOrderMediaOverlay(true)
        }
        smallWindowSwitchBtn.bringToFront()
    }

//    fun setFollowShotDisplayDegree(degree: Int) {
//        veRecorder.setFollowShotDisplayDegree(degree)
//    }

    fun setSmallWindowBackground(color: Int) {
        val red = (color and 0x00FF0000) shr 16
        val green = (color and 0x0000FF00) shr 8
        val blue = (color and 0x000000FF)
        val alpha = (color and 0xFF000000.toInt()) shr 24
        veRecorder.setFollowingShotWindowsBackground(red, green, blue, alpha)
    }

    private fun setBackground(color: Int) {
        val red = (color and 0x00FF0000) shr 16
        val green = (color and 0x0000FF00) shr 8
        val blue = (color and 0x000000FF)
        val alpha = (color and 0xFF000000.toInt()) shr 24
        veRecorder.setBackground(red, green, blue, alpha)
    }

    suspend fun getCurPreviewImage(width: Int, height: Int, isSmallWindow: Boolean): Bitmap? =
        withTimeoutOrNull(
            200
        ) {
            suspendCancellableCoroutine { con ->
                val builder = VEGetFrameSettings.Builder()
                    .setGetFrameType(if (isSmallWindow) VEGetFrameSettings.VEGetFrameType.FOLLOW_SHOT_FRAME_MODE else VEGetFrameSettings.VEGetFrameType.NORMAL_GET_FRAME_MODE)
                    .setEffectType(VEGetFrameSettings.VEGetFrameEffectType.FULL_EFFECT)
                    .setFitMode(VEGetFrameSettings.VEGetFrameFitMode.CENTER_CROP)
                    .setTargetResolution(VESize(width, height))
                    .setGetFrameCallback { data, width, height ->
                        kotlin.runCatching {
                            val createBitmap =
                                Bitmap.createBitmap(data, width, height, Bitmap.Config.ARGB_8888)
                            con.resume(createBitmap)
                        }.onFailure {
                            con.resume(null)
                        }
                    }

                val result = veRecorder.getPreviewFrame(builder.build())
            }
        }

    fun show() {
        smallWindowView.show()
    }

    fun hide() {
        smallWindowView.hide()
    }
}

enum class VideoWindowStatus {
    SMALL_WINDOW_FOLD,
    SMALL_WINDOW_UN_FOLD,
    SMALL_WINDOW_FULL_SCREEN
}

enum class CameraWindowStatus {
    CAMERA_WINDOW_FOLD,
    CAMERA_WINDOW_UN_FOLD,
    CAMERA_WINDOW_FULL_SCREEN
}

object SmallWindowConfig {
    private const val MARGIN_HORIZONTAL = 10F
    private const val MARGIN_VERTICAL = 70F

    val smallWindowWidth = SizeUtil.dp2px(110F)
    val smallWindowHeight = smallWindowWidth * 16 / 9
    val smallWindowFoldWidth = (smallWindowWidth / 2.0F).roundToInt()
    val smallWindowFoldHeight = (smallWindowWidth / 2.0F).roundToInt()
    val foldIconWidth = (smallWindowWidth / 4.0F).roundToInt()
    val foldIconMargin = (foldIconWidth / 4.0F).roundToInt()

    val unFoldIconWidth = (smallWindowFoldWidth * 2 / 3F).roundToInt()

    val horizontalPadding = SizeUtil.dp2px(MARGIN_HORIZONTAL)
    val verticalPadding = SizeUtil.dp2px(MARGIN_VERTICAL)
}
