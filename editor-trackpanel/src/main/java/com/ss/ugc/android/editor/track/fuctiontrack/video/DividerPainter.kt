package com.ss.ugc.android.editor.track.fuctiontrack.video

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import com.ss.ugc.android.editor.track.fuctiontrack.TrackItemHolder
import com.ss.ugc.android.editor.track.fuctiontrack.TrackItemView
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.utils.drawRect

class DividerPainter(private val view: TrackItemView) {
    private val paint = Paint().apply { isAntiAlias = true }
    private val cornerPath = Path()

    fun draw(canvas: Canvas, rect: Rect) {
        val canDrawDivider = rect.width() - DIVIDER_WIDTH * 2 > 0
        if (view.drawDivider && canDrawDivider) {
            paint.color = TrackItemHolder.PARENT_BG_COLOR
            canvas.drawRect(
                rect.right - DIVIDER_WIDTH,
                rect.top,
                rect.right,
                rect.bottom,
                paint
            )

            canvas.drawRect(
                rect.left,
                rect.top,
                rect.left + DIVIDER_WIDTH,
                rect.bottom,
                paint
            )
        }

        if (!view.isItemSelected) {
            paint.color = TrackItemHolder.PARENT_BG_COLOR
            val left: Float
            val right: Float
            val top: Float = rect.top.toFloat()
            val bottom: Float = rect.bottom.toFloat()
            if (view.drawDivider && canDrawDivider) {
                left = (rect.left + DIVIDER_WIDTH).toFloat()
                right = (rect.right - DIVIDER_WIDTH).toFloat()
            } else {
                left = rect.left.toFloat()
                right = rect.right.toFloat()
            }

            val drawCorner: (Float, Float, Float, Float) -> Unit = { x, y, xRadius, yRadius ->
                cornerPath.reset()
                cornerPath.moveTo(x, y)
                cornerPath.lineTo(x + xRadius, y)
                cornerPath.quadTo(x, y, x, y + yRadius)
                cornerPath.close()
                canvas.drawPath(cornerPath, paint)
            }

            drawCorner(left, top, TrackItemHolder.CORNER_WIDTH, TrackItemHolder.CORNER_WIDTH)
            drawCorner(left, bottom, TrackItemHolder.CORNER_WIDTH, -TrackItemHolder.CORNER_WIDTH)
            drawCorner(right, top, -TrackItemHolder.CORNER_WIDTH, TrackItemHolder.CORNER_WIDTH)
            drawCorner(right, bottom, -TrackItemHolder.CORNER_WIDTH, -TrackItemHolder.CORNER_WIDTH)
            cornerPath.reset()
        }
    }

    companion object {
        private val DIVIDER_WIDTH = SizeUtil.dp2px(1F)
    }
}
