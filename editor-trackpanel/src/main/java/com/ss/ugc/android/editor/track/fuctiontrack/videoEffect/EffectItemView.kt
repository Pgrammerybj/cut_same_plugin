package com.ss.ugc.android.editor.track.fuctiontrack.videoEffect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import androidx.core.content.ContextCompat
import android.text.TextPaint
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.bytedance.ies.nle.editor_jni.NLESegmentEffect
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.bytedance.ies.nle.editor_jni.NLETrackType
import com.ss.ugc.android.editor.base.theme.ThemeStore
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.track.fuctiontrack.BaseTrackItemHolder
import com.ss.ugc.android.editor.track.fuctiontrack.TagPainter
import com.ss.ugc.android.editor.track.fuctiontrack.audio.AudioItemHolder
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.BaseTrackKeyframeItemView
import com.ss.ugc.android.editor.track.fuctiontrack.video.DividerPainter
import com.ss.ugc.android.editor.track.utils.DataHolder
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.utils.getTrackByVideoEffect
import com.ss.ugc.android.editor.track.widget.TrackConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

private val TEXT_SIZE = SizeUtil.dp2px(12F).toFloat()
private val IMAGE_MARGIN = SizeUtil.dp2px(8F).toFloat()
private val TEXT_MARGIN = SizeUtil.dp2px(8F).toFloat()

class EffectItemViewHolder(context: Context) : BaseTrackItemHolder<EffectItemView>(context) {
    override val itemView: EffectItemView =
        EffectItemView(context)
}

class EffectItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseTrackKeyframeItemView(context, attrs, defStyleAttr), CoroutineScope {


    companion object {
        private val STICKER_BG_COLOR = Color.parseColor("#5E76EE")
        private val TEXT_BG_COLOR = Color.parseColor("#ECC66B")
        private val SUBTITLE_BG_COLOR = Color.parseColor("#AB6D36")
        private val LYRIC_BG_COLOR = Color.parseColor("#C4425B")
        private val VIDEO_EFFECT_COLOR = Color.parseColor("#EBAED8")
        private val BG_COLOR = Color.parseColor("#9f76c6")

        private val GLOBAL_VIDEO_EFFECT_COLOR:Int = Color.parseColor("#6b7cca")
    }


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
    override var bgColor: Int = Color.parseColor("#51c7b1")

    private val paint = Paint().apply { isAntiAlias = true }
    private val dividerPainter = DividerPainter(this)
    private val tagPainter = TagPainter(this)

    private val textPaint = TextPaint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textSize = TEXT_SIZE
        strokeWidth = SizeUtil.dp2px(1F).toFloat()
        style = Paint.Style.FILL
    }

    private var duration: Long = 0L
    private var enterDuration: Long = 0
    private var exitDuration: Long = 0
    private var loopDuration: Long = 0
    private var isLoopAnimation: Boolean = false

    private val path = Path()
    private val circleRadius = SizeUtil.dp2px(2F).toFloat()
    private val clipCanvasBounds = Rect()
    private var bitmap: Bitmap? = null
    private val textBounds = Rect()

    /**
     * 应用类型
     * 0：主视频（默认作用于主视频）， 1：画中画， 2：全局
     */
    private var applyType = -1

    override val coroutineContext: CoroutineContext = Dispatchers.IO
    private var downloading = false



    var text: String = ""
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    override fun setSegment(slot: NLETrackSlot) {

        nleTrackSlot = slot
        bgColor = BG_COLOR
        val mainSegment = slot.mainSegment

        NLESegmentEffect.dynamicCast(mainSegment)?.let {
            text = it.resource.resourceName
            NLESegmentEffect.dynamicCast(mainSegment)?.let {
                bgColor = if(ThemeStore.getCustomEffectSlotColor()!=null)
                    ContextCompat.getColor(context,ThemeStore.getCustomEffectSlotColor()!!)
                else
                    VIDEO_EFFECT_COLOR
            }
        }
        DataHolder.nleModel?.getTrackByVideoEffect(slot)?.apply {
            applyType = if (mainTrack) {
                MaterialEffect.APPLY_TYPE_MAIN
            } else if (trackType == NLETrackType.VIDEO) {
                MaterialEffect.APPLY_TYPE_SUB
            } else {
                bgColor = GLOBAL_VIDEO_EFFECT_COLOR

                MaterialEffect.APPLY_TYPE_ALL
            }
        }

    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        textPaint.textSize = SizeUtil.dp2px(10F).toFloat()
        val dp8 = SizeUtil.dp2px(8F)
        setPadding(dp8, 0, dp8, 0)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            if (!isItemSelected) drawOn(it)
            val slot = nleTrackSlot ?: return
        }
    }

    override fun drawOn(canvas: Canvas) {
        // 先绘制一下背景
        canvas.getClipBounds(clipCanvasBounds)
        paint.color = bgColor
        canvas.drawRect(clipCanvasBounds, paint)
        when (applyType) {
            MaterialEffect.APPLY_TYPE_ALL -> {
                tagPainter.draw(canvas, resources.getString(R.string.ck_track_panel_all_videos), bgColor)
            }
            MaterialEffect.APPLY_TYPE_SUB -> {
                tagPainter.draw(canvas, resources.getString(R.string.ck_track_panel_pip_2), bgColor)
            }
            MaterialEffect.APPLY_TYPE_MAIN -> {
                tagPainter.draw(canvas, resources.getString(R.string.ck_track_panel_main_track), bgColor)
            }
        }
        textPaint.textSize = AudioItemHolder.TEXT_SIZE
        textPaint.color = Color.WHITE
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        canvas.drawText(
            text,
            tagPainter.tagWidth + AudioItemHolder.TEXT_MARGIN + AudioItemHolder.TEXT_LEFT_MARGIN,
            getBaseLine(),
            textPaint
        )
        dividerPainter.draw(canvas, clipCanvasBounds)
        super.drawOn(canvas)
    }


    override fun onDetachedFromWindow() {
        if (bitmap != null) {
            bitmap = null
        }
        super.onDetachedFromWindow()
    }

    private fun getBaseLine(): Float {
        return measuredHeight / 2 + (abs(textPaint.ascent()) - textPaint.descent()) / 2 +
                SizeUtil.dp2px(1f)
    }

    private fun drawLine(canvas: Canvas, percent: Float, enterAnimation: Boolean) {
        val arrowWidth = SizeUtil.dp2px(5F)
        val lineWidth =
            kotlin.math.max(percent * (measuredWidth - (paddingStart + paddingEnd)), 0F)
        val startX =
            if (enterAnimation) paddingStart.toFloat() else measuredWidth - paddingStart.toFloat()
        var stopX = if (enterAnimation) startX + lineWidth else startX - lineWidth
        val y = measuredHeight.toFloat() - SizeUtil.dp2px(7F)
        val color = textPaint.color
        val style = textPaint.style
        textPaint.color = Color.WHITE
        val save = canvas.save()
        val dp3 = SizeUtil.dp2px(3F)
        if (enterAnimation) {
            path.reset()
            path.moveTo(stopX, y)
            stopX -= arrowWidth
            path.lineTo(stopX, y - dp3)
            path.lineTo(stopX, y + dp3)
            canvas.drawPath(path, textPaint)
        } else {
            path.reset()
            path.moveTo(stopX, y)
            stopX += arrowWidth
            path.lineTo(stopX, y - dp3)
            path.lineTo(stopX, y + dp3)
            canvas.drawPath(path, textPaint)
        }
        // UI需要加粗 简单画 2 遍
        canvas.drawLine(startX, y, stopX, y, textPaint)
        canvas.drawLine(startX, y + 1, stopX, y + 1, textPaint)
        canvas.restoreToCount(save)
        textPaint.color = color
        textPaint.style = style
    }

    private fun drawLoopLine(canvas: Canvas) {
        val padding = paddingStart + circleRadius * 2
        val y = measuredHeight.toFloat() - SizeUtil.dp2px(7F)
        val style = textPaint.style
        if (measuredWidth > 2 * padding) {
            canvas.drawLine(padding, y, measuredWidth - padding, y, textPaint)
            canvas.drawLine(padding, y + 1, measuredWidth - padding, y + 1, textPaint)
        }
        textPaint.style = Paint.Style.STROKE
        canvas.drawCircle(paddingStart + circleRadius, y, circleRadius, textPaint)
        canvas.drawCircle(paddingStart + circleRadius, y, circleRadius + 1, textPaint)
        canvas.drawCircle(measuredWidth - paddingEnd - circleRadius, y, circleRadius, textPaint)
        canvas.drawCircle(
            measuredWidth - paddingEnd - circleRadius,
            y,
            circleRadius + 1,
            textPaint
        )
        textPaint.style = style
    }
}
