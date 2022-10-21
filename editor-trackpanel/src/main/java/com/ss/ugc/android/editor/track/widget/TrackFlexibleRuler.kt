package com.ss.ugc.android.editor.track.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.ss.ugc.android.editor.track.utils.SizeUtil
import kotlin.math.roundToInt

//*
// * 时间刻度尺
//

class TrackFlexibleRuler @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val FPS_INTERVAL = 1.0f / 30
    }

    private val rateList: List<Float> = listOf(
            0.033334f, // 1f
            0.05f, // 1.5f
            0.083334f, // 2.5f
            0.166667f, // 5f
            0.25f, // 7.5f
            0.5f, // 15f
            1f, // 1s
            1.5f, 2.5f, 5f, 10f, 15f, 30f, 60f, 90f, 150f, 300f, 600f, 900f
    ) // 基于秒的倍率表

    private val pointSize = SizeUtil.dp2px(1f).toFloat()
    private val textColor = Color.WHITE
    private val textAlpha = 0x80 // 半透明
    private val textSize = SizeUtil.dp2px(9f).toFloat()
    private val minSpace = TrackConfig.THUMB_WIDTH / 2 // 两点距离最小宽度

    private val screenWidth = SizeUtil.getScreenWidth(context)

    private val pointPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var parentLayout: EditScroller? = null

    private var textBaseLine = 0f

    private var suitableRate: Float = rateList.first() // 适合的倍率
    private var suitablePointSpace: Float = 0f // 两点间距

    private var currentPosX = 0f // 当前绘制点的x坐标
    private var currentPointIdx = 0 // 当前绘制到第几个点

    // 当前view的可见范围
    private var visibleStartX = 0
    private var visibleEndX = 0

    private var viewCenterY = 0f

    private var durationWidth = 0 // 长度
        set(value) {
            if (field == value || value <= 0) {
                return
            }

            field = value
            resetSuitableParam()
        }

    var durationTime: Long = 0 // 时间 unit: ms
        set(value) {
            if (field == value || value <= 0) {
                return
            }

            field = value
            resetSuitableParam()
        }

    init {
        pointPaint.color = textColor
        pointPaint.textSize = textSize
        pointPaint.alpha = textAlpha
        textBaseLine = (pointPaint.ascent() + pointPaint.descent()) / 2
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        parentLayout = parent as EditScroller
        parentLayout?.setScrollChangeListener { _, _ ->
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        durationWidth = (durationTime * TrackConfig.PX_MS).toInt()
        val horizontalPadding = screenWidth/2
        setMeasuredDimension(durationWidth + horizontalPadding * 2, MeasureSpec.getSize(heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewCenterY = (h / 2).toFloat()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        if (durationWidth <= 0 || durationTime <= 0 || suitablePointSpace < minSpace) return

        val horizontalPadding = screenWidth/2

        visibleStartX = parentLayout?.scrollX ?: 0
        visibleEndX = (visibleStartX + screenWidth * 1.5).toInt()
        visibleEndX = if (visibleEndX < durationWidth + horizontalPadding) visibleEndX else durationWidth + horizontalPadding

        currentPointIdx = if (visibleStartX > horizontalPadding)
            ((visibleStartX - horizontalPadding) / suitablePointSpace).toInt() else 0 // 从第一个可见的点开始绘制
        currentPosX = horizontalPadding.toFloat() + currentPointIdx * suitablePointSpace

        while (currentPosX < visibleEndX) { // 可见范围内才绘制，避免过绘
            if (currentPointIdx.rem(2) == 0) {
                drawText(canvas, currentPosX, currentPointIdx)
            } else {
                canvas.drawCircle(currentPosX, viewCenterY, pointSize, pointPaint)
            }
            currentPosX += suitablePointSpace
            ++currentPointIdx
        }
    }

//*
//     * 每次到秒 则显示 XX:XX  分钟:秒数
//     * 中间如果还有点 则显示 2f 3f 5f 10f 15f


    private fun drawText(canvas: Canvas?, x: Float, pointIndex: Int) {
        val currentTime = pointIndex * suitableRate

        val second = currentTime.toInt()
        val frame = ((currentTime - second) / FPS_INTERVAL).roundToInt()

        // 帧为0，证明是整除，那么显示time
        if (frame == 0) {
            val timeStr = formatSecond(second)
            canvas?.drawText(timeStr, x - pointPaint.measureText(timeStr) / 2, viewCenterY - textBaseLine, pointPaint)
        } else {
            val frameStr = "${frame}f"
            canvas?.drawText(frameStr, x - pointPaint.measureText(frameStr) / 2, viewCenterY - textBaseLine, pointPaint)
        }
    }

//*
//     * 计算合适的倍率 及 计算两点间距


    private fun resetSuitableParam() {
        for (rate in rateList) {
            // 总时间 / 倍率 = 点的个数
            val pointCount = durationTime / rate / 1000

            // 总长度 / 点个数 = 点间距
            suitablePointSpace = durationWidth / pointCount

            if (suitablePointSpace > minSpace) { // 能达到两点间距的最小要求，则认为合适
                suitableRate = rate
                break
            }
        }

        requestLayout()
    }

    // String.format代码里有new Formatter，在onDraw方法里面不适合用，使用自己的简单实现。
    private fun formatSecond(second: Int): String {
        val min = second / 60
        val sec = second % 60
        val minStr = if (min < 10) {
            "0$min"
        } else {
            min.toString()
        }
        val secStr = if (sec < 10) {
            "0$sec"
        } else {
            sec.toString()
        }
        return "$minStr:$secStr"
    }
}
