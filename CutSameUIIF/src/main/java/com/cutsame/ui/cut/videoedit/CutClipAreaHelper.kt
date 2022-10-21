package com.cutsame.ui.cut.videoedit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Point
import android.graphics.RectF
import android.view.MotionEvent
import android.view.animation.OvershootInterpolator
import com.bytedance.ies.cutsame.util.VideoMetaDataInfo
import com.cutsame.ui.cut.videoedit.customview.gesture.MoveGestureDetector
import com.cutsame.ui.cut.videoedit.customview.gesture.OnGestureListenerAdapter
import com.cutsame.ui.cut.videoedit.customview.gesture.ScaleGestureDetector
import com.ss.android.ugc.cut_log.LogUtil
import com.ss.android.ugc.cut_ui.ItemCrop
import com.ss.android.ugc.cut_ui.MediaItem
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CutClipAreaHelper {

    companion object {
        const val TAG = "CutClipAreaHelper"
        const val SCALE_MAX_RATIO = 50
    }

    var onMove: ((Boolean, Float, Float, Float) -> Unit)? = null
    var onDown: (() -> Unit)? = null
    var onUp: (() -> Unit)? = null
    var canvasSize = Point(0, 0)

    private var boxRectF: RectF = RectF()
    private var videoRectF = RectF()
    private var centerX = 0F
    private var centerY = 0F
    private var width = 0F
    private var height = 0F
    // 显示 scale 用于显示时缩放，需要与操作后的 data.scale 相乘，得到最终 scale
    private var innerScaleFactor = 1F

    private var curScale = 0.3F
    private var curTranslateX = 0F
    private var curTranslateY = 0F

    private var isRebounding = false

    var isChanged = false

    var alignMode = MediaItem.ALIGN_MODE_CANVAS

    val onGestureListener = object : OnGestureListenerAdapter() {
        // 是否对视频进行了改变
        var changeVideo = false
        // 标记是否移动
        var changeVideoLocation = false

        override fun onScaleBegin(scaleFactor: ScaleGestureDetector?): Boolean {
            LogUtil.d(TAG, "onScaleBegin")
            return super.onScaleBegin(scaleFactor)
        }

        override fun onScale(scaleFactor: ScaleGestureDetector?): Boolean {
            scaleFactor ?: return false

            if (innerScaleFactor * curScale * scaleFactor.scaleFactor > SCALE_MAX_RATIO) return true
            curScale *= scaleFactor.scaleFactor
            onMove()
            changeVideo = true
            isChanged = true
            return true
        }

        override fun onScaleEnd(scaleFactor: Float): Boolean {
            LogUtil.d(TAG, "onScaleEnd")
            return super.onScaleEnd(scaleFactor)
        }

        override fun onMoveBegin(
            detector: MoveGestureDetector?,
            downX: Float,
            downY: Float
        ): Boolean {
            LogUtil.d(TAG, "onMoveBegin")
            return super.onMoveBegin(detector, downX, downY)
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            var isTranslate = false
            val tempCenterX = centerX + detector.focusDelta.x
            val tempCenterY = centerY + detector.focusDelta.y
            val tempWidth = width
            val tempHeight = height
            if (tempCenterX - tempWidth / 2 <= boxRectF.left && tempCenterX + tempWidth / 2 >= boxRectF.right) {
                curTranslateX += detector.focusDelta.x / canvasSize.x
                isTranslate = true
            }
            if (tempCenterY - tempHeight / 2 <= boxRectF.top && tempCenterY + tempHeight / 2 >= boxRectF.bottom) {
                curTranslateY += detector.focusDelta.y / canvasSize.y
                isTranslate = true
            }
            LogUtil.d(
                TAG,
                "on move left is " + (tempCenterX - tempWidth / 2) + " right is " +
                        (tempCenterX + tempWidth / 2) + " boxRectF.left " + boxRectF.left +
                        " boxRectF.right " + boxRectF.right + " tempCenterX " + tempCenterX +
                        " center x " + centerX + " is translate " + isTranslate +
                        " detector.focusDelta.x " + detector.focusDelta.x
            )
            if (isTranslate) {
                onMove()
                changeVideo = true
                isChanged = true
                changeVideoLocation = true
            }
            return true
        }

        override fun onMoveEnd(detector: MoveGestureDetector?) {
            LogUtil.d(TAG, "onMoveEnd")
        }

        override fun onDown(event: MotionEvent): Boolean {
            LogUtil.d(TAG, "onDown")
            changeVideo = false
            // 标记是否移动
            changeVideoLocation = false
            onDown?.invoke()
            return super.onDown(event)
        }

        override fun onUp(event: MotionEvent): Boolean {
            LogUtil.d(TAG, "onUp")
            onUp()
            return super.onUp(event)
        }
    }

    fun calculateInnerScaleFactor(
        alignMode: String,
        minWidth: Float,
        minHeight: Float,
        crop: ItemCrop,
        videoInfo: VideoMetaDataInfo
    ) {
        this.alignMode = alignMode
        // 计算视频本来相对 canvas 的缩放大小
        var width = videoInfo.width
        var height = videoInfo.height
        if (videoInfo.rotation == 90 || videoInfo.rotation == 270) {
            width = videoInfo.height
            height = videoInfo.width
        }
        val canvasScaleFactor = getSaleFactorMin(
            width.toFloat(),
            height.toFloat(),
            canvasSize.x.toFloat(),
            canvasSize.y.toFloat()
        )
        val videoWidth = width * canvasScaleFactor
        val videoHeight = height * canvasScaleFactor
        // 根据视频在画布上真实显示的大小计算出视频在显示的时候需要缩放的比例 （一边铺满白框）
        val videoFrameScaleFactor = getSaleFactorMax(
            videoWidth,
            videoHeight,
            minWidth,
            minHeight
        )
        innerScaleFactor = videoFrameScaleFactor
        calculateBoxCoordinates(minWidth, minHeight, canvasSize)
        calculateVideoCoordinates(
            videoWidth * videoFrameScaleFactor,
            videoHeight * videoFrameScaleFactor,
            canvasSize
        )

        restoreBoxPositionInfo(crop, canvasSize)
        onMove(false)
    }

    /**
     * 用于计算标记框相对于TextureView的左上角的初始位置
     */
    private fun calculateBoxCoordinates(boxWidth: Float, boxHeight: Float, canvasSize: Point) {
        val faultBoxWidth = boxWidth - 1
        val faultBoxHeight = boxHeight - 1
        val left = (canvasSize.x - faultBoxWidth) / 2
        val top = (canvasSize.y - faultBoxHeight) / 2
        boxRectF.set(
            left,
            top,
            left + faultBoxWidth,
            top + faultBoxHeight
        )
        LogUtil.d(TAG, " init box rect is $boxRectF")
    }

    /**
     * 用于计算视频相对于TextureView的左上角的初始位置
     */
    private fun calculateVideoCoordinates(
        videoWidth: Float,
        videoHeight: Float,
        canvasSize: Point
    ) {
        val left = (canvasSize.x - videoWidth) / 2
        val top = (canvasSize.y - videoHeight) / 2
        videoRectF.set(
            left,
            top,
            left + videoWidth,
            top + videoHeight
        )
    }

    /**
     * The box position can be calculated by `data.scaleFactor`, `data.transitionX` and
     * `data.transitionY`.
     *
     * Restore these value by `data.veTranslateXXX`
     */
    private fun restoreBoxPositionInfo(crop: ItemCrop, canvasSize: Point) {
        val scale =
            if (boxRectF.width() / boxRectF.height() >= videoRectF.width() / videoRectF.height()) {
                1F / (crop.lowerRightX - crop.upperLeftX)
            } else {
                1F / (crop.lowerRightY - crop.upperLeftY)
            }
        this.curScale = with(scale) { if (this != 0F && this.isFinite()) this else 1F }

        val width = boxRectF.width() / (crop.lowerRightX - crop.upperLeftX)
        centerX = width * (0.5F - crop.upperLeftX) + boxRectF.left
        this.curTranslateX = with((centerX - videoRectF.centerX()) / canvasSize.x) {
            if (this.isFinite()) this else 0F
        }
        val height = boxRectF.height() / (crop.lowerRightY - crop.upperLeftY)
        centerY = height * (0.5F - crop.upperLeftY) + boxRectF.top
        this.curTranslateY = with((centerY - videoRectF.centerY()) / canvasSize.y) {
            if (this.isFinite()) this else 0F
        }
    }

    private fun getSaleFactorMin(srcX: Float, srcY: Float, distX: Float, distY: Float): Float {
        val sx = distX / srcX
        val sy = distY / srcY
        return min(sx, sy)
    }

    private fun getSaleFactorMax(srcX: Float, srcY: Float, distX: Float, distY: Float): Float {
        val sx = distX / srcX
        val sy = distY / srcY
        return max(sx, sy)
    }

    private fun onMove(isTouch: Boolean = true) {
        centerX = videoRectF.centerX() + curTranslateX * canvasSize.x
        centerY = videoRectF.centerY() + curTranslateY * canvasSize.y
        width = videoRectF.width() * curScale
        height = videoRectF.height() * curScale
        onMove?.invoke(isTouch, innerScaleFactor * curScale, curTranslateX, curTranslateY)
    }

    private fun onUp() {
        if (alignMode == MediaItem.ALIGN_MODE_CANVAS) {
            rebound()
            return
        }

        if ((centerX - width / 2 <= boxRectF.left && centerX + width / 2 >= boxRectF.right) &&
            (centerY - height / 2 <= boxRectF.top && centerY + height / 2 >= boxRectF.bottom)
        ) {
            onUp?.invoke()
            return
        }

        rebound()
    }

    private fun rebound() {
        if (isRebounding) return
        isRebounding = true

        val beginScale = curScale
        val beginTransX = curTranslateX
        val beginTransY = curTranslateY
        val animator = ValueAnimator.ofFloat(0F, 1F)
        animator.interpolator = OvershootInterpolator(1.0F)
        animator.duration = 300
        animator.addUpdateListener {
            val fraction = it.animatedFraction
            curScale = beginScale + (1 - beginScale) * fraction
            curTranslateX = beginTransX * (1 - fraction)
            curTranslateY = beginTransY * (1 - fraction)
            onMove(false)
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                isRebounding = false
                onUp?.invoke()
            }
        })
        animator.start()
    }

    /**
     * 获取此时区域裁减的结果
     */
    fun getEditedCrop(): ItemCrop {
        if (width == 0F || height == 0F) return ItemCrop(0F, 0F, 1F, 1F)

        val upperLeftX = removeDeviation((boxRectF.left - (centerX - width / 2)) / width)
        val upperLeftY = removeDeviation((boxRectF.top - (centerY - height / 2)) / height)
        val lowRightX = removeDeviation(1 + (boxRectF.right - (centerX + width / 2)) / width)
        val lowRightY = removeDeviation(1 + (boxRectF.bottom - (centerY + height / 2)) / height)

        LogUtil.d(
            TAG,
            "left is " + (centerX - width / 2) + " right is " +
                    (centerX + width / 2) + " boxRectF.left " + boxRectF.left +
                    " boxRectF.right " + boxRectF.right +
                    "center x is $centerX center y is $centerY width is $width height is $height"
        )

        return ItemCrop(upperLeftX, upperLeftY, lowRightX, lowRightY)
    }

    /**
     *  抹除超过五位小数后的误差
     */
    private fun removeDeviation(float: Float): Float {
        return if (float.isNaN()) {
            0F
        } else {
            (float * 100000).roundToInt() / 100000.0F
        }
    }
}