package com.angelstar.ola.view.audioclip

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import java.math.BigDecimal

/**
 * @email      pgrammer.ybj@outlook.com
 * @author     yangbaojiang
 * @date       2022/10/16
 * @des        视频裁剪
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

    private lateinit var seekBar: CropSeekBar
    lateinit var scrollTrackView: ScrollTrackView
    private var coverRectF = RectF()
    private var mVideoDuration = 0 * 1000f//音频总时长
    @kotlin.jvm.JvmField
    var mStartEditTimer = 0 * 1000f // 开始编辑的时间点
    @kotlin.jvm.JvmField
    var mEndEditTimer = 0 * 1000f //结束编辑的时间点


    var onSeekChange: (progress: Long) -> Unit = { }//当进度发生变化
    var onScrollSeekChange: (left: Float, right: Float) -> Unit = { _, _ -> }//当底部音符条滚动变化
    var onSectionChange: (left: Float, right: Float) -> Unit = { _, _ -> }
    var onTouchChange: (isTouch: Boolean) -> Unit = {}

    private fun initView(context: Context) {
        //区间选择
        seekBar = CropSeekBar(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        //音符条滑动块
        scrollTrackView = ScrollTrackView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        addView(scrollTrackView)
        addView(seekBar)
    }

    private fun initData() {
        //1.每个Track小块的数据,不设置也可以，有默认
        scrollTrackView.setDuration(mVideoDuration) // 音频时间
        scrollTrackView.setCutDuration((mEndEditTimer - mStartEditTimer).toInt()) //屏幕左边跑到右边持续的时间
        scrollTrackView.setTrackFragmentCount((mVideoDuration / scrollTrackView.trackCount).toInt()) //每一个Track有10条音符，每个音符设置1秒，那么总数就是音频总时长/10
        scrollTrackView.bindClipSeekBar(seekBar, mStartEditTimer, mEndEditTimer)
        scrollTrackView.setOnProgressRunListener(object : ScrollTrackView.OnProgressRunListener {
            override fun onTrackStart(ms: Float) {
                //滑块滑动了
            }

            override fun onTrackStartTimeChange(clipStartMs: Float, clipEndMs: Float) {
                mStartEditTimer = clipStartMs
                mEndEditTimer = clipEndMs
                onScrollSeekChange(clipStartMs, clipEndMs)
            }

            override fun onTrackEnd() {}
        })

        seekBar.setDuration(mVideoDuration)
        seekBar.onChangeProgress = { seekChange() }
        seekBar.onSectionChange = { left, right ->
            seekChange()
            onSectionChange(left, right)
            Log.i("Ola-Audio", "left:$left,right:$right")
            Log.i(
                "JackYang",
                "startTime:${scrollTrackView.startTime},endTime:${scrollTrackView.endTime}"
            )
            mStartEditTimer = scrollTrackView.startTime
            mEndEditTimer = scrollTrackView.endTime
            onScrollSeekChange(mStartEditTimer, mEndEditTimer)
        }
    }

    fun setClipEditTime(startEditTimer: Float, endEditTimer: Float, videoDuration: Float) {
        mStartEditTimer = startEditTimer
        mEndEditTimer = endEditTimer
        mVideoDuration = videoDuration
        //第一次进度回调，刷新上方截取的时间值
        onSeekChange(0)
        onScrollSeekChange(mStartEditTimer, mEndEditTimer)
        initData()
    }

    /**
     * 获取截取的时间值
     */
    fun getClipAudioTimerMS(): Float {
        Log.i("Ola-Audio", "mEndEditTimer:$mEndEditTimer,mStartEditTimer:$mStartEditTimer")
        return mEndEditTimer - mStartEditTimer
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        seekBar.layout(0, 0, width, height)
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
                        coverRectF.right = width + coverRectF.left
                    }
                    coverRectF.right + diffX <= seekBar.width - seekBar.slidePadding - seekBar.slideW -> {
                        coverRectF.right = seekBar.width - seekBar.slidePadding - seekBar.slideW
                        coverRectF.left = coverRectF.right - width
                    }
                    else -> {
                        coverRectF.left += diffX
                        coverRectF.right += diffX
                    }
                }
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
        val progress = (seekBar.midProgress - coverRectF.left) / scrollTrackView.width//进度百分比
        onSeekChange((progress * mVideoDuration).toLong())
    }

    /**
     * 设置当前播放进度
     *
     * @param duration 视频总时常
     * @param position 当前播放时常
     */
    fun setProgress(duration: Long, position: Float) {
        if (scrollTrackView.width == 0) {
            return
        }
        Log.e("ola-setProgress", "duration:$duration,position:$position")
        var newProgress = position / duration
        var leftRatio = (seekBar.seekLeft - coverRectF.left + seekBar.slideW / 2) / scrollTrackView.width
        var rightRatio = (seekBar.seekRight - coverRectF.left - seekBar.slideW / 2) / scrollTrackView.width

        newProgress = BigDecimal(newProgress.toString()).setScale(3, BigDecimal.ROUND_HALF_UP).toFloat()
        leftRatio = BigDecimal(leftRatio.toString()).setScale(3, BigDecimal.ROUND_HALF_UP).toFloat()
        rightRatio = BigDecimal(rightRatio.toString()).setScale(3, BigDecimal.ROUND_HALF_UP).toFloat()

        if (newProgress < leftRatio && leftRatio >= 0) {
            seekBar.midProgress = seekBar.getDefaultMid()
        } else if (newProgress >= rightRatio && rightRatio <= 1) {
            seekBar.midProgress = seekBar.getDefaultMid()
        } else {
            seekBar.midProgress = newProgress * scrollTrackView.width + coverRectF.left
        }
        seekBar.invalidate()
    }

}