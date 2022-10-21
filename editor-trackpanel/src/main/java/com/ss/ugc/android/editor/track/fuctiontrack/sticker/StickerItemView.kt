package com.ss.ugc.android.editor.track.fuctiontrack.sticker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import androidx.core.content.ContextCompat
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.bytedance.ies.nle.editor_jni.*
import com.ss.ugc.android.editor.base.EditorSDK
import com.ss.ugc.android.editor.base.imageloder.ImageOption
import com.ss.ugc.android.editor.base.theme.ThemeStore
import com.ss.ugc.android.editor.core.ext.fullContent
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.track.fuctiontrack.BaseTrackItemHolder
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.BaseTrackKeyframeItemView
import com.ss.ugc.android.editor.track.fuctiontrack.video.DividerPainter
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.widget.TrackConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private val TEXT_SIZE = SizeUtil.dp2px(12F).toFloat()
private val IMAGE_MARGIN = SizeUtil.dp2px(8F).toFloat()
private val TEXT_MARGIN = SizeUtil.dp2px(8F).toFloat()

class StickerItemHolder(context: Context) : BaseTrackItemHolder<StickerItemView>(context) {
    override val itemView: StickerItemView =
        StickerItemView(context)
}

class StickerItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseTrackKeyframeItemView(context, attrs, defStyleAttr), CoroutineScope {


    companion object {
        //信息贴纸轨道背景默认颜色
        private val INFO_STICKER_BG_COLOR = Color.parseColor("#5E76EE")
        //图片贴纸轨道背景默认颜色
        private val IMAGE_STICKER_BG_COLOR = Color.parseColor("#5E76EE")
        //文本轨道默认颜色
        private val TEXT_BG_COLOR = Color.parseColor("#ECC66B")
        //语音转字幕的文本轨道默认颜色
        private val SUBTITLE_BG_COLOR = Color.parseColor("#E5674E")
    }


    private var previewIcon: String? =  null
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

    override val coroutineContext: CoroutineContext = Dispatchers.IO
    private var downloading = false

    private var hasLine = false

    private var shouldDrawTextFlag = false
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    private var textFlagBitmap: Bitmap? = null

    var text: String = ""
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    override fun setSegment(slot: NLETrackSlot) {
        //todo need  judge  type then dispatch
        nleTrackSlot = slot
        val mainSegment = slot.mainSegment
        NLESegmentInfoSticker.dynamicCast(mainSegment)?.let {
            bgColor = if(ThemeStore.getCustomStickerSlotColor()!=null)
                ContextCompat.getColor( context,ThemeStore.getCustomStickerSlotColor()!!)
            else
                INFO_STICKER_BG_COLOR
            text = ""
        }
        NLESegmentImageSticker.dynamicCast(mainSegment)?.let {
            bgColor =
                IMAGE_STICKER_BG_COLOR
            text = ""
            previewIcon = it.imageFile?.resourceFile

        } ?: NLESegmentTextSticker.dynamicCast(mainSegment)?.let {
            text = it.content
            var textBgColor = TEXT_BG_COLOR
            if(ThemeStore.getCustomTextSlotColor()!=null){
                textBgColor = ContextCompat.getColor(context,ThemeStore.getCustomTextSlotColor()!!)
            }
            var subtitleBgColor = SUBTITLE_BG_COLOR
            if(ThemeStore.getCustomSubtitleSlotColor()!=null){
                subtitleBgColor = ContextCompat.getColor(context,ThemeStore.getCustomSubtitleSlotColor()!!)
            }
            bgColor = if (it.getExtra("type") == "subtitle") subtitleBgColor else textBgColor
        }

        NLESegmentTextTemplate.dynamicCast(mainSegment)?.let {
            text = it.fullContent()
            bgColor = TEXT_BG_COLOR
        }

        NLESegmentSticker.dynamicCast(mainSegment)?.animation?.let {
            if (it.loop) {
                setLoopAnimation(it.inDuration.toLong(), slot.duration)
            } else {
                setAnimationInfo(it.inDuration.toLong(), it.outDuration.toLong(), slot.duration)
            }
        }
    }

    private fun getRealAnimDuration(animResource: String, duration: Long): Long {
        return if (TextUtils.isEmpty(animResource)) {
            0L
        } else {
            duration
        }
    }

    private fun setAnimationInfo(inDuration: Long, outDuration: Long, duration: Long) {
        this.enterDuration = inDuration
        this.exitDuration = outDuration
        this.duration = duration
        this.isLoopAnimation = false
        invalidate()
    }

    private fun setLoopAnimation(loopDuration: Long, duration: Long) {
        this.loopDuration = loopDuration
        this.duration = duration
        isLoopAnimation = true
        invalidate()
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
        kotlin.run {
            val slot = nleTrackSlot ?: return@run

            var imagePath =  slot.getExtra("previewIconPath")
            if (imagePath.isEmpty() && previewIcon != null){
                imagePath = previewIcon
            }
            if (TextUtils.isEmpty(imagePath)) return@run
            if (bitmap == null || bitmap!!.isRecycled) {
                getDrawBitmap(imagePath)
            } else {
                val save = canvas.save()
                //fix：动画的线和预览图片重合
                val offset =
                    if (loopDuration > 0 || enterDuration > 0 || exitDuration > 0) SizeUtil.dp2px(3F) else 0
                canvas.drawBitmap(bitmap!!, IMAGE_MARGIN, IMAGE_MARGIN + offset, paint)
                canvas.restoreToCount(save)
            }
        }
        hasLine = false
        if (isClipping.not() && measuredWidth > paddingStart + paddingEnd) {
            if (isLoopAnimation) {
                if (loopDuration > 0) {
                    hasLine = true
                    drawLoopLine(canvas)
                }
            } else {
                if (enterDuration > 0) {
                    hasLine = true
                    drawLine(canvas, (enterDuration.toFloat() / duration), true)
                }
                if (exitDuration > 0) {
                    hasLine = true
                    drawLine(canvas, (exitDuration.toFloat() / duration), false)
                }
            }
        }

        textPaint.color = Color.WHITE
        val textMargin = TEXT_MARGIN + if (shouldDrawTextFlag) SizeUtil.dp2px(19f) else 0
        canvas.drawText(
            text,
            textMargin, getBaseLine(), textPaint
        )

        if (shouldDrawTextFlag) {
            if (textFlagBitmap == null) {
                textFlagBitmap = BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.sticker_item_text_flag
                )
            }
            textFlagBitmap?.also { bitmap ->
                canvas.drawBitmap(
                    bitmap,
                    SizeUtil.dp2px(4f).toFloat(),
                    SizeUtil.dp2px(4f).toFloat(),
                    paint
                )
            }
        }
        // 绘制分割线
        dividerPainter.draw(canvas, clipCanvasBounds)
        super.drawOn(canvas)
    }

    private fun getDrawBitmap(imagePath: String) {
        if (downloading || bitmap != null) return
        downloading = true
        launch {
            val size = measuredHeight / 2
            val tmpBitmap = loadBitmap(context, imagePath, size, size)
            withContext(Dispatchers.Main) {
                bitmap = tmpBitmap
                if (bitmap != null) {
                    //force redraw
                    requestLayout()
                    //fix：draw preview icon when first show sticker track if have stickers
                    invalidate()
                }
                downloading = false
            }
        }
    }

    private fun loadBitmap(context: Context, url: String, width: Int, height: Int): Bitmap? {
        return kotlin.runCatching {
            EditorSDK.instance.imageLoader()
                .loadBitmapSync(context, url, ImageOption.Builder().width(width).height(height).build())
        }.getOrDefault(null)
    }

    override fun onDetachedFromWindow() {
        if (bitmap != null) {
            bitmap = null
        }
        super.onDetachedFromWindow()
    }

    private fun getBaseLine(): Float {
        return if (hasLine) {
            21 / 34F * measuredHeight
        } else {
            15 / 34F * measuredHeight
        }
    }

    private fun getLineHeight(): Float {
        return SizeUtil.dp2px(7F).toFloat()
    }


    // 箭头划线
    private fun drawLine(canvas: Canvas, percent: Float, enterAnimation: Boolean) {
        val arrowWidth = SizeUtil.dp2px(4F)
        val lineWidth =
            kotlin.math.max(percent * (measuredWidth - (paddingStart + paddingEnd)), 0F)
        val startX =
            if (enterAnimation) paddingStart.toFloat() else measuredWidth - paddingStart.toFloat()
        var stopX = if (enterAnimation) startX + lineWidth else startX - lineWidth
        val y = getLineHeight()
        val color = textPaint.color
        val style = textPaint.style
        val paintWidth = textPaint.strokeWidth
        textPaint.color = Color.WHITE
        val linePaint = textPaint
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeJoin = Paint.Join.MITER
        val save = canvas.save()
        val dp3 = SizeUtil.dp2px(3F)
        // 先画直线
        path.reset()
        path.moveTo(startX, y)
        path.lineTo(stopX, y)
        // 再画箭头
        if (enterAnimation) {
            path.lineTo(stopX - arrowWidth, y - dp3)
        } else {
            path.lineTo(stopX + arrowWidth, y - dp3)
        }

        canvas.drawPath(path, linePaint)
        canvas.restoreToCount(save)
        textPaint.color = color
        textPaint.style = style
    }

    private fun drawLoopLine(canvas: Canvas) {
        val padding = paddingStart + circleRadius * 2
        val y = getLineHeight()
        val style = textPaint.style
        if (measuredWidth > 2 * padding) {
            canvas.drawLine(padding, y, measuredWidth - padding, y, textPaint)
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
