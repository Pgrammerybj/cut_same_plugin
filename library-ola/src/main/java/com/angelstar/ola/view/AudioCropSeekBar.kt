package com.angelstar.ola.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.angelstar.ola.R
import java.math.BigDecimal

/**
 * @package    pgrammer.ybj@outlook.com
 * @author     yangbaojiang
 * @date       2022/10/16
 * @des        音频裁剪
 */
class AudioCropSeekBar : FrameLayout {
    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(context)
    }

    lateinit var coverView: LinearLayout
    lateinit var seekBar: CropSeekBar
    private var coverRectF = RectF()
    private var picW = 180f//每张封面宽度--这并不是最终值，会根据控件长度调整
    var originAudioDuration: Long = 10000L//默认视频时长
    var onSeekChange: (progress: Long) -> Unit = { }//当进度发生变化
    var onSectionChange: (left: Float, right: Float) -> Unit = { _, _ -> }
    var onTouchChange: (isTouch: Boolean) -> Unit = {}

    //准备完毕回调
    var onInitComplete: (originAudioDuration: Long) -> Unit = {}

    private fun initView(context: Context) {
        coverView = LinearLayout(context)
        seekBar = CropSeekBar(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        addView(coverView)
        addView(seekBar)
        seekBar.onChangeProgress = { seekChange() }
        seekBar.onSectionChange = { left, right ->
            seekChange()
            onSectionChange(left, right)
        }
        postDelayed({
            setAudioNoteBg()
        }, 200)
    }

    fun setAudioDuration(audioDuration: Float) {
        originAudioDuration = audioDuration.toLong()
    }

    //获取左侧滑块时间轴
    fun getLeftSlideSecond(): Long {
        return (((seekBar.seekLeft - coverRectF.left + seekBar.slideW / 2) / coverView.width) * originAudioDuration).toLong()
    }

    //获取右侧滑块时间轴
    fun getRightSlideSecond(): Long {
        return (((seekBar.seekRight - coverRectF.left - seekBar.slideW / 2) / coverView.width) * originAudioDuration).toLong()
    }

    private fun setAudioNoteBg() {
        onSectionChange(seekBar.seekLeft, seekBar.seekRight)
        //计算封面列表矩形大小和位置
        //如果视频长度小于最大区间，则封面列表宽度等于最大区间,这个时候时间轴会被拉伸
        val coverW: Float = seekBar.seekRight - seekBar.seekLeft - seekBar.slideW
        seekBar.maxInterval = originAudioDuration
        val coverMargin = seekBar.slidePadding + seekBar.slideW
        coverRectF.set(
            coverMargin,
            seekBar.slideOutH + seekBar.strokeW,
            coverW + coverMargin,
            height - seekBar.slideOutH - seekBar.strokeW
        )
        //todo 需要动态计算
        for (i in 1 until 8) {
            addCover()
        }
        post {
            onInitComplete.invoke(originAudioDuration)
        }
    }

    private fun addCover() {
        try {
            coverView.addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(10, 0, 0, 0)
                setImageResource(R.mipmap.record_edit_ware)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        seekBar.layout(0, 0, width, height)
        layoutCover()
    }

    private var lastX = 0f
    private var lastY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                onTouchChange(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                //限制边界
                val diffX = event.x - lastX
                when {
                    coverRectF.left + diffX > seekBar.slidePadding + seekBar.slideW -> {
                        coverRectF.left = seekBar.slidePadding + seekBar.slideW
                        coverRectF.right = coverView.width + coverRectF.left
                    }
                    coverRectF.right + diffX <= seekBar.width - seekBar.slidePadding - seekBar.slideW -> {
                        coverRectF.right = seekBar.width - seekBar.slidePadding - seekBar.slideW
                        coverRectF.left = coverRectF.right - coverView.width
                    }
                    else -> {
                        coverRectF.left += diffX
                        coverRectF.right += diffX
                    }
                }
                layoutCover()
                seekChange()
                lastX = event.x
                lastY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                onTouchChange(false)
            }
        }
        return super.onTouchEvent(event)
    }

    private fun seekChange() {
        val progress = (seekBar.midProgress - coverRectF.left) / coverView.width//进度百分比
        onSeekChange((progress * originAudioDuration).toLong())
    }

    private fun layoutCover() {
        coverView.layout(
            coverRectF.left.toInt(),
            coverRectF.top.toInt(),
            coverRectF.right.toInt(),
            coverRectF.bottom.toInt()
        )
    }

    /**
     * 设置当前播放进度
     *
     * @param duration 视频总时常
     * @param position 当前播放时常
     */
    fun setProgress(duration: Long, position: Float) {
        if (coverView.width == 0) {
            return
        }
        var newProgress = position / duration
        var leftRatio = (seekBar.seekLeft - coverRectF.left + seekBar.slideW / 2) / coverView.width
        var rightRatio =
            (seekBar.seekRight - coverRectF.left - seekBar.slideW / 2) / coverView.width
        newProgress =
            BigDecimal(newProgress.toString()).setScale(3, BigDecimal.ROUND_HALF_UP).toFloat()
        leftRatio = BigDecimal(leftRatio.toString()).setScale(3, BigDecimal.ROUND_HALF_UP).toFloat()
        rightRatio =
            BigDecimal(rightRatio.toString()).setScale(3, BigDecimal.ROUND_HALF_UP).toFloat()
        Log.e(
            "ola-setProgress",
            "newProgress:$newProgress,leftRatio:$leftRatio,rightRatio:$rightRatio"
        )
        if (newProgress < leftRatio && leftRatio >= 0) {
            seekBar.midProgress = seekBar.seekLeft
        } else if (newProgress >= rightRatio && rightRatio <= 1) {
            seekBar.midProgress = seekBar.seekLeft
        } else {
            seekBar.midProgress = newProgress * coverView.width + coverRectF.left
        }
        seekBar.invalidate()
    }
}