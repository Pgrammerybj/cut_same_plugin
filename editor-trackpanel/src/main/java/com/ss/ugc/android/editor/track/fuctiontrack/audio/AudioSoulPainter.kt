package com.ss.ugc.android.editor.track.fuctiontrack.audio

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.utils.SizeUtil
import kotlin.math.max

@Suppress("DEPRECATION")
val BEAT_COLOR = ContextCompat.getColor(TrackSdk.application,R.color.beat_color)

class AudioSoulPainter {
    private val paint = Paint().apply {
        strokeWidth = SizeUtil.dp2px(1f).toFloat()
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val path = Path()
    private val heightScale = SizeUtil.dp2px(13F)

    private var startOffset = 0F
    private var speed = 1F

    private var beatRadius = SizeUtil.dp2px(1.5F).toFloat()

    fun setBeatRadius(radius: Float) {
        beatRadius = radius
    }

    /**
     * `Segment#sourceTimeRange#start != 0`的情况下，绘制需要一个偏移量
     *
     * 假设`Segment#sourceTimeRange#star = 1000ms`，那么第1001ms的点就应该绘制在1处
     */
    fun setStartOffset(offset: Float, speed: Float) {
        startOffset = offset
        this.speed = speed
    }

    fun drawWave(
        canvas: Canvas,
        rect: RectF,
        wavePoints: List<Pair<Long, Float>>,
        timelineScale: Float,
        @ColorInt color: Int
    ) {
        if (wavePoints.isEmpty()) return

        // 前后多取一个点，避免在放大时间轨道的情况下短一截
        val intervalInPx = 33 * timelineScale / speed
        val left = rect.left - intervalInPx
        val right = rect.right + intervalInPx
        val points = wavePoints.mapNotNull {
            val x = (it.first * timelineScale - startOffset) / speed
            // 如果是0的话，也给画条线
            val waveCrest = max(it.second * heightScale, 0.5F)
            if (x in left..right) x to waveCrest else null
        }

        paint.color = color
        path.reset()
        val centerY = rect.centerY()
        points.forEachIndexed { index, pair ->
            if (index == 0) {
                path.moveTo(pair.first, centerY - pair.second)
                return@forEachIndexed
            }
            path.moveTo(pair.first, centerY - pair.second)
//            path.moveTo(pair.first, centerY - pair.second)

            path.lineTo(pair.first, pair.second + centerY)
        }

//        points.asReversed().forEach {
//            path.lineTo(it.first, it.second + centerY)
//        }
//        path.close()
        canvas.drawPath(path, paint)
        path.reset()
    }

    fun drawBeats(
        canvas: Canvas,
        rect: RectF,
        beats: Set<Long>,
        timelineScale: Float,
        @ColorInt color: Int
    ) {
        paint.color = color
        beats.map { it * timelineScale }.forEach {
            val relativeTime = (it - startOffset) / speed
            if (relativeTime >= rect.left && relativeTime <= rect.right) {
                canvas.drawCircle(relativeTime, rect.centerY(), beatRadius, paint)
            }
        }
    }
}
