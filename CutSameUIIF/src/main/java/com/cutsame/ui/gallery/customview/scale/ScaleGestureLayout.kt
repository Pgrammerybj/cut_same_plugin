package com.cutsame.ui.gallery.customview.scale

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.cutsame.ui.cut.videoedit.customview.gesture.MoveGestureDetector
import com.cutsame.ui.cut.videoedit.customview.gesture.ScaleGestureDetector
import com.cutsame.ui.gallery.customview.AppearOrDisappearAnimationConfiguration
import com.cutsame.ui.gallery.customview.ClipFrameLayout
import com.cutsame.ui.gallery.customview.utils.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ClickableViewAccessibility")
class ScaleGestureLayout @JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ClipFrameLayout(context, attrs, defStyleAttr), IScaleGestureLayout {

    companion object {
        private const val ANIMATION_DURATION = 200L
        private const val MIN_SCALE_VALUE = 0.7F
        private const val MAX_SCALE_VALUE = 10F

        //双击放大三倍
        private const val SCALE_RATIO = 3F
    }

    private var displayMatrix = Matrix()
    private var displayRectF: RectF? = null
    private var videoRect: Rect? = null

    private var scaleEndExecute = false
    private val scroller = OverScroller(context)
    private var scrollByMultiPoint = false

    private var autoFitAnimator: Animator? = null
    private var springBackAnimator: Animator? = null
    private var appearOrDisAppearAnimator: AnimatorSet? = null
    private var animationRunnable: Runnable? = null


    private var totalDragDistanceY = 0F
    private var totalDragDistanceX = 0F
    private var currentAlpha = 1F
    private var curScaleFactor = 1F

    private val fastOutSlowInInterpolator = FastOutSlowInInterpolator()
    private var scaleGestureLayoutListener: ScaleGestureLayoutListener? = null

    //可以复用，不用反复new的对象
    private val matrixEvaluator = MatrixEvaluator()

    private val listener = object : CombinationGestureListener {
        override fun onDown(e: MotionEvent): Boolean {
            if (e.pointerCount > 2) {
                parent.requestDisallowInterceptTouchEvent(true)
            }
            if (isAnimationAppearRunning()) {
                return false
            }
            if (curScaleFactor > 1F) {
                parent.requestDisallowInterceptTouchEvent(true)
            }
            scroller.forceFinished(true)
            return true
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (scaleEndExecute) {
                //onScaleEnd到onMoveEnd之间的onScroll事件丢掉
                return false
            }
            val pointerCount = e2.pointerCount
            if (pointerCount >= 3) {
                return false
            }
            scrollByMultiPoint = e2.pointerCount > 1
            if (scrollByMultiPoint) {
                //双指滑时只进行Matrix位移操作
                translateBy(-distanceX, -distanceY)
            } else {
                if (curScaleFactor > 1F) {
                    moveIfNeeded(-distanceX, -distanceY)
                } else if (curScaleFactor == 1F) {
                    zoomOutIfNeeded(e2, -distanceX, -distanceY)
                }
            }
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            flingIfNeeded(velocityX, velocityY)
            return true
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            //onScaleEnd执行完毕之后不执行onMoveEnd
            if (scaleEndExecute) {
                scaleEndExecute = false
                return
            }
            restoreFromMoveEnd()
        }

        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            scaleGestureLayoutListener?.onClick()
            return true
        }

        override fun onLongPress(event: MotionEvent) {

        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            //防止和父布局ViewPager事件冲突
            parent.requestDisallowInterceptTouchEvent(true)
            val canScale = curScaleFactor >= 1F && !isDragging
            if (canScale) {
                scaleGestureLayoutListener?.onScaleBegin()
            }
            return canScale
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val focusX = detector.focusX
            val focusY = detector.focusY
            if (scaleFactor.isNaN() || scaleFactor.isInfinite()) {
                return false
            }
            scaleIfNeeded(scaleFactor, focusX, focusY)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            scaleEndExecute = true
            scaleGestureLayoutListener?.onScaleEnd(curScaleFactor)
            restoreFromScaleEnd()
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            doubleTapIfNeed(e.x, e.y)
            return true
        }
    }
    private val gestureDetector = CombinationGestureDetector(context, listener)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (childCount == 0) {
            return false
        }
        return gestureDetector.onTouchEvent(this, event)
    }

    init {
        post {
            calculateDisplayArea(width.toFloat(), height.toFloat())
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (videoRect != null
            && width > 0
            && height > 0
            && displayMatrix.isIdentity
        ) {
            executePendingAppearAnimation()
        }
    }

    override fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        videoRect = Rect(0, 0, videoWidth, videoHeight)
    }

    override fun setZoomListener(listener: ScaleGestureLayoutListener) {
        scaleGestureLayoutListener = listener
    }

    @Suppress("LongParameterList")
    override fun animateAppear(
        fromRect: Rect?,
        visibleRect: Rect?,
        radius: Float,
        maskInsetPixel: IntArray?,
        listItemScaleType: ListItemScaleType,
        animationConfiguration: AppearOrDisappearAnimationConfiguration
    ) {
        animationRunnable = Runnable {
            val videoRectNotAvailable = videoRect?.isEmpty ?: true
            val fromRectNotAvailable = fromRect?.isEmpty ?: true
            val visibleRectNotAvailable = visibleRect?.isEmpty ?: true
            if (videoRectNotAvailable
                || fromRectNotAvailable
                || visibleRectNotAvailable
            ) {
                val fallbackAnimator = ValueAnimator.ofFloat(0.0F, 1.0F).apply {
                    addUpdateListener {
                        val value = it.animatedValue
                        if (value is Float) {
                            scaleGestureLayoutListener?.onAlphaPercent(value)
                            alpha = value
                        }
                    }
                }
                appearOrDisAppearAnimator = AnimatorSet()
                appearOrDisAppearAnimator?.run {
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            super.onAnimationEnd(animation)
                            appearOrDisAppearAnimator = null
                        }
                    })
                    playTogether(fallbackAnimator)
                    interpolator = fastOutSlowInInterpolator
                    duration = ANIMATION_DURATION
                    start()
                }
                return@Runnable
            }
            val mFromRect = fromRect ?: return@Runnable
            val mVisibleRect = visibleRect ?: return@Runnable

            val locationInWindow = IntArray(2)
            getLocationOnScreen(locationInWindow)
            mFromRect.offset(-locationInWindow[0], -locationInWindow[1])
            mVisibleRect.offset(-locationInWindow[0], -locationInWindow[1])

            val fromMatrix = getFromMatrix(fromRect, width, height, videoRect, listItemScaleType)
            val matrixAnimator = getMatrixToMatrixAnimator(
                Matrix(fromMatrix),
                Matrix(),
                videoRect,
                animationConfiguration
            ) { matrix ->
                displayMatrix = matrix
            }
            val alphaAnimator = ValueAnimator.ofFloat(0.0F, 1.0F).apply {
                addUpdateListener {
                    val value = it.animatedValue
                    if (value is Float) {
                        scaleGestureLayoutListener?.onAlphaPercent(value)
                    }
                }
                duration = animationConfiguration.alphaDuration
                interpolator = animationConfiguration.alphaInterpolator
            }

            val clipFromRect = Rect(fromRect).apply {
                maskInsetPixel?.let {
                    require(maskInsetPixel.size == 4) { "maskInsetPixel length must equal 4" }
                    left += it[0]
                    top += it[1]
                    right -= it[2]
                    bottom -= it[3]
                }
            }

            val clipAnimator = getClipAnimator(clipFromRect, visibleRect, radius, false).apply {
                duration = animationConfiguration.clipDuration
                interpolator = animationConfiguration.clipInterpolator
            }
            appearOrDisAppearAnimator = AnimatorSet()
            appearOrDisAppearAnimator?.run {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        resetGestureDetectorState()
                        displayMatrix.reset()
                        ViewCompat.postInvalidateOnAnimation(this@ScaleGestureLayout)
                        appearOrDisAppearAnimator = null
                    }
                })
                playTogether(matrixAnimator, clipAnimator, alphaAnimator)
                interpolator = fastOutSlowInInterpolator
                duration = ANIMATION_DURATION
                start()
            }
        }
        if (videoRect != null && width > 0 && height > 0) {
            animationRunnable?.run()
        }
    }

    @Suppress("LongParameterList")
    override fun animateDisappear(
        fromRect: Rect?,
        visibleRect: Rect?,
        radius: Float,
        maskInsetPixel: IntArray?,
        listItemScaleType: ListItemScaleType,
        animationConfiguration: AppearOrDisappearAnimationConfiguration,
        endAction: () -> Unit
    ) {
        if (!ViewCompat.isAttachedToWindow(this)) {
            endAction.invoke()
            return
        }
        val videoRectNotAvailable = videoRect?.isEmpty ?: true
        val fromRectNotAvailable = fromRect?.isEmpty ?: true
        val visibleRectNotAvailable = visibleRect?.isEmpty ?: true
        if (videoRectNotAvailable
            || fromRectNotAvailable
            || visibleRectNotAvailable
        ) {
            val fallbackAnimator = ValueAnimator.ofFloat(1.0F, 0.0F).apply {
                addUpdateListener { animation ->
                    scaleX = (animation.animatedValue as Float)
                    scaleY = (animation.animatedValue as Float)
                }
            }
            appearOrDisAppearAnimator = AnimatorSet()
            appearOrDisAppearAnimator?.run {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        appearOrDisAppearAnimator = null
                        endAction.invoke()
                    }
                })
                playTogether(fallbackAnimator, createAlphaAnimator(0F))
                interpolator = fastOutSlowInInterpolator
                duration = ANIMATION_DURATION
                start()
            }
            return
        }
        val mFromRect = fromRect ?: return
        val mVisibleRect = visibleRect ?: return

        val locationInWindow = IntArray(2)
        getLocationOnScreen(locationInWindow)
        mFromRect.offset(-locationInWindow[0], -locationInWindow[1])
        mVisibleRect.offset(-locationInWindow[0], -locationInWindow[1])
        val fromMatrix = getFromMatrix(fromRect, width, height, videoRect, listItemScaleType)

        val matrixAnimator = getMatrixToMatrixAnimator(
            Matrix(displayMatrix),
            Matrix(fromMatrix),
            videoRect,
            animationConfiguration
        ) { matrix ->
            displayMatrix = matrix
        }

        val clipFromRect = Rect(fromRect).apply {
            maskInsetPixel?.let {
                require(maskInsetPixel.size == 4) { "maskInsetPixel length must equal 4" }
                left += it[0]
                top += it[1]
                right -= it[2]
                bottom -= it[3]
            }
        }

        val clipAnimator = getClipAnimator(clipFromRect, visibleRect, radius, true).apply {
            duration = animationConfiguration.clipDuration
            interpolator = animationConfiguration.clipInterpolator
        }

        val alphaAnimator = createAlphaAnimator(0F).apply {
            duration = animationConfiguration.clipDuration
            interpolator = animationConfiguration.clipInterpolator
        }

        appearOrDisAppearAnimator = AnimatorSet()
        appearOrDisAppearAnimator?.run {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    appearOrDisAppearAnimator = null
                    endAction.invoke()
                }
            })
            playTogether(matrixAnimator, clipAnimator, alphaAnimator)
            interpolator = fastOutSlowInInterpolator
            duration = ANIMATION_DURATION
            start()
        }
    }

    override fun drawChildContent(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        var result: Boolean
        canvas.run {
            save()
            concat(displayMatrix)
            result = super.drawChildContent(canvas, child, drawingTime)
            restore()
        }
        return result
    }

    private var isDragging = false
    private fun zoomOutIfNeeded(motionEvent: MotionEvent, distanceX: Float, distanceY: Float) {
        totalDragDistanceX += distanceX
        totalDragDistanceY += distanceY
        if (!isDragging && totalDragDistanceY > abs(totalDragDistanceX)) {
            isDragging = true
            parent.requestDisallowInterceptTouchEvent(true)
        }
        if (isDragging) {
            currentAlpha = MatrixUtils.getAlphaValue(
                RectF(0F, 0F, width.toFloat(), height.toFloat()),
                totalDragDistanceY
            )
            scaleGestureLayoutListener?.onAlphaPercent(currentAlpha)
            val scale = MatrixUtils.getScaleValue(
                0.5F,
                RectF(0F, 0F, width.toFloat(), height.toFloat()),
                totalDragDistanceY
            )
            val currentScale = MatrixUtils.scaleValue(displayMatrix)
            val scaleValue = scale / currentScale
            displayMatrix.run {
                postTranslate(distanceX, distanceY)
                postScale(scaleValue, scaleValue, motionEvent.x, motionEvent.y)
            }
            invalidate()
        }
    }

    private fun moveIfNeeded(distanceX: Float, distanceY: Float) {
        val displayRect = getCurrentDisPlayRectF()
        //左右边界判定
        var deltaX = distanceX
        if (FloatUtils.isGreaterThanEqual(displayRect.left, 0F)
            && FloatUtils.isLessThanEqual(displayRect.right, width.toFloat())
        ) {
            //左右边缘都在内部
            deltaX = 0F
        } else if (FloatUtils.isGreaterThanEqual(displayRect.left, 0F)
            && FloatUtils.isGreaterThan(displayRect.right, width.toFloat())
        ) {
            //左侧在内部，右侧露出
            deltaX = if (distanceX >= 0) {
                //向右滑
                0F
            } else {
                -min(abs(displayRect.right - width), abs(distanceX))
            }
        } else if (FloatUtils.isLessThan(displayRect.left, 0F)
            && FloatUtils.isLessThanEqual(displayRect.right, width.toFloat())
        ) {
            //左侧露出，右侧在内部
            deltaX = if (distanceX >= 0) {
                //向右滑
                min(abs(displayRect.left), abs(distanceX))
            } else {
                0F
            }
        } else if (FloatUtils.isLessThan(displayRect.left, 0F)
            && FloatUtils.isGreaterThan(displayRect.right, width.toFloat())
        ) {
            //左右都露出
            deltaX = if (distanceX >= 0) {
                (min(abs(displayRect.left), abs(distanceX)))
            } else {
                -min(abs(displayRect.right - width), abs(distanceX))
            }
        }

        //上下边界判定
        var deltaY = distanceY
        if (FloatUtils.isGreaterThanEqual(displayRect.top, 0F)
            && FloatUtils.isLessThanEqual(displayRect.bottom, height.toFloat())
        ) {
            //上下边缘都在内部
            deltaY = 0F
        } else if (FloatUtils.isGreaterThanEqual(displayRect.top, 0F)
            && FloatUtils.isGreaterThan(displayRect.bottom, height.toFloat())
        ) {
            //上边缘在内部，下边缘露出
            deltaY = if (distanceY >= 0) {
                //向下滑
                0F
            } else {
                -min(abs(displayRect.bottom - height), abs(distanceY))
            }
        } else if (FloatUtils.isLessThan(displayRect.top, 0F)
            && FloatUtils.isLessThanEqual(displayRect.bottom, height.toFloat())
        ) {
            //上边缘露出，下边缘在内部
            deltaY = if (distanceY >= 0) {
                //向下滑
                min(abs(displayRect.top), abs(distanceY))
            } else {
                0F
            }
        } else if (FloatUtils.isLessThan(displayRect.top, 0F)
            && FloatUtils.isGreaterThan(displayRect.bottom, height.toFloat())
        ) {
            //上下边缘都露出
            deltaY = if (distanceY >= 0) {
                (min(abs(displayRect.top), abs(distanceY)))
            } else {
                -min(abs(displayRect.bottom - height), abs(distanceY))
            }
        }
        if (abs(distanceX) > abs(distanceY) //横滑的情况
            && FloatUtils.isEqual(deltaX, 0F)
        ) {
            if (distanceX > 0 && FloatUtils.isGreaterThanEqual(displayRect.left, 0F)) {
                //向右滑
                parent.requestDisallowInterceptTouchEvent(false)
                return
            } else if (distanceX < 0 && FloatUtils.isLessThanEqual(
                    displayRect.right,
                    width.toFloat()
                )
            ) {
                //向左滑
                parent.requestDisallowInterceptTouchEvent(false)
                return
            }
        }
        translateBy(deltaX, deltaY)
    }

    private fun scaleIfNeeded(scaleFactor: Float, focusX: Float, focusY: Float) {
        if (scaleFactor == 1F) {
            return
        }
        val currentScale = MatrixUtils.scaleValue(displayMatrix)
        var newScaleValue = scaleFactor
        if (scaleFactor < 1F) {
            newScaleValue = max(MIN_SCALE_VALUE / currentScale, scaleFactor)
        } else if (scaleFactor > 1F) {
            newScaleValue = min(MAX_SCALE_VALUE / currentScale, scaleFactor)
        }
        val pair = calculateFocusPoint(focusX, focusY)
        scaleMatrix(newScaleValue, pair.first, pair.second)
    }

    private fun restoreFromMoveEnd() {
        if (curScaleFactor > 1F) {
            return
        }
        if (totalDragDistanceY > 0 && abs(totalDragDistanceY) > height * MatrixUtils.EXIT_MIN_TRANSLATION_FACTOR_VALUE) {
            scaleGestureLayoutListener?.onExit()
            resetGestureDetectorState()
        } else {
            if (springBackAnimator?.isRunning == true) {
                return
            }
            val matrixAnimator = createMatrixAnimator(Matrix(displayMatrix), Matrix()).apply {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        resetGestureDetectorState()
                    }
                })
            }
            springBackAnimator = AnimatorSet().apply {
                playTogether(matrixAnimator, createAlphaAnimator(1F))
                start()
            }
        }
    }

    private fun restoreFromScaleEnd() {
        if (curScaleFactor >= 1F) {
            autoFitForScale()
            return
        }
        if (springBackAnimator?.isRunning == true) {
            return
        }
        springBackAnimator = createMatrixAnimator((displayMatrix), Matrix()).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    resetGestureDetectorState()
                }
            })
            start()
        }
    }

    private fun autoFitForScale() {
        if (autoFitAnimator?.isRunning == true
            || springBackAnimator?.isRunning == true
        ) {
            return
        }
        if (!scroller.isFinished) {
            return
        }
        val pair = calculateAutoFitValue(getCurrentDisPlayRectF()) ?: return
        val resultMatrix = Matrix(displayMatrix).apply {
            postTranslate(pair.first, pair.second)
        }
        autoFitAnimator = createMatrixAnimator(Matrix(displayMatrix), resultMatrix).apply {
            start()
        }
    }

    private fun calculateAutoFitValue(displayRect: RectF): Pair<Float, Float>? {
        if (FloatUtils.isLessThanEqual(displayRect.left, 0F)
            && FloatUtils.isLessThanEqual(displayRect.top, 0F)
            && FloatUtils.isGreaterThanEqual(displayRect.right, width.toFloat())
            && FloatUtils.isGreaterThanEqual(displayRect.bottom, height.toFloat())
        ) {
            //四个点全部超出屏幕，不进行zoom
            return null
        }
        var deltaX = 0F
        if (displayRect.width() < width.toFloat()) {
            deltaX = width / 2 - displayRect.centerX()
        } else {
            if (FloatUtils.isGreaterThan(displayRect.left, 0F)
                && FloatUtils.isGreaterThan(displayRect.right, width.toFloat())
            ) {
                //左边缘在屏幕内部，右边缘超出屏幕
                deltaX = -displayRect.left
            } else if (FloatUtils.isLessThan(displayRect.left, 0F)
                && FloatUtils.isLessThanEqual(displayRect.right, width.toFloat())
            ) {
                //左边缘超出屏幕，右边缘在屏幕内部
                deltaX = width - displayRect.right
            }
        }

        var deltaY = 0F
        if (displayRect.height() < height.toFloat()) {
            deltaY = height / 2 - displayRect.centerY()
        } else {
            if (FloatUtils.isGreaterThan(displayRect.top, 0F)
                && FloatUtils.isGreaterThan(displayRect.bottom, height.toFloat())
            ) {
                //上边缘在屏幕内部，下边缘超出
                deltaY = -displayRect.top
            } else if (FloatUtils.isLessThan(displayRect.top, 0F)
                && FloatUtils.isLessThan(displayRect.bottom, height.toFloat())
            ) {
                //上边缘超出屏幕，下边缘在屏幕内部
                deltaY = height - displayRect.bottom
            }
        }
        return Pair(deltaX, deltaY)
    }

    private fun calculateFocusPoint(focusX: Float, focusY: Float): Pair<Float, Float> {
        val displayRect = getCurrentDisPlayRectF()
        var realFocusX = focusX
        if (FloatUtils.isLessThan(realFocusX, displayRect.left)) {
            realFocusX = displayRect.left
        } else if (FloatUtils.isGreaterThan(realFocusX, displayRect.right)) {
            realFocusX = displayRect.right
        }

        var realFocusY = focusY
        if (FloatUtils.isLessThan(realFocusY, displayRect.top)) {
            realFocusY = displayRect.top
        } else if (FloatUtils.isGreaterThan(realFocusY, displayRect.bottom)) {
            realFocusY = displayRect.bottom
        }
        return Pair(realFocusX, realFocusY)
    }

    private var previousScrollX = 0
    private var previousScrollY = 0
    private fun flingIfNeeded(velocityX: Float, velocityY: Float) {
        if (autoFitAnimator?.isRunning == true
            || springBackAnimator?.isRunning == true
            || appearOrDisAppearAnimator?.isRunning == true
        ) {
            return
        }
        val displayRect = getCurrentDisPlayRectF()
        if (displayRect.width() < width && displayRect.height() < height) {
            return
        }
        //x轴负方向最大滚动距离
        val minX = -max(displayRect.right - width, 0F)
        val maxX = max(0 - displayRect.left, 0F)
        val minY = -max(displayRect.bottom - height, 0F)
        val maxY = max(0 - displayRect.top, 0F)

        if (FloatUtils.isEqual(minX, 0F)
            && FloatUtils.isEqual(maxX, 0F)
            && FloatUtils.isEqual(minY, 0F)
            && FloatUtils.isEqual(maxY, 0F)
        ) {
            return
        }
        //velocityX>0 图片向右滑 velocityX<0 图片向左滑
        //velocityY>0 图片向下滑 velocityY<0 图片向上滑
        val canFlingX = velocityX > 0 && maxX > 0 || velocityX < 0 && minX < 0
        val canFlingY = velocityY > 0 && maxY > 0 || velocityY < 0 && minY < 0

        if (!canFlingX && !canFlingY) {
            return
        }
        previousScrollX = 0
        previousScrollY = 0
        scroller.fling(
            0,
            0,
            velocityX.toInt(),
            velocityY.toInt(),
            minX.toInt(),
            maxX.toInt(),
            minY.toInt(),
            maxY.toInt()
        )
        ViewCompat.postInvalidateOnAnimation(this)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val oldX = previousScrollX
            val oldY = previousScrollY
            val x = scroller.currX
            val y = scroller.currY
            val deltaX = x - oldX
            val deltaY = y - oldY
            previousScrollX = x
            previousScrollY = y
            translateBy(deltaX.toFloat(), deltaY.toFloat())
        }
    }

    private fun doubleTapIfNeed(focusX: Float, focusY: Float) {
        if (appearOrDisAppearAnimator?.isRunning == true
            || autoFitAnimator?.isRunning == true
        ) {
            return
        }
        if (springBackAnimator?.isRunning == true) {
            springBackAnimator?.cancel()
        }
        val resultMatrix = if (FloatUtils.isGreaterThanEqual(curScaleFactor, SCALE_RATIO)) {
            Matrix()
        } else {
            val newScaleFactor = if (curScaleFactor * SCALE_RATIO > SCALE_RATIO) {
                SCALE_RATIO / curScaleFactor
            } else {
                SCALE_RATIO
            }
            val scaleMatrix = Matrix(displayMatrix).apply {
                val pair = calculateFocusPoint(focusX, focusY)
                postScale(newScaleFactor, newScaleFactor, pair.first, pair.second)
            }
            val rectF = RectF(displayRectF).apply {
                scaleMatrix.mapRect(this)
            }
            calculateAutoFitValue(rectF)?.run {
                scaleMatrix.postTranslate(first, second)
            }
            scaleMatrix
        }
        springBackAnimator = createMatrixAnimator(Matrix(displayMatrix), resultMatrix).apply {
            start()
        }
    }

    private fun executePendingAppearAnimation() {
        animationRunnable?.run()
    }

    private fun createMatrixAnimator(fromMatrix: Matrix, toMatrix: Matrix): Animator {
        return ValueAnimator.ofObject(matrixEvaluator, fromMatrix, toMatrix).apply {
            addUpdateListener {
                val value = it.animatedValue
                if (value is Matrix) {
                    displayMatrix = value
                    curScaleFactor = MatrixUtils.scaleValue(displayMatrix)
                    ViewCompat.postInvalidateOnAnimation(this@ScaleGestureLayout)
                }
            }
            interpolator = fastOutSlowInInterpolator
        }
    }

    private fun createAlphaAnimator(resultValue: Float): Animator {
        return ValueAnimator.ofFloat(currentAlpha, resultValue).apply {
            addUpdateListener {
                val value = it.animatedValue
                if (value is Float) {
                    scaleGestureLayoutListener?.onAlphaPercent(value)
                }
            }
        }
    }

    private fun resetGestureDetectorState() {
        totalDragDistanceX = 0F
        totalDragDistanceY = 0F
        curScaleFactor = 1F
        currentAlpha = 1F
        scrollByMultiPoint = false
        isDragging = false
        parent?.requestDisallowInterceptTouchEvent(false)
    }

    private fun isAnimationAppearRunning(): Boolean {
        return appearOrDisAppearAnimator?.isRunning == true
    }

    private fun scaleMatrix(scaleValue: Float, focusX: Float, focusY: Float) {
        displayMatrix.postScale(scaleValue, scaleValue, focusX, focusY)
        curScaleFactor = MatrixUtils.scaleValue(displayMatrix)
        invalidate()
    }

    private fun translateBy(deltaX: Float, deltaY: Float) {
        if (FloatUtils.isEqual(deltaX, 0.0f) && FloatUtils.isEqual(deltaY, 0.0f)) {
            return
        }
        displayMatrix.postTranslate(deltaX, deltaY)
        invalidate()
    }

    /**
     * 获取当前图片or视频显示的区域
     */
    private fun getCurrentDisPlayRectF(): RectF {
        return RectF(displayRectF).apply {
            displayMatrix.mapRect(this)
        }
    }

    private fun calculateDisplayArea(viewWidth: Float, viewHeight: Float) {
        val mediaWidth = videoRect?.width()?.toFloat() ?: return
        val mediaHeight = videoRect?.height()?.toFloat() ?: return
        val mediaRatio = mediaWidth / mediaHeight
        val viewRatio = viewWidth / viewHeight
        val left: Float
        val right: Float
        val top: Float
        val bottom: Float
        when {
            mediaRatio > viewRatio -> {
                //横图
                left = 0F
                right = viewWidth
                val displayHeight = mediaHeight * viewWidth / mediaWidth
                top = (viewHeight - displayHeight) / 2
                bottom = top + displayHeight
            }
            mediaRatio < viewRatio -> {
                //竖图
                top = 0F
                bottom = height.toFloat()
                val displayWidth = mediaWidth * viewHeight / mediaHeight
                left = (viewWidth - displayWidth) / 2
                right = left + displayWidth
            }
            else -> {
                //比例一致
                left = 0F
                top = 0F
                right = viewWidth
                bottom = viewHeight
            }
        }
        displayRectF = RectF(left, top, right, bottom)
    }

    override fun reset() {
        displayMatrix.reset()
        autoFitAnimator = null
        appearOrDisAppearAnimator = null
        springBackAnimator = null
        resetGestureDetectorState()
        invalidate()
    }
}