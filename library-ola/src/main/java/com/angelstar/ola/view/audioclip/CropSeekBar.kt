package com.angelstar.ola.view.audioclip

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.angelstar.ola.R

/**
 * @package    pgrammer.ybj@outlook.com
 * @author     yangbaojiang
 * @date       2022/10/16
 * @Describe        视频裁剪区域选择View
 */
class CropSeekBar : View {
    constructor(context: Context) : super(context) {
        initView(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(context, attrs)
    }

    private val color = Color.WHITE//边框颜色
    var slideW = 30f//两侧滑块宽度
    var strokeW = 8f//上下边框宽度
    var slideOutH = 0f//进度滑块越界高度
    private var midSlideW = 12f//中间滑块宽度
    private var radio = 30f//圆角角度
    val slidePadding = 0//两侧滑块外边距

    var midProgress = 0f//中间滑块的x坐标
    var seekLeft = 0f//左测滑块的x坐标
    var seekRight = 0f//右测滑块的x坐标
    var maxInterval = 30f * 1000//最大区间-时长ms
    var minInterval = 15f * 1000//最小区间-时长ms
    var minClipX = 0f
    var maxClipX = 0f

    var audioDuration: Float = 0f

    private val strokeLinePaint = Paint()
    private val slidePaint = Paint()
    private val path = Path()

    private val progressRectF = RectF()//中间滑块有效触摸范围
    private val leftSlideTouchRectF = RectF()//左滑块有效触摸范围
    private val rightSlideTouchRectF = RectF()//右滑块有效触摸范围
    private var isMoveSlide: Boolean = false
    var onChangeProgress: (progress: Float) -> Unit = { }
    var onSectionChange: (left: Float, right: Float) -> Unit = { _, _ -> }

    private fun initView(context: Context, attrs: AttributeSet?) {
        setWillNotDraw(false)
        attrs?.apply {
            val obtain = context.obtainStyledAttributes(attrs, R.styleable.CropSeekBar)
            slideOutH = obtain.getDimension(R.styleable.CropSeekBar_audio_slide_out_h, slideOutH)
            radio = obtain.getDimension(R.styleable.CropSeekBar_audio_radio, radio)
            obtain.recycle()
        }
        strokeLinePaint.isAntiAlias = true
        strokeLinePaint.strokeWidth = strokeW
        strokeLinePaint.color = color
        strokeLinePaint.strokeWidth = strokeW

        slidePaint.isAntiAlias = true
        slidePaint.color = Color.parseColor("#18D8B5")
        slidePaint.style = Paint.Style.FILL
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        seekLeft = slidePadding.toFloat() + slideW / 2
        seekRight = width - slidePadding.toFloat() - slideW / 2
        midProgress = getDefaultMid()
        super.onLayout(changed, left, top, right, bottom)
    }


    override fun onDraw(canvas: Canvas) {
        leftSlideTouchRectF.left = seekLeft - slideW / 2
        leftSlideTouchRectF.top = slideOutH
        leftSlideTouchRectF.right = seekLeft + slideW / 2
        leftSlideTouchRectF.bottom = height.toFloat() - slideOutH

        rightSlideTouchRectF.left = seekRight - slideW / 2
        rightSlideTouchRectF.top = slideOutH
        rightSlideTouchRectF.right = seekRight + slideW / 2
        rightSlideTouchRectF.bottom = height.toFloat() - slideOutH

        //绘制播放进度滑块
        progressRectF.left = midProgress - midSlideW / 2
        progressRectF.top = strokeW //进度条需要再边框内部
        progressRectF.right = midProgress + midSlideW / 2
        progressRectF.bottom = height.toFloat() - strokeW

        //绘制上边框
        canvas.drawLine(
            leftSlideTouchRectF.right,
            slideOutH + strokeW / 2,
            rightSlideTouchRectF.left,
            slideOutH + strokeW / 2,
            strokeLinePaint
        )
        //下边框
        canvas.drawLine(
            leftSlideTouchRectF.right,
            height.toFloat() - slideOutH - strokeW / 2,
            rightSlideTouchRectF.left,
            height.toFloat() - slideOutH - strokeW / 2,
            strokeLinePaint
        )
        //绘制两边滑块//////////////////////
        //左滑块
        path.reset()
        path.moveTo(leftSlideTouchRectF.left, radio + leftSlideTouchRectF.top)
        path.quadTo(
            leftSlideTouchRectF.left,
            leftSlideTouchRectF.top,
            radio + leftSlideTouchRectF.left,
            leftSlideTouchRectF.top
        )
        path.lineTo(leftSlideTouchRectF.right, leftSlideTouchRectF.top)
        path.lineTo(leftSlideTouchRectF.right, leftSlideTouchRectF.bottom)
        path.lineTo(radio + leftSlideTouchRectF.left, leftSlideTouchRectF.bottom)
        path.quadTo(
            leftSlideTouchRectF.left,
            leftSlideTouchRectF.bottom,
            leftSlideTouchRectF.left,
            leftSlideTouchRectF.bottom - radio
        )
        path.lineTo(leftSlideTouchRectF.left, radio + leftSlideTouchRectF.top)
        canvas.drawPath(path, strokeLinePaint)

        //右滑块
        path.reset()
        path.moveTo(rightSlideTouchRectF.left, rightSlideTouchRectF.top)
        path.lineTo(rightSlideTouchRectF.right - radio, rightSlideTouchRectF.top)
        path.quadTo(
            rightSlideTouchRectF.right,
            rightSlideTouchRectF.top,
            rightSlideTouchRectF.right,
            radio + rightSlideTouchRectF.top
        )
        path.lineTo(rightSlideTouchRectF.right, rightSlideTouchRectF.bottom - radio)
        path.quadTo(
            rightSlideTouchRectF.right,
            rightSlideTouchRectF.bottom,
            rightSlideTouchRectF.right - radio,
            rightSlideTouchRectF.bottom
        )
        path.lineTo(rightSlideTouchRectF.left, rightSlideTouchRectF.bottom)
        path.lineTo(rightSlideTouchRectF.left, rightSlideTouchRectF.top)
        canvas.drawPath(path, strokeLinePaint)
        //滑块绘制结束/////////////////////////////////

        //绘制中间进度指示条
        canvas.drawRect(progressRectF, slidePaint)
        super.onDraw(canvas)
    }

    private val SCROLL_MODE_NONE = 0
    private val SCROLL_MODE_LEFT = 1//左滑块
    private val SCROLL_MODE_RIGHT = 2//右滑块
    private val SCROLL_MODE_PROGRESS = 3//播放进度滑块
    private var scrollMode = SCROLL_MODE_NONE

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                when {
                    leftSlideTouchRectF.contains(event.x, event.y) -> {
                        //移动左滑块
                        scrollMode = SCROLL_MODE_LEFT
                        return true
                    }
                    rightSlideTouchRectF.contains(event.x, event.y) -> {
                        //移动右滑块
                        scrollMode = SCROLL_MODE_RIGHT
                        return true
                    }
                    event.x in progressRectF.left - 10..progressRectF.right + 10 -> {
                        //移动中间滑块
                        scrollMode = SCROLL_MODE_PROGRESS
                        return true
                    }
                }

            }
            MotionEvent.ACTION_MOVE -> {
                when (scrollMode) {
                    //允许的最小时长15秒最大时长30秒
                    SCROLL_MODE_LEFT -> {
                        //手指滑动后的区间
                        val sliderRange = seekRight - event.x - slideW
                        seekLeft = if (sliderRange > minClipX && sliderRange < maxClipX) {
                            //满足择取当前的event.x
                            event.x
                        } else {
                            //拖拽的距离超过区间需要重新计算
                            if (sliderRange < minClipX) {
                                //比最小还小
                                seekRight - minClipX - slideW
                            } else {
                                //比最大的还大
                                seekRight - maxClipX - slideW
                            }
                        }
                        midProgress = getDefaultMid()
                        isMoveSlide = true
                        onSectionChange(seekLeft, seekRight)
                        onChangeProgress(midProgress)
                        invalidate()
                        return true
                    }
                    SCROLL_MODE_RIGHT -> {
                        //手指滑动后的区间
                        val sliderRange = event.x - seekLeft - slideW
                        seekRight = if (event.x < width) {
                            if (sliderRange > minClipX && sliderRange < maxClipX) {
                                //满足区间内
                                event.x
                            } else {
                                //拖拽的距离超过区间需要重新计算
                                if (sliderRange < minClipX) {
                                    //比最小还小
                                    seekLeft + minClipX
                                } else {
                                    //比最大的还大
                                    seekLeft + maxClipX
                                }
                            }
                        } else {
                            width - slideW / 2
                        }
                        midProgress = seekRight - slideW / 2 - midSlideW / 2
                        isMoveSlide = true
                        onSectionChange(seekLeft, seekRight)
                        onChangeProgress(midProgress)
                        invalidate()
                        return true
                    }
                    SCROLL_MODE_PROGRESS -> {
                        if (event.x in seekLeft + slideW / 2..seekRight - slideW / 2) {//只允许在区间内滑动
                            midProgress = event.x
                        }
                        isMoveSlide = false
                        onChangeProgress(midProgress)
                        invalidate()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isMoveSlide = false
                if (scrollMode == SCROLL_MODE_RIGHT || scrollMode == SCROLL_MODE_LEFT) {
                    onChangeProgress(midProgress)
                }
                scrollMode = SCROLL_MODE_NONE
            }
        }
        return super.onTouchEvent(event)
    }

    fun setDefaultClipInterval(startEditX: Float, endEditX: Float, trackWidth: Float) {
        //初始化成功滑块的左右坐标
        seekLeft = startEditX
        seekRight = endEditX
        midProgress = getDefaultMid()
        minClipX = (minInterval / audioDuration) * trackWidth
        maxClipX = (maxInterval / audioDuration) * trackWidth
        this.invalidate()
    }

    fun setDuration(audioDuration: Float) {
        this.audioDuration = audioDuration
    }

    /**
     * 默认位置的中间滑块位置
     */
    fun getDefaultMid():Float{
        return seekLeft + slideW / 2 + midSlideW / 2
    }

}