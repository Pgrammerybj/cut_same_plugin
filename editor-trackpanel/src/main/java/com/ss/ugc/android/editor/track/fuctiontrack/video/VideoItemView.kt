package com.ss.ugc.android.editor.track.fuctiontrack.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.ColorInt
import com.bytedance.ies.nle.editor_jni.NLEResType
import com.bytedance.ies.nle.editor_jni.NLESegmentVideo
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.core.avgSpeed
import com.ss.ugc.android.editor.track.frame.FrameLoader
import com.ss.ugc.android.editor.track.fuctiontrack.TrackGroup
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.BaseTrackKeyframeItemView
import com.ss.ugc.android.editor.track.widget.TrackConfig
import kotlin.math.ceil

internal class VideoItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseTrackKeyframeItemView(context, attrs, defStyleAttr) {
    override var drawDivider: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    override var isItemSelected: Boolean = false

    override var clipLength: Float = 0F
    override var timelineScale: Float = TrackConfig.PX_MS

    @ColorInt
    override var bgColor: Int = Color.TRANSPARENT

    private val paint = Paint()

    private val bitmapRect = Rect()
    private val drawRect = RectF()
    private val canvasClipBounds = Rect()

    private val labelPainter = VideoLabelPainter(this)
    private val dividerPainter = DividerPainter(this)

    internal var labelType =
        LabelType.NONE
        private set

    private var frameFetcher: ((path: String, timestamp: Int) -> Bitmap?)? = null
    private var lastBitmap: Bitmap? = null

    private var videoAnimDuration: Long = 0L

    internal fun setFrameFetcher(fetcher: (path: String, timestamp: Int) -> Bitmap?) {
        frameFetcher = fetcher
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            if (!isItemSelected) drawOn(it)
        }
    }

    override fun drawOn(canvas: Canvas) {
        val parent = parentView
        val segment = nleTrackSlot

        if (parent == null || segment == null) return

        drawFrames(canvas, parent, segment)
        labelPainter.draw(segment, canvas, parent.scrollX, videoAnimDuration)

        canvas.getClipBounds(canvasClipBounds)
        dividerPainter.draw(canvas, canvasClipBounds)
        super.drawOn(canvas)
    }

    private fun drawFrames(
        canvas: Canvas,
        parent: ViewGroup,
        slot: NLETrackSlot
    ) {
        // 通过【视频自身的起始点】和【左侧裁剪长度】计算出需要绘制的第一帧的offset
        // 假设不处于裁剪状态，且每1秒绘制1帧，每帧完整绘制的情况下是40dp宽，sourceTimeRange.start = 200，
        // 那么offset = 200 / 1000 * 40，也就是说第一帧只绘制32dp
        // 因为要考虑裁剪状态，所以这里换算成像素值来计算
        val convertToSegmentVideo = NLESegmentVideo.dynamicCast(slot.mainSegment)
        val startInPx = convertToSegmentVideo.timeClipStart/1000 * timelineScale / convertToSegmentVideo.avgSpeed()
        val firstPicOffset =
            (startInPx + clipLeft) % VideoItemHolder.ITEM_WIDTH
        var drawCurrInPx = -firstPicOffset

        // 计算出需要绘制的区域
        val scrollX = parent.scrollX
        // 算上长按拖移的距离
        val leftWidthClip = left + translationX + clipLeft
        val paintLeft =
            if (scrollX > leftWidthClip) scrollX - leftWidthClip else 0F
        val right = if (clipLength != 0F) {
            clipLength - this.clipLeft
        } else {
            measuredWidth.toFloat()
        }
        val scrollRight = scrollX + TrackGroup.getPaddingHorizontal() - clipLeft
        val paintRight = if (scrollRight < right) scrollRight else right

        while (true) {
            val tmpInPx =
                drawCurrInPx + VideoItemHolder.ITEM_WIDTH
            if (tmpInPx < paintLeft) {
                drawCurrInPx = tmpInPx
            } else {
                break
            }
        }

        // 绘制到View的右侧边缘，如果正在裁剪，那么是按裁剪长度来计算
        // 如果不是在裁剪，那就是按当前segment的duration来计算
        val loadPath = FrameLoader.getFrameLoadPath(slot)
        paint.alpha = (alpha * 255).toInt()
        while (drawCurrInPx < paintRight) {
            // 此帧结束绘制的位置
            val drawNextInPx =
                drawCurrInPx + VideoItemHolder.ITEM_WIDTH

            var timestamp: Int
            // 如果是图片，则取第一帧就足够了
            val bitmap = if (convertToSegmentVideo.type == NLEResType.IMAGE) {
                // 如果是图片的话，永远只使用一张图片，不需要再取
                lastBitmap ?: frameFetcher?.invoke(loadPath, 0)
            } else {
                // 此帧对应的视频时间戳，如果正在裁剪，那么sourceTimeRange.start还没有更新，
                // 所以要加上左侧裁剪长度去计算时间戳
                val drawCurrInTime = (drawCurrInPx + clipLeft) / timelineScale * convertToSegmentVideo.avgSpeed() +
                        convertToSegmentVideo.timeClipStart/1000
                // 每秒取一帧，所以这里计算一下，精确到秒
                timestamp = FrameLoader.accurateToSecond(
                    ceil(drawCurrInTime).toInt()
                )
                // 如果计算出来的时间大于最大时长，就使用小于最大时长的最大秒数
                if (timestamp > convertToSegmentVideo.resource.duration/1000) {
                    timestamp = (convertToSegmentVideo.resource.duration/1000 ).toInt()
                }

                // 如果cache中没有，那就使用最近得到的图片
                frameFetcher?.invoke(loadPath, timestamp) ?: lastBitmap
            }

            if (bitmap != null) {
                lastBitmap = bitmap

                bitmapRect.set(
                    0,
                    VideoItemHolder.BITMAP_TOP,
                    bitmap.width,
                    VideoItemHolder.BITMAP_BOTTOM
                )
                drawRect.set(
                    drawCurrInPx,
                    0F,
                    drawNextInPx,
                    VideoItemHolder.ITEM_HEIGHT.toFloat()
                )
                canvas.drawBitmap(bitmap, bitmapRect, drawRect, paint)
            }

            drawCurrInPx = drawNextInPx
        }
    }

    override fun setSegment(slot: NLETrackSlot) {
        //NLE重写了equals 只比对value 不再比对内存地址 导致这部分逻辑异常
        if (slot === this.nleTrackSlot) return
        this.nleTrackSlot = slot
        lastBitmap = null
        videoAnimDuration = 0
        if (isItemSelected) parentView?.invalidate() else invalidate()
    }

    fun updateLabelType(type: LabelType) {
        this.labelType = type
        videoAnimDuration = 0
        if (isItemSelected) parentView?.invalidate() else invalidate()
    }



    /**
     * 拖拽移动的过程中刷新
     */
    override fun setTranslationX(translationX: Float) {
        super.setTranslationX(translationX)
        invalidate()
    }

    override fun onSetAlpha(alpha: Int): Boolean = true

    enum class LabelType {
        NONE,
        FILTER,
        ADJUST,
        BEAUTY,
        VIDEO_ANIM
    }
}
