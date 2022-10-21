package com.ss.ugc.android.editor.track.fuctiontrack.audio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.bytedance.ies.nle.editor_jni.NLEResType
import com.bytedance.ies.nle.editor_jni.NLESegmentAudio
import com.bytedance.ies.nle.editor_jni.NLESegmentFilter
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.bytedance.ies.nlemediajava.utils.MediaUtil
import com.ss.ugc.android.editor.base.logger.ILog
import com.ss.ugc.android.editor.base.theme.ThemeStore
import com.ss.ugc.android.editor.core.api.video.AudioFilterParam
import com.ss.ugc.android.editor.core.utils.DLog
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.fuctiontrack.BaseTrackItemHolder
import com.ss.ugc.android.editor.track.fuctiontrack.TrackGroup
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.BaseTrackKeyframeItemView
import com.ss.ugc.android.editor.track.fuctiontrack.video.DividerPainter
import com.ss.ugc.android.editor.track.fuctiontrack.viewmodel.FadeState
import com.ss.ugc.android.editor.track.utils.ColorUtil
import com.ss.ugc.android.editor.track.utils.PadUtil
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.utils.VEUtils
import com.ss.ugc.android.editor.track.widget.TrackConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class AudioItemHolder(
    activity: AppCompatActivity
) : BaseTrackItemHolder<AudioItemView>(activity), LifecycleOwner {

    override val itemView: AudioItemView = AudioItemView(activity)

    init {
        //判断是否有自定义音频轨的波浪线的颜色，有则使用，无则用默认的
        ThemeStore.getCustomAudioWaveColor()?.apply { AUDIO_WAVE_COLOR = ContextCompat.getColor(context, this) }
        //判断是否有自定义录音完成后的音频轨的波浪线的颜色，有则使用，无则用默认的
        ThemeStore.getCustomAfterRecordWaveColor()?.apply { RECORD_WAVE_COLOR = ContextCompat.getColor(context, this) }
        itemView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View?) {
//                lifecycleRegistry.currentState = Lifecycle.State.STARTED
                onStart()
            }

            override fun onViewDetachedFromWindow(v: View?) {
//                lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            }
        })
    }

    private fun onStart() {
    }

    private val lifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    companion object {
        /**
         * 在前后各绘制一条线（方块），避免两个靠的很近的音轨没有分割线，不好区分
         * 前面绘制一半后面绘制一半，加起来跟Play Head一样宽
         */
        val DIVIDER_WIDTH = SizeUtil.dp2px(1F)
        val TEXT_LEFT_MARGIN = SizeUtil.dp2px(4F).toFloat()
        val TEXT_BOTTOM_MARGIN = SizeUtil.dp2px(2F).toFloat()
        val WAVE_TEXT_SIZE = SizeUtil.dp2px(if (PadUtil.isPad) 10F else 8F).toFloat()
        val TEXT_MARGIN = SizeUtil.dp2px(8F).toFloat()
        val TEXT_SIZE = SizeUtil.dp2px(12F).toFloat()

        var RECORD_WAVE_COLOR = Color.parseColor("#11862F")
        var AUDIO_WAVE_COLOR = Color.parseColor("#ECC66B") // Yellow
        val SOUND_WAVE_COLOR = Color.parseColor("#ED8ACE") // Pink
    }

    enum class Type {
        MUSIC,
        RECORD,
        SOUND_EFFECT
    }
}

class AudioItemView @JvmOverloads constructor(
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
        set(value) {
            if (field == value) return
            field = value
        }
    override var clipLength: Float = 0F
    override var timelineScale: Float = TrackConfig.PX_MS

    @ColorInt
    override var bgColor: Int = BG_COLOR

    private val clipCanvasBounds = Rect()
    private val drawRect = RectF()
    private val paint = Paint()
    private val dividerPainter = DividerPainter(this)
    private val textPaint = TextPaint()
    private val textBounds = Rect()
    private val textBackgroundPaint = Paint()

    private val fadePath = Path()
    private val fadeArcPaint = Paint()
    private val fadeShadowPaint = Paint()

    private var speedIcon: Drawable? = null
    private var audioFilterIcon: Drawable? = null
    private var speedTextTop: Float = 0F
    private var audioFilterTextTop: Float = 0F
    private val speedTextPaint = Paint()
    private val audioFilterPaint = Paint()

    private var audioType: AudioItemHolder.Type = AudioItemHolder.Type.MUSIC
    private val beats = mutableSetOf<Long>()
    private var sourceTimeStart: Long = 0L
    private var speed: Float = 1F
    private var audioFilterName: String = ""

    private val painter = AudioSoulPainter()

    private var allWavePoints = mutableListOf<Pair<Long, Float>>()

    private var data: NLETrackSlot? = null

    var text: String = ""
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    @ColorInt
    var waveColor: Int = AudioItemHolder.AUDIO_WAVE_COLOR
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    private var fadeSubscribeJob: Job? = null
    var fadeState: FadeState? = null

    init {
        paint.isAntiAlias = true
        paint.textSize = AudioItemHolder.TEXT_SIZE

        textPaint.isAntiAlias = true
        textPaint.textSize = AudioItemHolder.TEXT_SIZE
        textPaint.strokeWidth = SizeUtil.dp2px(1F).toFloat()
        textPaint.style = Paint.Style.FILL

        textBackgroundPaint.isAntiAlias = true

        fadePath.fillType = Path.FillType.INVERSE_EVEN_ODD
        fadeShadowPaint.isAntiAlias = true

        fadeArcPaint.isAntiAlias = true
        fadeArcPaint.style = Paint.Style.STROKE
        fadeArcPaint.strokeWidth = SizeUtil.dp2px(0.5F).toFloat()

        speedTextPaint.apply {
            color = Color.WHITE
            isAntiAlias = true
            textSize = SizeUtil.dp2px(8f).toFloat()
            val fontRect = Rect()
            getTextBounds("0.0", 0, 3, fontRect)
            val fontHeight = fontRect.height()
            speedTextTop = ((SizeUtil.dp2px(16f) - fontHeight) / 2 + fontHeight).toFloat()
        }

        audioFilterPaint.apply {
            color = Color.WHITE
            isAntiAlias = true
            textSize = SizeUtil.dp2px(8f).toFloat()
            val fontRect = Rect()
            getTextBounds("0.0", 0, 3, fontRect)
            val fontHeight = fontRect.height()
            audioFilterTextTop = ((SizeUtil.dp2px(16f) - fontHeight) / 2 + fontHeight).toFloat()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setPadding(SizeUtil.dp2px(8F), SizeUtil.dp2px(6F), 0, SizeUtil.dp2px(6F))
    }

    override fun setSegment(slot: NLETrackSlot) {
        val nleSegmentAudio = NLESegmentAudio.dynamicCast(slot.mainSegment)
        if (slot.audioFilter != null) {
            slot.audioFilter.segment?.apply {
                val audioFilter = NLESegmentFilter.dynamicCast(this)
                audioFilterName = audioFilter.filterName
            }
        } else {
            audioFilterName = ""
        }
        nleSegmentAudio ?: return
        if (this.data?.id == slot.id) {
            sourceTimeStart = nleSegmentAudio.timeClipStart / 1000
            speed = nleSegmentAudio.absSpeed
            invalidate()
            return
        }
        this.data = slot
        super.nleTrackSlot = slot
        allWavePoints.clear()
        val start = System.currentTimeMillis()
        GlobalScope.launch(Dispatchers.Main) {
            // val waveArray = VEUtils.getWaveArray(
            //     slot.mainSegment.resource.resourceFile,
            //     (slot.mainSegment.resource.duration / 1000L / 30).toInt()
            // )
            // ILog.d("getWave", "timeCost ${System.currentTimeMillis() - start}")

            val waveArray = withContext(Dispatchers.IO) {
                val oriDuration = slot.mainSegment.resource.duration
                // 商城资源duration 单位毫秒，  剪辑SDK 单位微秒，  这里不统一不能直接使用oriDuration，需要本地重新提取来统一单位问题。
                val actualDuration = MediaUtil.getAudioFileInfo(slot.mainSegment.resource.resourceFile)?.duration ?: 0
                DLog.d("oriDuration:$oriDuration actualDuration:$actualDuration timeCost ${System.currentTimeMillis() - start}")
                val pointCount = (actualDuration / 30L).toInt()
                VEUtils.getWaveArray(
                    slot.mainSegment.resource.resourceFile,
                    pointCount
                )
            }
            ILog.d("getWave", " waveArray ${waveArray.size} timeCost ${System.currentTimeMillis() - start}")
            waveArray.forEachIndexed { index, fl ->
                allWavePoints.add(Pair((index * 30).toLong(), fl))
            }
            requestLayout()
            postInvalidate()
        }
        beats.clear()
        sourceTimeStart = nleSegmentAudio.timeClipStart / 1000
        speed = nleSegmentAudio.absSpeed
//        text = slot.audioInfo?.audioName ?: ""
        text = nleSegmentAudio.resource.resourceName
        waveColor = AudioItemHolder.AUDIO_WAVE_COLOR

        bgColor = MUSIC_TRACK_BG_COLOR
        audioType = when (slot.mainSegment.resource.resourceType) {
            NLEResType.RECORD -> {
                AudioItemHolder.Type.RECORD
            }
            NLEResType.SOUND -> {
                AudioItemHolder.Type.SOUND_EFFECT
            }
            else -> {
                AudioItemHolder.Type.MUSIC
            }
        }
        invalidate()
    }

    private fun disposeFadeSubscribe() {
        fadeSubscribeJob?.cancel()
        fadeSubscribeJob = null
    }

    override fun onDetachedFromWindow() {
        disposeFadeSubscribe()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            if (!isItemSelected) drawOn(it)
            val slot = nleTrackSlot ?: return
//            frameDrawer.draw(canvas = it, segment = slot)
        }
    }

    override fun drawOn(canvas: Canvas) {
        parent ?: return
        // 先绘制一下背景
        canvas.getClipBounds(clipCanvasBounds)
        paint.color = ColorUtil.adjustAlpha(bgColor, alpha)
        canvas.drawRect(clipCanvasBounds, paint)

        if (audioType == AudioItemHolder.Type.MUSIC || audioType == AudioItemHolder.Type.RECORD
            || audioType == AudioItemHolder.Type.SOUND_EFFECT
        ) {
            val right: Float
            val leftOffset: Float
            if (this.clipLeft != 0F || this.clipLength != 0F) {
                right = this.clipLength - this.clipLeft
                leftOffset = this.clipLeft
            } else {
                right = measuredWidth.toFloat()
                leftOffset = 0F
            }
            val offset = sourceTimeStart * timelineScale + leftOffset * speed
            painter.setStartOffset(offset, speed)

            // 计算所需绘制的边界
            // 比如一个9像素的View，只有中间1/3在屏幕上，那就只需要绘制像素区间[4,6]即可
            val scrollX = (parent as? TrackGroup)?.scrollX ?: 0
            val scrollRight = scrollX + TrackGroup.getPaddingHorizontal().toFloat() - leftOffset
            val leftWidthClip = left + leftOffset + translationX
            val paintLeft = if (scrollX > leftWidthClip) scrollX - leftWidthClip else 0F
            val paintRight = if (scrollRight < right) scrollRight else right

            drawRect.set(
                paintLeft,
                measuredHeight - BEAT_RADIUS * 3,
                paintRight,
                measuredHeight - BEAT_RADIUS
            )
            painter.drawBeats(
                canvas,
                drawRect,
                beats,
                timelineScale,
                ColorUtil.adjustAlpha(BEAT_COLOR, alpha)
            )

            drawRect.set(paintLeft, 0F, paintRight, measuredHeight.toFloat())
            val waveColor = when (audioType) {
                AudioItemHolder.Type.RECORD -> {
                    AudioItemHolder.RECORD_WAVE_COLOR
                }
                AudioItemHolder.Type.SOUND_EFFECT -> {
                    AudioItemHolder.SOUND_WAVE_COLOR
                }
                else -> {
                    AudioItemHolder.AUDIO_WAVE_COLOR
                }
            }
            painter.drawWave(
                canvas,
                drawRect,
                allWavePoints,
                timelineScale,
                ColorUtil.adjustAlpha(waveColor, alpha)
            )

            drawFade(canvas)

            if (text.isNotBlank()) {
                drawText(canvas, true)
            }

            if (audioFilterName.isNotEmpty()) {
                drawAudioFilter(canvas)
                hasAudioFilter = true
            } else {
                hasAudioFilter = false
            }
            if (speed != 1F && !hasAudioFilter) {
                drawSpeed(canvas)
                hasSpeed = true
            } else {
                speedIcon = null
                hasSpeed = false
            }
        } else if (audioType == AudioItemHolder.Type.SOUND_EFFECT) {
            drawFade(canvas)
            drawText(canvas, false)
            if (speed != 1F && !hasAudioFilter) {
                drawSpeed(canvas)
                hasSpeed = true
            } else {
                speedIcon = null
                hasSpeed = false
            }
        }

        dividerPainter.draw(canvas, clipCanvasBounds)
        super.drawOn(canvas)
    }

    private var topLabelMarginLeft: Float = 0.0f

    private fun drawText(canvas: Canvas, drawBackground: Boolean) {
        textBackgroundPaint.color = ColorUtil.adjustAlpha(TEXT_BG_COLOR, alpha)
        if (drawBackground) {
            canvas.drawRect(
                0F,
                (height - SizeUtil.dp2px(16F)).toFloat(),
                textBounds.width() + AudioItemHolder.TEXT_LEFT_MARGIN * 2,
                height.toFloat(),
                textBackgroundPaint
            )
        }

        textPaint.textSize = AudioItemHolder.WAVE_TEXT_SIZE
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        textPaint.color = ColorUtil.adjustAlpha(Color.WHITE, alpha)
        canvas.drawText(text, AudioItemHolder.TEXT_LEFT_MARGIN, getWaveBaseLine(), textPaint)
    }

    private var hasSpeed: Boolean = false
    private var hasAudioFilter: Boolean = false
    private val SPEED_LABEL_WIDTH = 39F
    private val AUDIO_FILTER_LABEL_WIDTH = 42F

    private fun drawAudioFilter(canvas: Canvas) {
        val icon = context.getDrawable(R.drawable.ic_cut_change_voice)
        if (icon != null) {
            canvas.drawRect(
                0F,
                0F,
                SizeUtil.dp2px(AUDIO_FILTER_LABEL_WIDTH).toFloat(),
                SizeUtil.dp2px(16F).toFloat(),
                textBackgroundPaint
            )

            val marginLeft = SizeUtil.dp2px(3F).toFloat()
            val marginTop = SizeUtil.dp2px(2F).toFloat()
            canvas.save()
            canvas.translate(marginLeft, marginTop)

            icon.setBounds(0, marginTop.toInt(), icon.intrinsicWidth * 2 / 3, icon.intrinsicHeight * 2 / 3)
            icon.draw(canvas)
            canvas.restore()
            canvas.drawText(
                audioFilterName,
                (marginLeft * 2 + icon.intrinsicWidth * 2 / 3),
                audioFilterTextTop,
                audioFilterPaint
            )
        }
    }

    private fun drawSpeed(canvas: Canvas) {
        val icon = speedIcon ?: context.getDrawable(R.drawable.ic_speed_n)
        if (icon != null) {
            canvas.drawRect(
                0F,
                0F,
                SizeUtil.dp2px(SPEED_LABEL_WIDTH).toFloat(),
                SizeUtil.dp2px(16F).toFloat(),
                textBackgroundPaint
            )

            val marginLeft = SizeUtil.dp2px(3F).toFloat()
            val marginTop = SizeUtil.dp2px(2F).toFloat()
            canvas.save()
            canvas.translate(marginLeft, marginTop)
            icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
            icon.draw(canvas)
            canvas.restore()
            val speedText = String.format("%3.1fx", speed)
            canvas.drawText(
                speedText,
                (marginLeft * 2 + icon.intrinsicWidth),
                speedTextTop,
                speedTextPaint
            )
        }
    }

    private fun drawFade(canvas: Canvas) {
        val segment = data ?: return

        val canvasWidth: Float
        val fadeIn: Long
        val fadeOut: Long
        if (isClipping) {
            val audioInfo = NLESegmentAudio.dynamicCast(segment.mainSegment) ?: return
            canvasWidth = if (this.clipLength != 0F) {
                this.clipLength
            } else {
                (segment.duration / 1000 * timelineScale)
            }
            fadeIn = audioInfo.fadeInLength
            fadeOut = audioInfo.fadeOutLength
        } else {
            val fadeState = fadeState
            if (fadeState != null && fadeState.drawFade) {
                canvasWidth = segment.mainSegment.duration / 1000 * timelineScale
                fadeIn = fadeState.fadeInDuration / 1000
                fadeOut = fadeState.fadeOutDuration / 1000
            } else {
                val audioInfo = NLESegmentAudio.dynamicCast(segment.mainSegment) ?: return
                canvasWidth = segment.duration / 1000 * timelineScale
                fadeIn = audioInfo.fadeInLength / 1000
                fadeOut = audioInfo.fadeOutLength / 1000
            }
        }

        val fadeInRight = fadeIn * timelineScale
        fadeShadowPaint.color = ColorUtil.adjustAlpha(FADE_SHADOW_COLOR, alpha)
        if (fadeInRight > 0) {
            canvas.save()
            canvas.clipRect(0F, 0F, fadeInRight, measuredHeight.toFloat())
            fadePath.reset()
            fadePath.addOval(
                0F,
                0F,
                fadeInRight * 2,
                measuredHeight.toFloat(),
                Path.Direction.CW
            )
            canvas.drawPath(fadePath, fadeShadowPaint)
            canvas.restore()
        }

        val fadeOutLeft = fadeOut * timelineScale
        val fadeOutLeftBound = canvasWidth - fadeOutLeft
        if (fadeOutLeft > 0) {
            canvas.save()
            canvas.clipRect(fadeOutLeftBound, 0F, canvasWidth, measuredHeight.toFloat())
            fadePath.reset()
            fadePath.addOval(
                fadeOutLeftBound - fadeOutLeft,
                0F,
                canvasWidth,
                measuredHeight.toFloat(),
                Path.Direction.CW
            )
            canvas.drawPath(fadePath, fadeShadowPaint)
            canvas.restore()
        }

        fadeArcPaint.color = ColorUtil.adjustAlpha(FADE_ARC_COLOR, alpha)
        if (fadeInRight > 0) {
            canvas.save()
            canvas.clipRect(0F, 0F, fadeInRight, measuredHeight.toFloat())
            canvas.drawOval(
                0F,
                0F,
                fadeInRight * 2,
                measuredHeight.toFloat(),
                fadeArcPaint
            )
            canvas.restore()
        }

        if (fadeOutLeft > 0) {
            canvas.save()
            canvas.clipRect(fadeOutLeftBound, 0F, canvasWidth, measuredHeight.toFloat())
            canvas.drawOval(
                fadeOutLeftBound - fadeOutLeft,
                0F,
                canvasWidth,
                measuredHeight.toFloat(),
                fadeArcPaint
            )
            canvas.restore()
        }
    }

    private fun getWaveBaseLine(): Float {
        return clipCanvasBounds.height() - (abs(textPaint.ascent()) - textPaint.descent()) / 2 - AudioItemHolder.TEXT_BOTTOM_MARGIN
    }

    private fun getBaseLine(): Float {
        return measuredHeight / 2 + (abs(textPaint.ascent()) - textPaint.descent()) / 2 +
            SizeUtil.dp2px(1f)
    }

    /**
     * 拖拽移动的过程中刷新
     */
    override fun setTranslationX(translationX: Float) {
        super.setTranslationX(translationX)
        invalidate()
    }

    override fun onSetAlpha(alpha: Int): Boolean = true

    companion object {
        private val BEAT_RADIUS = SizeUtil.dp2px(1.5F).toFloat()
        private val BG_COLOR = Color.parseColor("#51c7b1")
        private val TEXT_BG_COLOR = Color.parseColor("#66101010")
        private val FADE_ARC_COLOR = Color.parseColor("#7F3D7A7F")

        @Suppress("DEPRECATION")
        private val FADE_SHADOW_COLOR =
            ContextCompat.getColor(TrackSdk.application, R.color.transparent_45p)
    }
}
