package com.ola.editor.kit.cutsame.cut.videoedit.customview

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.ola.editor.kit.R
import com.ola.editor.kit.cutsame.utils.SizeUtil
import kotlin.math.min

class CenterMaskView : View {

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        initAttrs(context!!, attrs!!)
        initPaint()
    }

    companion object {
        private const val DEFAULT_LINE_COLOR = 0x8000FFFF.toInt()
        private const val DEFAULT_LINE_WIDTH = 1f // dp 
        private const val DEFAULT_MASK_COLOR = 0x4500ff00 // dp 

        private const val DEFAULT_ANGLE_WIDTH = 10f
        private const val DEFAULT_ANGLE_LENGTH = 130f

        private const val DEFAULT_SUPPLEMENTARY_WIDTH = 1f //默认辅助线宽度
    }

    private var maskPathAll = Path()
    private var boxPath = Path()
    private var anglePath = Path()
    private var supplementaryPath = Path()
    private var screenWidth = 0
    private var screenHeight = 0
    private var boxWidth = 0
    private var boxHeight = 0

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boxLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val anglePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val supplementaryPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var mLineColor = DEFAULT_LINE_COLOR // 选区线的颜色 
    private var mLineWidth = DEFAULT_LINE_WIDTH // 选区线的宽度 
    private var mMaskColorDark = DEFAULT_MASK_COLOR // 遮罩颜色 深
    private val mMaskColorLight
        get() = mMaskColorDark and 0x00FFFFFF or (((mMaskColorDark ushr 24) * 0.6).toInt() shl 24) // 遮罩颜色 浅 即 alpha * 0.6
    private var inited = true

    private fun initAttrs(context: Context, attrs: AttributeSet) {
        context.obtainStyledAttributes(attrs, R.styleable.CenterMaskView).apply {
            mLineColor = getColor(R.styleable.CenterMaskView_civLineColor, DEFAULT_LINE_COLOR)
            mLineWidth = getDimension(
                R.styleable.CenterMaskView_civLineWidth, SizeUtil.px2dp(DEFAULT_LINE_WIDTH.toInt())
            )
            mMaskColorDark = getColor(R.styleable.CenterMaskView_civMaskColor, DEFAULT_MASK_COLOR)
            recycle()
        }
    }

    /**
     * 设置蒙版的size
     */
    private fun setMaskSize(width: Int, height: Int) {
        inited = false
        boxWidth = width
        boxHeight = height
        invalidate()
    }

    /**
     * 用于选择缩放的因子
     * 1. 选择相对于屏幕的缩放比例
     * 2. 添加padding，根据padding，调整新的缩放比例
     * 3. 计算出所位于的显示位置
     * 4. 将计算出的值设置到 boxWidth、boxHeight
     * 调用时机：在监听到界面初始化完成，view的宽高计算完成之后
     * @return Triple<计算出最终所选择的缩放比例、长、宽>
     */
    fun selectBoxScaleFactor(
        srcX: Int, srcY: Int,
        horizontalMargin: Int, verticalMargin: Int
    ): Triple<Float, Float, Float> {
        val sx = (width.toFloat() - SizeUtil.dp2px(horizontalMargin.toFloat()) * 2) / srcX.toFloat()
        val sy = (height.toFloat() - SizeUtil.dp2px(verticalMargin.toFloat()) * 2) / srcY.toFloat()
        val scaleFactor = min(sx, sy)
        val distW = srcX * scaleFactor
        val distH = srcY * scaleFactor
        setMaskSize(distW.toInt(), distH.toInt())
        return Triple(distH / srcY.toFloat(), distW, distH)
    }

    private fun initDrawAttrs(canvas: Canvas?) {
        if (inited.not()) {
            canvas?.apply {
                screenWidth = width
                screenHeight = height
            }

            val drawWeight = boxWidth + SizeUtil.dp2px(mLineWidth / 2)
            val drawHeight = boxHeight + SizeUtil.dp2px(mLineWidth / 2)

            maskPathAll.apply {
                reset()
                moveTo(0f, 0f)
                lineTo(0f, screenHeight.toFloat())
                lineTo(screenWidth.toFloat(), screenHeight.toFloat())
                lineTo(screenWidth.toFloat(), 0f)

                val wl = ((screenWidth - drawWeight) shr 1).toFloat()
                val wr = wl + drawWeight

                val ht = ((screenHeight - drawHeight) shr 1).toFloat()
                val hb = ht + drawHeight
                fillType = Path.FillType.EVEN_ODD
                moveTo(wl, ht)
                lineTo(wr, ht)
                lineTo(wr, hb)
                lineTo(wl, hb)
                close()
            }

            val wl = ((screenWidth - drawWeight) shr 1).toFloat() + 3
            val wr = wl + drawWeight - 6
            val ht = ((screenHeight - drawHeight) shr 1).toFloat() + 3
            val hb = ht + drawHeight - 6
            boxPath.apply {
                reset()
                fillType = Path.FillType.EVEN_ODD
                moveTo(wl, ht)
                lineTo(wr, ht)
                lineTo(wr, hb)
                lineTo(wl, hb)
                close()
            }

            val angleWl = wl - DEFAULT_ANGLE_WIDTH / 2
            val angleWr = wr + DEFAULT_ANGLE_WIDTH / 2
            val angleHt = ht - DEFAULT_ANGLE_WIDTH / 2
            val angleHb = hb + DEFAULT_ANGLE_WIDTH / 2
            anglePath.apply {
                reset()
                moveTo(angleWl, angleHt + DEFAULT_ANGLE_LENGTH)
                lineTo(angleWl, angleHt)
                lineTo(angleWl + DEFAULT_ANGLE_LENGTH, angleHt)

                moveTo(angleWr - DEFAULT_ANGLE_LENGTH, angleHt)
                lineTo(angleWr, angleHt)
                lineTo(angleWr, angleHt + DEFAULT_ANGLE_LENGTH)


                moveTo(angleWr, angleHb - DEFAULT_ANGLE_LENGTH)
                lineTo(angleWr, angleHb)
                lineTo(angleWr - DEFAULT_ANGLE_LENGTH, angleHb)


                moveTo(angleWl + DEFAULT_ANGLE_LENGTH, angleHb)
                lineTo(angleWl, angleHb)
                lineTo(angleWl, angleHb - DEFAULT_ANGLE_LENGTH)
            }


            supplementaryPath.apply {
                reset()
                val horizontalWidth = drawWeight / 3
                val verticalWidth = drawHeight / 3

                moveTo(wl + horizontalWidth, ht)
                lineTo(wl + horizontalWidth, hb)

                moveTo(wl + 2 * horizontalWidth, ht)
                lineTo(wl + 2 *horizontalWidth, hb)


                moveTo(wl , ht + verticalWidth)
                lineTo(wr , ht + verticalWidth)

                moveTo(wl , ht + 2*verticalWidth)
                lineTo(wr , ht +2*verticalWidth)

            }

            inited = true
        }
    }

    private fun initPaint() {
        maskPaint.apply {
            color = mMaskColorLight
            style = Paint.Style.FILL
        }

        boxLinePaint.apply {
            color = mLineColor
            strokeWidth = mLineWidth
            style = Paint.Style.STROKE
        }
        anglePaint.apply {
            color = mLineColor
            strokeWidth = DEFAULT_ANGLE_WIDTH
            style = Paint.Style.STROKE
        }
        supplementaryPaint.apply {
            color = mLineColor
            strokeWidth = DEFAULT_SUPPLEMENTARY_WIDTH
            style = Paint.Style.STROKE
        }
    }

    /**
     * immersive effect
     * mask color darker
     */
    fun startMaskDarker() {
        startMaskColorAnim(mMaskColorDark, Color.TRANSPARENT)
    }

    /**
     * immersive effect
     * mask color lighter
     */
    fun startMaskLighter() {
        startMaskColorAnim(mMaskColorLight, mLineColor)
    }

    private fun startMaskColorAnim(maskTargetColor: Int, lineTargetColor: Int) {
        val maskAnimator = ValueAnimator.ofObject(ArgbEvaluator(), maskPaint.color, maskTargetColor)
        maskAnimator.addUpdateListener {
            maskPaint.color = it.animatedValue as Int
        }

        val lineAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), boxLinePaint.color, lineTargetColor)
        lineAnimator.addUpdateListener {
            boxLinePaint.color = it.animatedValue as Int
            invalidate()
        }

        val animSet = AnimatorSet()
        animSet.duration = 300
        animSet.playTogether(maskAnimator, lineAnimator)
        animSet.start()
    }

    override fun onDraw(canvas: Canvas?) {
        initDrawAttrs(canvas)
        if (inited) {
            canvas?.apply {
                drawPath(maskPathAll, maskPaint)
                drawPath(boxPath, boxLinePaint)
                drawPath(anglePath, anglePaint)
                drawPath(supplementaryPath, supplementaryPaint)
            }
        }
    }
}
