package com.ss.ugc.android.editor.track.fuctiontrack.audio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.track.fuctiontrack.audio.AudioItemHolder.Companion.AUDIO_WAVE_COLOR
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.widget.EditScroller
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal class AudioWaveCollectScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : EditScroller(context, attrs, defStyleAttr) {
    private var halfScreenWidth = SizeUtil.getScreenWidth(context) / 2F
    private var waveCollect: AudioWaveCollect? = null

    private val paint = Paint().apply {
        isAntiAlias = true
    }
    private val textPaint = TextPaint().apply {
        color = Color.WHITE
        textSize = SizeUtil.dp2px(11F).toFloat()
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val textBounds = Rect()
    private val drawRect = RectF()

    private val beatRadius = SizeUtil.dp2px(1.5F).toFloat()

    private val painter = AudioSoulPainter().apply { setBeatRadius(beatRadius) }

    @Suppress("DEPRECATION")
    @ColorInt
    private val maskColor = resources.getColor(R.color.transparent_30p)

    private var addIcon: Drawable? = null
    private var musicIcon: Drawable? = null

    fun updateHalfScreenWidth() {
        halfScreenWidth = SizeUtil.getScreenWidth(context) / 2F
    }

    override fun updateTotalDuration(duration: Long) {
        super.updateTotalDuration(duration)
        postInvalidate()
    }

    override fun checkAddView(child: View) {
    }

    @MainThread
    fun setWaveCollect(waveCollect: AudioWaveCollect) {
        this.waveCollect = waveCollect
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        canvas ?: return
        val collection = waveCollect ?: return

        val left = max(scrollX - halfScreenWidth, 0F)
        val right = min(scrollX + halfScreenWidth, desireMaxScrollX.toFloat())
        drawRect.set(left, 0F, right, measuredHeight.toFloat())

        canvas.save()
        canvas.translate(halfScreenWidth, 0F)

        paint.color = MUSIC_TRACK_BG_COLOR
        canvas.drawRect(drawRect, paint)

        if (collection.isEmpty()) {
            drawTitle(canvas, R.drawable.add_music_icon, R.string.add_audio)
        } else {
            collection.wavePoints.forEach { points ->
                painter.drawWave(canvas, drawRect, points, timelineScale, AUDIO_WAVE_COLOR)
            }

            drawRect.set(left, measuredHeight - beatRadius * 3, right, measuredHeight - beatRadius)
            painter.drawBeats(
                canvas, drawRect, collection.beats, timelineScale,
                BEAT_COLOR
            )

            paint.color = maskColor
            drawRect.set(left, 0F, right, measuredHeight.toFloat())
            canvas.drawRect(drawRect, paint)
            drawTitle(canvas, R.drawable.music_ic_collection, R.string.music_collection)
        }
        canvas.restore()
    }

    private fun drawTitle(canvas: Canvas, @DrawableRes iconId: Int, @StringRes textId: Int) {
        val icon = if (iconId == R.drawable.add_music_icon) {
            musicIcon = null
            addIcon ?: context.getDrawable(iconId)
        } else {
            addIcon = null
            musicIcon ?: context.getDrawable(iconId)
        }
        if (icon != null) {
            val left = SizeUtil.dp2px(7F)
            val top = (height - icon.intrinsicHeight) / 2
            icon.setBounds(
                left,
                top,
                left + icon.intrinsicWidth,
                top + icon.intrinsicHeight
            )
            icon.draw(canvas)
        }
        val text = context.getString(textId)
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        canvas.drawText(
            text,
            SizeUtil.dp2px(12F).toFloat() + (icon?.intrinsicWidth ?: 0),
            getBaseLine(),
            textPaint
        )
    }

    private fun getBaseLine(): Float {
        return measuredHeight / 2 + (abs(textPaint.ascent()) - textPaint.descent()) / 2 +
                SizeUtil.dp2px(1F)
    }

    fun isEmpty() = waveCollect?.isEmpty() ?: true
}
