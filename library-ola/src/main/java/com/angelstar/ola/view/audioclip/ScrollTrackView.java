package com.angelstar.ola.view.audioclip;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import org.jetbrains.annotations.NotNull;

/**
 * Describe:滑动音轨View
 */

public class ScrollTrackView extends HorizontalScrollView {

    private Handler mScrollHandler;
    private OnScrollTrackListener mOnScrollTrackListener;
    private OnProgressRunListener mProgressRunListener;

    private final boolean isAutoRun = false;//是否自动跑进度
    private boolean isLoopRun = false;//是否循环跑进度
    private float mCutDuration = 20 * 1000f;//裁剪区间，也就是控件左边，跑到右边的时间
    private float mSpeed = 10;
    private CropSeekBar mCropSeekBar;

    /**
     * 滚动状态:
     * IDLE=滚动停止
     * TOUCH_SCROLL=手指拖动滚动
     * FLING=滚动
     */
    enum ScrollStatus {IDLE, TOUCH_SCROLL, FLING}

    /**
     * 记录当前滚动的距离
     */
    private int currentX = -9999999;

    /**
     * 当前滚动状态
     */
    private ScrollStatus scrollStatus = ScrollStatus.IDLE;

    private Track track;
    private boolean disableTouch;
    private TrackMoveController moveController;

    private float audioDuration;

    public ScrollTrackView(Context context) {
        super(context);
        initView(context);
    }

    public ScrollTrackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ScrollTrackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public void bindClipSeekBar(@NotNull CropSeekBar cropSeekBar, final float startEditTimer, final float endEditTimer) {
        this.mCropSeekBar = cropSeekBar;
        post(() -> mCropSeekBar.setDefaultClipInterval(timeConvertX(startEditTimer), timeConvertX(endEditTimer),track.getWidth()));
    }

    private void initView(final Context context) {
        setHorizontalScrollBarEnabled(false);
        track = new Track(context);
        track.setBackgroundColorInt(Color.parseColor("#858585"));//背景色：灰色
        track.setForegroundColor(Color.parseColor("#FF4081"));//进度色：橙红
        //音符线的间距
        track.setSpaceSize(6);
        //音轨的个数
        track.setTrackFragmentCount(10);
        //音符线的宽度
        int mTrackItemWidth = 10;
        track.setTrackItemWidth(mTrackItemWidth);

        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(track, lp);
        setSmoothScrollingEnabled(false);

        //ms
        long mDelayTime = 20;
        moveController = new TrackMoveController(mDelayTime, new TrackMoveController.OnProgressChangeListener() {
            @Override
            public void onProgressChange(float progress) {
                Message msg = progressHandler.obtainMessage(1);
                msg.arg1 = (int) progress;
                progressHandler.sendMessage(msg);
            }

            @Override
            public void onProgressStart() {
                if (mProgressRunListener != null) {
                    mProgressRunListener.onTrackStart(getStartTime());
                }
            }

            @Override
            public void onProgressEnd() {
                if (mProgressRunListener != null) {
                    mProgressRunListener.onTrackEnd();
                }
            }
        });

        post(() -> {
            //可视的时候开始走进度
            moveController.setScrollTrackViewWidth(getWidth());
            mSpeed = ((getWidth() * 1f) / (mCutDuration * 1f));//根据时间和控件的宽度计算速度
            float delayTime = 1f / mSpeed;//根据速度来算走每个像素点需要多久时间
            moveController.setDelayTime(Math.round(delayTime));//四舍五入
            moveController.setLoopRun(isLoopRun);
            if (isAutoRun) {
                startMove();
            }

        });

        mScrollHandler = new Handler();
        //滑动状态监听
        mOnScrollTrackListener = scrollStatus -> {
            switch (scrollStatus) {
                case IDLE:
                    if (moveController != null) {
                        moveController.setScrollTrackStartX(getScrollX());
                        moveController.continueRun();
                    }
                    if (mProgressRunListener != null) {
                        mProgressRunListener.onTrackStartTimeChange(getStartTime(),getEndTime());
                    }
                    break;
                case TOUCH_SCROLL:
                    if (moveController != null) {
                        moveController.pause();
                    }
                    break;
                default:
                    break;
            }
        };
    }

    public void setCutDuration(int cutDuration) {
        mCutDuration = cutDuration;
    }

    public int getTrackCount() {
        //配置的10秒一个track,外面但是毫秒，所以提前计算为毫秒
        return track.getTrackTemplateCount() * 1000;
    }

    /**
     * 设置循环播放
     */
    public void setLoopRun(boolean isLoop) {
        isLoopRun = isLoop;
    }

    public void setTrackFragmentCount(int count) {
        if (track != null) {
            track.setTrackFragmentCount(count);
        }
    }

    //-------------scroll control-----------------
    private interface OnScrollTrackListener {
        void onScrollChanged(ScrollStatus scrollStatus);
    }


    /**
     * 滚动监听runnable 方便获取滑动状态
     */
    private final Runnable scrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (getScrollX() == currentX) {
                //滚动停止,取消监听线程
                scrollStatus = ScrollStatus.IDLE;
                if (mOnScrollTrackListener != null) {
                    mOnScrollTrackListener.onScrollChanged(scrollStatus);
                }
                mScrollHandler.removeCallbacks(this);
                return;
            } else {

                //手指离开屏幕,但是view还在滚动
                scrollStatus = ScrollStatus.FLING;
                if (mOnScrollTrackListener != null) {
                    mOnScrollTrackListener.onScrollChanged(scrollStatus);
                }
            }
            currentX = getScrollX();
            //滚动监听间隔:milliseconds
            mScrollHandler.postDelayed(this, 20);
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                this.scrollStatus = ScrollStatus.TOUCH_SCROLL;
                mOnScrollTrackListener.onScrollChanged(scrollStatus);
                mScrollHandler.removeCallbacks(scrollRunnable);
                break;
            case MotionEvent.ACTION_UP:
                mScrollHandler.post(scrollRunnable);
                break;
        }
        return super.onTouchEvent(ev);
    }


    /**
     * 进度控制
     */
    @SuppressLint("HandlerLeak")
    Handler progressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                track.setProgress(msg.arg1);
            }
        }
    };

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (disableTouch) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * 开始
     */
    public void startMove() {
        disableTouch = true;
        if (moveController != null) {
            moveController.start();
        }
    }

    /**
     * 重新开始播放
     */
    public void restartMove() {
        disableTouch = true;
        if (moveController != null) {
            scrollTo(0, 0);
            smoothScrollTo(0, 0);
            moveController.restart();
            if (mProgressRunListener != null) {
                mProgressRunListener.onTrackStartTimeChange(0,0);
            }
        }
    }

    /**
     * 停止
     */
    public void stopMove() {
        disableTouch = false;
        if (moveController != null) {
            moveController.stop();
        }
    }

    public void pauseMove() {
        disableTouch = false;
        if (moveController != null) {
            moveController.pause();
        }
    }

    /**
     * 轨道开始播放到轨道结束监听
     */
    public interface OnProgressRunListener {
        void onTrackStart(float ms);

        void onTrackStartTimeChange(float clipStartMs, float clipEndMs);

        void onTrackEnd();
    }

    public void setOnProgressRunListener(OnProgressRunListener listener) {
        mProgressRunListener = listener;
    }

    /**
     * 设置音频总时间
     */
    public void setDuration(float ms) {
        audioDuration = ms;
    }

    /**
     * 获取歌曲开始时间 (毫秒)
     */
    public float getStartTime() {
        //滑块距左边的距离+音符滑动条滑动的距离
        float realOffsetX = getScrollX() + mCropSeekBar.getSeekLeft();
        float rate = Math.abs(realOffsetX) / (track.getWidth() * 1f);
        return audioDuration * rate;
    }

    public float getEndTime(){
        //滑块距右边的距离+音符滑动条滑动的距离
        float realOffsetX = getScrollX() + mCropSeekBar.getSeekRight();
        float rate = Math.abs(realOffsetX) / (track.getWidth() * 1f);
        return audioDuration * rate;
    }

    /**
     * 将裁剪的起始时间转换为X轴上的像素值
     *
     * @param clipTime 需要转换的时间ms
     * @return X轴的像素值，从屏幕左边缘开始计算
     */
    private float timeConvertX(float clipTime) {
        float rate = Math.abs(clipTime / audioDuration);
        return (track.getWidth() * 1f) * rate;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopMove();
    }
}