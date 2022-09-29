package com.ss.ugc.android.editor.main

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.*
import android.view.View
import androidx.annotation.ColorInt
import com.ss.ugc.android.editor.base.utils.SizeUtil
import kotlin.math.max
import kotlin.math.min

/**
 * 因为SliderView的position是整数，所以作为进度条的时候不流畅
 * 为此，复刻一个position为Float类型的SliderView
 */
@SuppressLint("CustomViewStyleable")
class FloatSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val shadowColor = 0x40000000

    @ColorInt
    var lineColor = shadowColor
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    @ColorInt
    private var handleColor = Color.TRANSPARENT

    @ColorInt
    private var lineHintColor = Color.TRANSPARENT

    @ColorInt
    private var lineChooseColor = Color.TRANSPARENT

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG)


    private var lineStart: Float = 0F
    private var lineEnd: Float = 0F
    private var lineCenterY: Float = 0F
    private var precision: Float = 1F
    private var decorateSize: Float = 0F
    private var lineLen: Float = 0F
    private var valueDiff: Float = 0F
    private var roundRectRadius: Float = 8F

    var currPosition: Float = 0F
        set(value) {
            if (field != value) {
                field = max(min(value, maxValue), minValue)
                invalidate()
            }
        }

    private var lineWidth: Int = 6
    private var handleRadius: Float = 0F

    private var isSliding = false
    private var drawColorProgress = true

    private var fillWidth = false
    private var floatingBottom = false
    private var hasMeasure = false

    private var listener: OnFloatSliderChangeListener? = null

    var maxValue = 100.0F
        private set(value) {
            field = value
        }

    var minValue = 0F
        private set(value) {
            field = value
        }

    companion object {
        val textHeight by lazy {
            // 这里我们不需要绘制text, 只需要求取和原 SliderView 一样的 textHeight，
            // 所以textPaint，textBounds等相关的变量都不需要声明为类的成员，作为临时变量用于计算即可
            val textSize = SizeUtil.dp2px(12F).toFloat()
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            textPaint.textSize = textSize
            val textBounds = Rect()
            textPaint.getTextBounds("0", 0, 1, textBounds)
            textBounds.height()
        }
        private val floatingMarginBottom = SizeUtil.dp2px(20F)
    }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.SliderView).apply {
            //当前进度进度条颜色（美团黄）
            lineColor = getColor(
                R.styleable.SliderView_lineColor,
                resources.getColor(R.color.style_slider_line)
            )
            lineHintColor = getColor(
                R.styleable.SliderView_lineHintColor,
                resources.getColor(R.color.light_grey)
            )
            lineChooseColor = getColor(
                R.styleable.SliderView_lineChooseColor,
                resources.getColor(R.color.line_choose_color)
            )
            //中心小圆球的颜色
            handleColor = getColor(R.styleable.SliderView_handleColor, Color.WHITE)
            //小圆球半径
            handleRadius = getDimensionPixelSize(
                R.styleable.SliderView_handle_radius,
                SizeUtil.dp2px(5F)
            ).toFloat()
            //总进度条的高度
            lineWidth = getDimensionPixelSize(R.styleable.SliderView_lineWidth, SizeUtil.dp2px(6F))
            fillWidth = getBoolean(R.styleable.SliderView_fillWidth, false)
            floatingBottom = getBoolean(R.styleable.SliderView_floatingBottom, false)
            recycle()
        }

        linePaint.style = Paint.Style.FILL_AND_STROKE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.UNSPECIFIED -> SizeUtil.getScreenWidth(context)
            else -> 0
        }

        val maxHeight = MeasureSpec.getSize(heightMeasureSpec)
        // 对标SliderView，计算desireHeight的时候，除了handleRadius，还有 textHeight 和 floatingMarginBottom
        // 而用在预览（PreviewLayout）时不需要floatingMargin
        // 故而用floatingBottom来区分样式
        val floatingMargin = if (floatingBottom) textHeight + floatingMarginBottom else 0
        val desireHeight: Int =
            ((handleRadius + floatingMargin) * 2).toInt() + paddingTop + paddingBottom
        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> maxHeight
            MeasureSpec.AT_MOST -> min(desireHeight, maxHeight)
            MeasureSpec.UNSPECIFIED -> desireHeight
            else -> desireHeight
        }
        setMeasuredDimension(width, height)
        //应该是左右间距
        decorateSize = if (fillWidth) 0F else max(handleRadius, SizeUtil.dp2px(20F).toFloat())
    }

    private fun setDimenParams() {
        hasMeasure = true
        lineStart = paddingLeft + decorateSize
        lineEnd = measuredWidth - paddingRight - decorateSize
        lineCenterY = measuredHeight / 2F
        lineLen = lineEnd - lineStart
        valueDiff = maxValue - minValue
        precision = lineLen / valueDiff
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (hasMeasure) {
            setDimenParams()
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        if (!hasMeasure) {
            setDimenParams()
        }
        val t = (currPosition - minValue) / valueDiff
        val handleCenterX = lineStart + t * lineLen
        //矩形进度条和小圈球需要对齐
        val moveY = (lineCenterY - lineWidth / 2)


        // 画圆角矩形总进度
        linePaint.color = lineHintColor;
        val oval = RectF(lineStart, moveY, lineEnd, lineWidth.toFloat() + moveY)
        canvas.drawRoundRect(oval, roundRectRadius, roundRectRadius, linePaint)

        // 歌曲选中区域（94d6b6）
        linePaint.color = lineChooseColor
        val oval2 = RectF((lineEnd * 0.46).toFloat(), moveY, (lineEnd * 0.76).toFloat(), lineWidth.toFloat() + moveY)
        canvas.drawRoundRect(oval2, roundRectRadius, roundRectRadius, linePaint)

        val progressStart = when {
            0 < minValue -> {
                lineStart
            }
            0 > maxValue -> {
                lineEnd
            }
            else -> {
                (0 - minValue) * precision + lineStart
            }
        }
        // 画当前进度
        if (drawColorProgress) {
            linePaint.color = lineColor
            val oval3 = RectF(progressStart, moveY, handleCenterX, lineWidth.toFloat() + moveY)
            canvas.drawRoundRect(oval3, roundRectRadius, roundRectRadius, linePaint)
        }

        //圆球进度
        handlePaint.color = Color.WHITE
        handlePaint.style = Paint.Style.FILL
        canvas.drawCircle(handleCenterX, lineCenterY, handleRadius, handlePaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        if (event.action == ACTION_DOWN && listener?.onPreChange() == false) {
            return false
        }

        when (event.action) {
            ACTION_DOWN -> {
                val position = calcCurrPosition(event)
                listener?.onBegin(position)
                onChange(position)
                isSliding = true
                parent.requestDisallowInterceptTouchEvent(true)
            }

            ACTION_MOVE -> {
                if (isSliding) {
                    onChange(calcCurrPosition(event))
                }
            }

            ACTION_UP, ACTION_CANCEL -> {
                if (isSliding) {
                    isSliding = false
                    if (fillWidth) {
                        // 在SlideView铺满时，无法拖动滑块到边缘
                        // 这里设置一个阈值，超过则吸附到边缘
                        val t = (currPosition - minValue) / (maxValue - minValue)
                        if (t < 0.05F) {
                            currPosition = minValue
                        } else if (t > 0.95F) {
                            currPosition = maxValue
                        }
                    }
                    listener?.onFreeze(currPosition)
                }
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }

        invalidate()
        return true
    }

    private fun onChange(position: Float) {
        if (currPosition != position) {
            currPosition = position
            listener?.onChange(currPosition)
        }
    }

    private fun calcCurrPosition(event: MotionEvent): Float {
        val x = event.x
        return when {
            x in lineStart..lineEnd -> {
                ((x - lineStart) / precision) + minValue
            }
            x < lineStart -> minValue
            else -> maxValue
        }
    }

    fun setOnSliderChangeListener(listener: OnFloatSliderChangeListener) {
        this.listener = listener
    }

    fun isSliding() = isSliding
}

abstract class OnFloatSliderChangeListener {
    /**
     * 时间触发前是否响应
     * true - 响应事件
     * false - 忽略触摸事件
     */
    open fun onPreChange(): Boolean = true

    /**
     * 收到ACTION_DOWN事件时的滑杆值
     *
     * @param value 收到ACTION_DOWN事件时的滑杆值
     */
    open fun onBegin(value: Float) {}

    /**
     * ACTION_MOVE时调用
     *
     * @param value 收到ACTION_MOVE事件时的滑杆值
     */
    abstract fun onChange(value: Float)

    /**
     * 抬手或ACTION_CANCEL时调用
     *
     * @param value 滑杆值，这个值跟最近一次onChange回调出去的值应该是一样的
     */
    abstract fun onFreeze(value: Float)
}
