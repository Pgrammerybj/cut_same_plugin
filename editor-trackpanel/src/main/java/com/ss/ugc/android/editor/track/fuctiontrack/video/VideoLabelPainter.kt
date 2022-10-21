package com.ss.ugc.android.editor.track.fuctiontrack.video

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.bytedance.ies.nle.editor_jni.NLESegmentVideo
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.base.theme.ThemeStore
import com.ss.ugc.android.editor.core.api.video.CURVE_SPEED_NAME
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.track.data.SpeedInfo
import com.ss.ugc.android.editor.track.fuctiontrack.TrackGroup
import com.ss.ugc.android.editor.track.fuctiontrack.TrackItemHolder
import com.ss.ugc.android.editor.track.utils.FormatUtil
import com.ss.ugc.android.editor.track.utils.SizeUtil
import java.util.Locale

internal class VideoLabelPainter(private val view: VideoItemView) {
    private val context = view.context

    private val cornerPath = Path()
    private val bgPaint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textSize = SizeUtil.dp2px(8F).toFloat()
        style = Paint.Style.FILL
    }
    private val maskPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#6603bac8")
    }
    private val textBounds = Rect()

    private var nleTrackSlot: NLETrackSlot? = null

    @Suppress("ComplexMethod")
    fun draw(
        slot: NLETrackSlot,
        canvas: Canvas,
        parentScrollX: Int,
        animDuration: Long = 0L
    ) {
        this.nleTrackSlot = slot
        // 计算所需绘制的边界
        // 比如一个9像素的View，只有最后1/3在屏幕上，那就只需要绘制像素区间[7,9]即可
        val leftWidthClip = if (view.isItemSelected) {
            view.left + view.clipLeft
        } else {
            view.right.toFloat()
        }
        val paintLeft =
            if (parentScrollX > leftWidthClip) parentScrollX - leftWidthClip else 0F

        val viewRight = if (view.clipLength == 0F) {
            view.measuredWidth.toFloat()
        } else {
            view.clipLength - view.clipLeft
        }
        val scrollRight = parentScrollX +
                TrackGroup.getPaddingHorizontal().toFloat() - view.clipLeft
        val paintRight = if (scrollRight < viewRight) scrollRight else viewRight
        val convertToSegmentVideo = NLESegmentVideo.dynamicCast(slot.mainSegment)
        when (view.labelType) {
            VideoItemView.LabelType.NONE -> {
                var left = paintLeft + LEFT_LABEL_MARGIN
                //时长
                if (view.isItemSelected) {//每次绘制以后往后叠加绘制
                    left += drawTimeLabel(view, slot, canvas, left, paintRight) + LEFT_LABEL_MARGIN
                }
                //变声
                drawIconWithText(
                    canvas, R.drawable.ic_track_audio_filter,
                    slot.audioFilter?.segment?.filterName, left
                )?.let {
                    left += it + LEFT_LABEL_MARGIN//每次绘制以后往后叠加绘制
                }
                //变速
                SpeedInfo().apply {
                    mode =
                        if (convertToSegmentVideo.segCurveSpeedPoints.isEmpty()) SpeedInfo.NORMAL_SPEED else SpeedInfo.CURVE_SPEED
                    normalSpeed = convertToSegmentVideo.absSpeed
                    aveSpeed = convertToSegmentVideo.curveAveSpeed.toFloat()
                    name = convertToSegmentVideo.getExtra(CURVE_SPEED_NAME)
                    val speedWidth = drawSpeedLabel(canvas, this, left)
                    if (speedWidth > 0) {//每次绘制以后往后叠加绘制
                        left += speedWidth + LEFT_LABEL_MARGIN
                    }
                }
            }
        }


    }

    private fun drawTimeLabel(
        view: VideoItemView,
        slot: NLETrackSlot,
        canvas: Canvas,
        paintLeft: Float,
        paintRight: Float
    ): Float {
        val clipLeft = view.clipLeft
        val clipLength = if (view.clipLength != 0F) {
            view.clipLength
        } else {
            slot.duration / 1000 * view.timelineScale
        }

        val duration = ((clipLength - clipLeft) / view.timelineScale).toLong()

        val top = LEFT_LABEL_MARGIN_VERTICAL.toFloat()
        val bottom = LEFT_LABEL_HEIGHT + top
        val right = TIME_LABEL_WIDTH + paintLeft

        drawCornerBackground(
            canvas, paintLeft, top, right, bottom,
            CornerSide.RIGHT
        )

        val text = FormatUtil.formatLabelTime(duration)
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(
            text,
            paintLeft + (TIME_LABEL_WIDTH - textBounds.width()) / 2F,
            getLabelTextY(top, LEFT_LABEL_HEIGHT.toFloat()),
            textPaint
        )
        return right - paintLeft
    }

    private fun drawMuteLabel(canvas: Canvas, drawHeight: Float, paintLeft: Float) {
        val drawable = context.getDrawable(R.drawable.ic_mute_n) ?: return
        val bottom = drawHeight - LEFT_LABEL_MARGIN_VERTICAL
        val top = bottom - LEFT_LABEL_HEIGHT
        drawLeftLabel(canvas, top, bottom, drawable, "", paintLeft)
    }

    private fun drawSpeedLabel(canvas: Canvas, speedInfo: SpeedInfo, paintLeft: Float): Float {
        val drawable = context.getDrawable(R.drawable.ic_speed_n) ?: return 0f
        var normalChangeSpeed = drawable
        var curveChangeSpeed = drawable
        ThemeStore.getCustomViceTrackNormalChangeSpeedIcon()
            ?.apply { normalChangeSpeed = context.getDrawable(this) ?: return 0f }

        ThemeStore.getCustomViceTrackCurveChangeSpeedIcon()
            ?.apply { curveChangeSpeed = context.getDrawable(this) ?: return 0f }

        if (speedInfo.mode == SpeedInfo.NORMAL_SPEED) {
            if (speedInfo.normalSpeed != 1.0F) {
                val text = String.format(Locale.ENGLISH, "%.1fx", speedInfo.normalSpeed)
                return drawLeftTopLabel(canvas, normalChangeSpeed, text, paintLeft)
            }
        } else {
            return drawLeftTopLabel(canvas, curveChangeSpeed, speedInfo.name, paintLeft)
        }
        return 0f
    }

    private fun drawIconWithText(
        canvas: Canvas,
        @DrawableRes drawableId: Int,
        text: String?,
        paintLeft: Float
    ): Float? {
        if (text.isNullOrEmpty()) {
            return null
        }
        val drawable = context.getDrawable(drawableId) ?: return null
        return drawLeftTopLabel(canvas, drawable, text, paintLeft)
    }

    private fun drawBeautyLabel(canvas: Canvas, paintLeft: Float) {
        val drawable = context.getDrawable(R.drawable.ic_beauty_n) ?: return
        drawLeftTopLabel(canvas, drawable, "", paintLeft)
    }

    private fun drawFilterLabel(canvas: Canvas, paintLeft: Float) {
        val drawable = context.getDrawable(R.drawable.ic_fliter_n) ?: return
        drawLeftTopLabel(canvas, drawable, "", paintLeft)
    }


    private fun drawLeftTopLabel(
        canvas: Canvas,
        drawable: Drawable,
        text: String,
        paintLeft: Float
    ): Float {
        val top = LEFT_LABEL_MARGIN_VERTICAL.toFloat()
        val bottom = top + LEFT_LABEL_HEIGHT

        return drawLeftLabel(canvas, top, bottom, drawable, text, paintLeft)
    }

    private fun drawLeftLabel(
        canvas: Canvas,
        top: Float,
        bottom: Float,
        drawable: Drawable,
        text: String,
        paintLeft: Float
    ): Float {
        textPaint.typeface = Typeface.DEFAULT
        val textWidth = textPaint.measureText(text)

        val right = paintLeft + LEFT_LABEL_ICON_WIDTH + textWidth
        drawCornerBackground(
            canvas, paintLeft, top, right, bottom,
            CornerSide.LEFT
        )

        canvas.save()
        val iconStart = paintLeft + LEFT_LABEL_ICON_MARGIN_LEFT
        val iconTop = top + (bottom - top - drawable.intrinsicHeight) / 2
        canvas.translate(iconStart, iconTop)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        canvas.restore()

        canvas.drawText(
            text,
            iconStart + drawable.intrinsicWidth + LEFT_LABEL_TEXT_MARGIN_LEFT,
            getLabelTextY(top, LEFT_LABEL_HEIGHT.toFloat()),
            textPaint
        )
        return right - paintLeft
    }

    private fun drawCornerBackground(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        side: CornerSide
    ) {
        val startX: Float
        val endX: Float
        val cornerX: Float
        if (side == CornerSide.LEFT) {
            startX = left
            endX = right
            cornerX = LEFT_LABEL_CORNER
        } else {
            startX = right
            endX = left
            cornerX = -LEFT_LABEL_CORNER
        }

        cornerPath.reset()
        cornerPath.moveTo(startX, top)
        cornerPath.lineTo(endX - cornerX, top)
        cornerPath.quadTo(endX, top, endX, top + LEFT_LABEL_CORNER)
        cornerPath.lineTo(endX, bottom - LEFT_LABEL_CORNER)
        cornerPath.quadTo(endX, bottom, endX - cornerX, bottom)
        cornerPath.lineTo(startX, bottom)
        cornerPath.close()

        bgPaint.color =
            LABEL_BG_COLOR
        canvas.drawPath(cornerPath, bgPaint)
        cornerPath.reset()
    }

    private fun getLabelTextY(labelTop: Float, labelHeight: Float): Float {
        return labelTop + (labelHeight - textPaint.descent() - textPaint.ascent()) / 2F
    }

    companion object {
        private val TIME_LABEL_WIDTH = SizeUtil.dp2px(30F)

        private val LEFT_LABEL_ICON_WIDTH = SizeUtil.dp2px(24F)
        private val LEFT_LABEL_HEIGHT = SizeUtil.dp2px(12F)
        private val LEFT_LABEL_MARGIN = SizeUtil.dp2px(3F)
        private val LEFT_LABEL_CORNER = SizeUtil.dp2px(3F).toFloat()
        private val LEFT_LABEL_MARGIN_VERTICAL = SizeUtil.dp2px(2F)
        private val LEFT_LABEL_ICON_MARGIN_LEFT = SizeUtil.dp2px(4F)
        private val LEFT_LABEL_TEXT_MARGIN_LEFT = SizeUtil.dp2px(1F)

        private val LABEL_BG_COLOR = Color.parseColor("#66101010")
    }

    private enum class CornerSide {
        LEFT, RIGHT
    }
}
