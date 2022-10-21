package com.ss.ugc.android.editor.track.fuctiontrack

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.view.View
import com.ss.ugc.android.editor.base.utils.SizeUtil
import kotlin.math.abs

class TagPainter(private val view: TrackItemView) {

    private val textBounds = Rect()
    private val cornerPath = Path()
    private val tagRect = RectF()
    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
    }
    private val textPaint = TextPaint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textSize = TAG_TEXT_SIZE
        strokeWidth = SizeUtil.dp2px(1F).toFloat()
        typeface = Typeface.DEFAULT_BOLD
        style = Paint.Style.FILL
    }
    private var viewHeight = 0
    var tagWidth = 0F

    fun draw(canvas: Canvas, text: String, tagTextColor: Int) {
        viewHeight = ((view as? View)?.measuredHeight ?: 0) + SizeUtil.dp2px(1F)
        textPaint.color = tagTextColor
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        tagWidth = textBounds.width() + TAG_TEXT_PADDING * 2
        tagRect.set(
            TAG_LEFT_MARGIN,
            (viewHeight - TAG_HEIGHT) / 2,
            TAG_LEFT_MARGIN + tagWidth,
            (viewHeight - TAG_HEIGHT) / 2 + TAG_HEIGHT
        )
        paint.color = Color.WHITE
        canvas.drawRect(tagRect, paint)
        paint.color = tagTextColor
        drawCornerBackground(canvas)
        canvas.drawText(text, TAG_LEFT_MARGIN + tagWidth / 2 - textBounds.width() / 2, getBaseLine(), textPaint)
    }

    private fun drawCornerBackground(canvas: Canvas) {
        val drawCorner: (Float, Float, Float, Float) -> Unit = { x, y, xRadius, yRadius ->
            cornerPath.reset()
            cornerPath.moveTo(x, y)
            cornerPath.lineTo(x + xRadius, y)
            cornerPath.quadTo(x, y, x, y + yRadius)
            cornerPath.close()
            canvas.drawPath(cornerPath, paint)
        }
        drawCorner(
            TAG_LEFT_MARGIN,
            (viewHeight - TAG_HEIGHT) / 2,
            TrackItemHolder.CORNER_WIDTH,
            TrackItemHolder.CORNER_WIDTH
        )
        drawCorner(
            TAG_LEFT_MARGIN,
            (viewHeight + TAG_HEIGHT) / 2,
            TrackItemHolder.CORNER_WIDTH,
            -TrackItemHolder.CORNER_WIDTH
        )
        drawCorner(
            TAG_LEFT_MARGIN + tagWidth,
            (viewHeight - TAG_HEIGHT) / 2,
            -TrackItemHolder.CORNER_WIDTH,
            TrackItemHolder.CORNER_WIDTH
        )
        drawCorner(
            TAG_LEFT_MARGIN + tagWidth,
            (viewHeight + TAG_HEIGHT) / 2,
            -TrackItemHolder.CORNER_WIDTH,
            -TrackItemHolder.CORNER_WIDTH
        )
        cornerPath.reset()
    }

    private fun getBaseLine(): Float {
        return viewHeight / 2 + (abs(textPaint.ascent()) - textPaint.descent()) / 2
    }

    companion object {
        private val TAG_LEFT_MARGIN = SizeUtil.dp2px(8F).toFloat()
        private val TAG_HEIGHT = SizeUtil.dp2px(12F).toFloat()
        private val TAG_TEXT_SIZE = SizeUtil.dp2px( 8F).toFloat()
        private val TAG_TEXT_PADDING = SizeUtil.dp2px(3F).toFloat()
//        val TAG_ALL = getString(R.string.all_)
//        val TAG_MAIN = getString(R.string.main_track)
//        val TAG_SUB = getString(R.string.pip_2)
    }
}
