package com.ss.ugc.android.editor.track.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.annotation.MainThread
import com.bytedance.ies.nle.editor_jni.*
import com.ss.ugc.android.editor.core.avgSpeed

import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.frame.FrameLoader
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt


/**
 *  author : wenlongkai
 *  date : 2019-08-06 11:10
 *  description :
 */

class ItemFrameView : View {

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        initPaint()
    }

    companion object {
        const val TAG = "ItemFrameView"
        val HALF_TRANSITION_DIVIDER_WIDTH: Float = SizeUtil.dp2px(2.5F) / 2F
        val VIDEO_ANIM_MASK_COLOR_IN = Color.parseColor("#4D99E2E7")
        val VIDEO_ANIM_MASK_COLOR_OUT = Color.parseColor("#4D99E2E7")
        val VIDEO_ANIM_MASK_COLOR_GROUP = Color.parseColor("#4D99E2E7")


        const val ANIM_IN = "anim_in"
        const val ANIM_OUT = "anim_out"
        const val ANIM_ALL = "anim_all"
        const val ANIM_TYPE = "anim_type"
    }

    private var slot: NLETrackSlot? = null
    private var videoAnimInfo: NLEVideoAnimation? = null
    private val videoAnimMaskPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var trackWidth = TrackConfig.THUMB_WIDTH
    private var screenWidth = SizeUtil.getScreenWidth(TrackSdk.application.applicationContext)

    private var sourceDuration = 0L

    private var startDrawPosition = -screenWidth
    private var stopDrawPosition = screenWidth
    private var playHeadTime = 0

    private var leftClip: Float = 0F
    private var rightClip: Float = 0F
    private var clipWidth: Float = 0F

    private var isDrag = false
    private var loadPath = ""
    private val canvasRect = RectF()
    private val drawRect = Rect()

    private var style = MultiTrackLayout.TrackStyle.NONE
    private var isFooter: Boolean = false
    private var preSegmentTransitionDuration = 0L
    private val transitionOverLappingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clipPath = Path()
    private val preOverlapTrianglePath = Path()
    private val selfOverlapTrianglePath = Path()
    private var drawPreTransitionOverlap = true
    private var drawMyTransitionOverlap = true

    var frameFetcher: ((path: String, timestamp: Int) -> Bitmap?)? = null

    /**
     * 现在只加载有限的图片到内存，所以需要保存一下最后加载的图片，避免黑帧
     */

    private var lastBitmap: Bitmap? = null

    private fun initPaint() {
        val color = Color.parseColor("#66000000")
        transitionOverLappingPaint.color = color
        transitionOverLappingPaint.style = Paint.Style.FILL_AND_STROKE

        videoAnimMaskPaint.style = Paint.Style.FILL_AND_STROKE
    }

    fun updateData(
        slot: NLETrackSlot,
        scrollX: Int,
        preOverlapTransitionDuration: Long
    ) {
        this.slot = slot
        lastBitmap = null
        sourceDuration = NLESegmentVideo.dynamicCast(slot.mainSegment)?.resource?.duration!! / 1000
        trackWidth =
            (sourceDuration * TrackConfig.PX_MS / NLESegmentVideo.dynamicCast(slot.mainSegment).avgSpeed()).roundToInt()
        layoutParams.width = trackWidth
        leftClip = 0F
        rightClip = 0F
        clipWidth = 0F
        loadPath = FrameLoader.getFrameLoadPath(slot)
        startDrawPosition = scrollX - screenWidth
        stopDrawPosition = scrollX + screenWidth
//        if (slot.videoAnims != null && slot.videoAnims.isNotEmpty()) {
//            videoAnimInfo = slot.videoAnims[0]
//        }
        this.preSegmentTransitionDuration = preOverlapTransitionDuration
        requestLayout()
        internalInvalidate()
    }

    private fun internalInvalidate() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            invalidate()
        } else {
            postInvalidate()
        }
    }

    @MainThread
    fun updateScrollX(scrollX: Int) {
        startDrawPosition = scrollX - screenWidth
        stopDrawPosition = scrollX + screenWidth
        trackWidth =
            (sourceDuration * TrackConfig.PX_MS / (NLESegmentVideo.dynamicCast(slot?.mainSegment)?.avgSpeed()
                ?: 1.0f)).roundToInt()
        layoutParams.width = trackWidth
        playHeadTime = (scrollX / TrackConfig.PX_MS).toInt()
        internalInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrag) {
            drawDrag(canvas)
        } else {
            drawTrack(canvas)
        }
    }

    fun setIsFooter(isFooter: Boolean) {
        this.isFooter = isFooter
        invalidate()
    }

    fun setTrackStyle(style: MultiTrackLayout.TrackStyle) {
        this.style = style
        invalidate()
    }

    fun setDrawPreTransitionOverlap(draw: Boolean) {
        drawPreTransitionOverlap = draw
        postInvalidate()
    }

    fun setDrawMyTransitionOverlap(draw: Boolean) {
        drawMyTransitionOverlap = draw
        postInvalidate()
    }

    fun updateVideoAnimInfo(videoAnimInfo: NLEVideoAnimation?) {
        this.videoAnimInfo = videoAnimInfo
        postInvalidate()
    }

    fun updateVideoAnimInfoDuration(animDuration: Long) {
        videoAnimInfo?.let {
            //            videoAnimInfo = it.copy(duration = animDuration)
            postInvalidate()
        }
    }

    private fun drawDrag(canvas: Canvas) {
        val slot = slot ?: return
        val videoSeg = NLESegmentVideo.dynamicCast(slot.mainSegment)
        val startLeft = videoSeg.timeClipStart/1000 * TrackConfig.PX_MS / videoSeg.avgSpeed()
        canvas.save()
        canvas.translate(startLeft, 0F)
        drawRect.set(0, 0, TrackConfig.THUMB_WIDTH, TrackConfig.THUMB_HEIGHT)
        if (loadPath.isNotBlank()) {
//            val isImage = slot.segment.resourceType == DraftTypeUtils.MetaType.TYPE_VIDEO_IMAGE
            val isImage = slot.mainSegment.type == NLEResType.IMAGE
            val firstFrame = if (isImage) 0 else {
                val acc = FrameLoader.accurateToSecond(videoSeg.timeClipStart.toInt() / 1000)
                val resourceDuration = slot.mainSegment.resource.duration/1000
                if (acc > resourceDuration) {
                    resourceDuration.toInt()
                } else {
                    acc
                }
            }
            val bitmap = frameFetcher?.invoke(loadPath, firstFrame)
            bitmap?.let {
                canvas.drawBitmap(
                    bitmap,
                    null,
                    drawRect,
                    null
                )
            }
        }
        canvas.restore()
    }

    /**
     * 绘制视频轨道
     */

    private fun drawTrack(canvas: Canvas) {
        slot?.let {
            val videoSeg = NLESegmentVideo.dynamicCast(it.mainSegment)

            var frameIndex = 0
            val countPair = calculateCount(it)
            // 视频源起始时间计算 canvas 绘制区域的起始位置，由于 sourceTimeRange start 未与 speed 关联，需要去掉 speed 影响
            val startLeft = videoSeg.timeClipStart / 1000 * TrackConfig.PX_MS / videoSeg.avgSpeed()
            val initRight =
                startLeft + videoSeg.duration / 1000 * TrackConfig.PX_MS

            canvasRect.set(startLeft + leftClip, 0F, initRight + rightClip, height.toFloat())
//            canvas.clipRect(canvasRect)

            val preSegmentTransitionWidth =
                preSegmentTransitionDuration * TrackConfig.PX_MS

            var preTransitionDividerWidth = HALF_TRANSITION_DIVIDER_WIDTH
            if (preSegmentTransitionWidth != 0F) {
                preTransitionDividerWidth =
                    (HALF_TRANSITION_DIVIDER_WIDTH /
                            (height * 1F / sqrt(
                                height.toDouble().pow(2.0) +
                                        (preSegmentTransitionWidth / 2.0).pow(2.0)
                            ))).toFloat()
            }
            if (style != MultiTrackLayout.TrackStyle.NONE) {
                clipPath.reset()
                // 左上
                clipPath.moveTo(
                    canvasRect.left,
                    canvasRect.top
                )
                // 左下
                clipPath.lineTo(canvasRect.left, canvasRect.bottom)
                // 右下
                clipPath.lineTo(canvasRect.right, canvasRect.bottom)
                // 右上
                clipPath.lineTo(canvasRect.right, canvasRect.top)
                // 左上
                clipPath.lineTo(
                    canvasRect.left,
                    canvasRect.top
                )
            } else {
                clipPath.reset()
                // 左上
                clipPath.moveTo(
                    canvasRect.left + preSegmentTransitionWidth + preTransitionDividerWidth,
                    canvasRect.top
                )
                if (drawPreTransitionOverlap) {
                    // 左下
                    clipPath.lineTo(canvasRect.left + preTransitionDividerWidth, canvasRect.bottom)
                } else {
                    // 左下
                    clipPath.lineTo(
                        canvasRect.left + preSegmentTransitionWidth + preTransitionDividerWidth,
                        canvasRect.bottom
                    )
                }
                // 右下
                clipPath.lineTo(canvasRect.right - HALF_TRANSITION_DIVIDER_WIDTH, canvasRect.bottom)
                // 右上
                clipPath.lineTo(canvasRect.right - HALF_TRANSITION_DIVIDER_WIDTH, canvasRect.top)

                // 左上
                clipPath.lineTo(
                    canvasRect.left + preSegmentTransitionWidth + preTransitionDividerWidth,
                    canvasRect.top
                )

                it.endTransition?.let { transition ->
                    if (transition.effectSDKTransition.resourceFile.isBlank().not()) {
                        clipPath.reset()
                        // 左上
                        clipPath.moveTo(
                            canvasRect.left + preSegmentTransitionWidth + preTransitionDividerWidth,
                            canvasRect.top
                        )
                        if (drawPreTransitionOverlap) {
                            // 左下
                            clipPath.lineTo(
                                canvasRect.left + preTransitionDividerWidth,
                                canvasRect.bottom
                            )
                        } else {
                            // 左下
                            clipPath.lineTo(
                                canvasRect.left + preSegmentTransitionWidth + preTransitionDividerWidth,
                                canvasRect.bottom
                            )
                        }

                        var myTransitionDivider = HALF_TRANSITION_DIVIDER_WIDTH
                        var myTransitionWidth = 0F
                        if (transition.overlap) {
                            myTransitionWidth =
                                transition.transitionDuration / 1000 * TrackConfig.PX_MS
                            myTransitionDivider = (HALF_TRANSITION_DIVIDER_WIDTH /
                                    (height * 1F / sqrt(
                                        height.toDouble().pow(2.0) +
                                                (myTransitionWidth / 2.0).pow(2.0)
                                    ))).toFloat()
                        }

                        // 右下
                        clipPath.lineTo(
                            canvasRect.right - myTransitionWidth - myTransitionDivider,
                            canvasRect.bottom
                        )

                        if (drawMyTransitionOverlap) {
                            // 右上
                            clipPath.lineTo(
                                canvasRect.right - myTransitionDivider,
                                canvasRect.top
                            )
                        } else {
                            // 右上
                            clipPath.lineTo(
                                canvasRect.right - myTransitionWidth - myTransitionDivider,
                                canvasRect.top
                            )
                        }

                        // 左上
                        clipPath.lineTo(
                            canvasRect.left + preSegmentTransitionWidth + preTransitionDividerWidth,
                            canvasRect.top
                        )
                    }
                }
            }

            canvas.clipPath(clipPath)

            if (isFooter) {
                return
            }

//            BLog.d(TAG, " this count is " + countPair.first)
            // 绘制每段视频在可视区域中的帧
            var laterTargetCount = 0
            for (i in 0..countPair.first) {
                val frameStart = i * TrackConfig.THUMB_WIDTH
                // 判断当前帧是否在可绘制矩形內
                if (frameStart.toFloat() in
                    canvasRect.left - TrackConfig.THUMB_WIDTH..canvasRect.right + TrackConfig.THUMB_HEIGHT
                ) {
                    laterTargetCount++
                    val targetStart =
                        (it.startTime / 1000 * TrackConfig.PX_MS +
                                clipWidth + laterTargetCount * TrackConfig.THUMB_WIDTH + leftClip).toInt()
                    // 当前帧是否在在可见区域內
                    if (targetStart in startDrawPosition..stopDrawPosition) {
                        if (loadPath.isNotBlank()) {
                            var bitmap = if (it.mainSegment.type == NLEResType.VIDEO) {
                                getPositionBitmap(
                                    i,
                                    loadPath,
                                        NLESegmentVideo.dynamicCast(it.mainSegment).avgSpeed()
                                )
                            } else {
                                frameFetcher?.invoke(loadPath, 0)
                            }
                            if (null == bitmap) bitmap = lastBitmap
                            if (null != bitmap) {
                                lastBitmap = bitmap
                                drawRect.set(
                                    frameStart,
                                    0,
                                    frameStart + TrackConfig.THUMB_WIDTH,
                                    TrackConfig.THUMB_HEIGHT
                                )
                                canvas.drawBitmap(
                                    bitmap,
                                    null,
                                    drawRect,
                                    null
                                )
                                frameIndex++
                            }
                        }
                    }
                }
            }

            if (videoAnimInfo != null && videoAnimInfo!!.duration > 0
                && videoAnimInfo?.segment?.resource?.resourceFile?.isEmpty() == false
            ) {
                videoAnimMaskPaint.color = when (videoAnimInfo!!.getExtra(ANIM_TYPE)) {
                    ANIM_ALL -> VIDEO_ANIM_MASK_COLOR_GROUP
                    ANIM_OUT -> VIDEO_ANIM_MASK_COLOR_OUT
                    else -> VIDEO_ANIM_MASK_COLOR_IN
                }

                videoAnimMaskPaint.color = VIDEO_ANIM_MASK_COLOR_IN
                val maskWidth = min(
                    (videoAnimInfo!!.duration.toFloat() / 1000 * TrackConfig.PX_MS),
                    canvasRect.width()
                )

                if (videoAnimInfo!!.getExtra(ANIM_TYPE) == ANIM_OUT) {
                    canvas.drawRect(
                        canvasRect.right - maskWidth,
                        canvasRect.top,
                        canvasRect.right,
                        canvasRect.bottom,
                        videoAnimMaskPaint
                    )
                } else {
                    canvas.drawRect(
                        canvasRect.left,
                        canvasRect.top,
                        canvasRect.left + maskWidth,
                        canvasRect.bottom,
                        videoAnimMaskPaint
                    )
                }
            }

            if (style != MultiTrackLayout.TrackStyle.NONE) {
                if (preSegmentTransitionWidth != 0F) {
                    preOverlapTrianglePath.reset()
                    preOverlapTrianglePath.moveTo(canvasRect.left, 0F)
                    preOverlapTrianglePath.lineTo(canvasRect.left + preSegmentTransitionWidth, 0F)
                    preOverlapTrianglePath.lineTo(canvasRect.left, height.toFloat())
                    preOverlapTrianglePath.lineTo(canvasRect.left, 0F)
                    canvas.drawPath(preOverlapTrianglePath, transitionOverLappingPaint)
                }

                it.endTransition?.let { transition ->
                    val myTransitionWidth: Float
                    if (transition.overlap) {
                        myTransitionWidth = transition.transitionDuration / 1000 * TrackConfig.PX_MS
                        selfOverlapTrianglePath.reset()
                        selfOverlapTrianglePath.moveTo(
                            canvasRect.right,
                            0F
                        )
                        selfOverlapTrianglePath.lineTo(
                            canvasRect.right - myTransitionWidth,
                            height.toFloat()
                        )
                        selfOverlapTrianglePath.lineTo(
                            canvasRect.right,
                            height.toFloat()
                        )
                        selfOverlapTrianglePath.lineTo(
                            canvasRect.right,
                            0F
                        )
                        canvas.drawPath(selfOverlapTrianglePath, transitionOverLappingPaint)
                    }
                }
            }

//            BLog.d(TAG, " draw bitmap count $frameIndex" + " canvas left is " + canvasRect.left)
        }
    }

    private fun calculateCount(segmentInfo: NLETrackSlot): Pair<Int, Float> {
        val videoSegment = NLESegmentVideo.dynamicCast(segmentInfo.mainSegment)
        var framesCount =
                (videoSegment.resource.duration / 1000 / TrackConfig.FRAME_DURATION.toFloat() / videoSegment.avgSpeed()).toInt()
        val realCount =
           videoSegment.resource.duration / 1000 / TrackConfig.FRAME_DURATION.toFloat() / videoSegment.avgSpeed().toInt()
        val lastScale = realCount - framesCount
        if (lastScale > 0) {
            framesCount++
        }
        return Pair(framesCount, lastScale)
    }

    private fun getPositionBitmap(position: Int, path: String, speed: Float): Bitmap? {
        var tempPosition = position
        var repeatCount = 0
        var bitmap: Bitmap? = null
        var frameTime: Int
        // 最多回溯40张图片，后续需要减少取帧粒度
        while (bitmap == null && tempPosition >= 0 && repeatCount <= 20) {
            frameTime = calculateFrame(tempPosition, speed)
            bitmap = frameFetcher?.invoke(path, frameTime)
            tempPosition -= 1
            repeatCount++
        }
        // 如果没有拿到 bitmap 则使用第一帧避免黑屏
        if (bitmap == null) {
            bitmap = frameFetcher?.invoke(path, 1000)
        }
        return bitmap
    }


    /**
     * 帧复用，根据缩放比例，不同帧复用策略
     */

    private fun calculateFrame(position: Int, speed: Float): Int {
        return ((position * TrackConfig.FRAME_DURATION * speed / 1000.0).roundToInt() * 1000)
    }

    fun clipLeft(leftClip: Float) {
        this.leftClip = leftClip
        invalidate()
    }

    fun clipRight(rightClip: Float) {
        this.rightClip = rightClip
        invalidate()
    }

    fun updateClipWidth(clipWidth: Float) {
        this.clipWidth = clipWidth
//        BLog.d(TAG, "clip width is $clipWidth")
        invalidate()
    }

    fun beginDrag() {
        isDrag = true
        invalidate()
    }

    fun endDrag() {
        isDrag = false
        invalidate()
    }

    fun enableAnimationLabel(enable: Boolean) {

    }
}

