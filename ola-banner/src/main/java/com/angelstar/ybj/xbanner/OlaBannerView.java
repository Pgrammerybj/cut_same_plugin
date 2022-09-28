package com.angelstar.ybj.xbanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.viewpager.widget.ViewPager;

import com.angelstar.ybj.xbanner.indicator.BaseIndicator;
import com.angelstar.ybj.xbanner.indicator.RectangleIndicator;
import com.angelstar.ybj.xbanner.transformers.ScalePageTransformer;

import java.util.ArrayList;
import java.util.List;

import static android.view.Gravity.BOTTOM;
import static android.view.Gravity.CENTER_HORIZONTAL;


/**
 * github: https://github.com/Pgrammerybj
 * <p>
 * description： 图片轮播控件
 * 1.支持图片无限轮播控件;
 * 2.支持自定义指示器的背景和两种状态指示点;
 **/
public class OlaBannerView extends FrameLayout {

    private static final int MAX_VALUE = Integer.MAX_VALUE;

    /**
     * 轮播图ViewPager
     */
    private ViewPager mBannerViewPager;
    /**
     * 轮播指示器
     */
    private BaseIndicator mIndicator;

    /**
     * 轮播图地址集合
     */
    private ArrayList<VideoItemView> mBannerUrlList;
    /**
     * 轮播内部Adapter,实现无限轮播
     */
    private VideoPagerAdapter mAdapter;
    /**
     * 图片之间的边距
     */
    private int mPageMargin;
    /**
     * 是否启用边距模式（同时显示部分左右Item）
     */
    private boolean mIsMargin;
    private SurfaceView mSurfaceView;

    public OlaBannerView(Context context) {
        this(context, null);
    }

    public OlaBannerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OlaBannerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        handleStyleable(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void init(Context context, AttributeSet attrs) {
        if (mIsMargin) {
            setClipChildren(false);
        }
        initBannerViewPager(context, attrs);
        initIndicatorView(context);
    }

    private void handleStyleable(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.BannerView, defStyle, 0);
        try {
            mPageMargin = ta.getDimensionPixelSize(R.styleable.BannerView_banner_page_margin, 0);
            /**
             * Banner圆角
             */
            float mBannerRadius = ta.getDimensionPixelSize(R.styleable.BannerView_banner_radius, 0);
            if (mPageMargin > 0) {
                mIsMargin = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ta.recycle();
        }
    }

    /**
     * 初始化ViewPager
     */
    private void initBannerViewPager(Context context, AttributeSet attrs) {
        mBannerViewPager = new ViewPager(context, attrs);
        LayoutParams bannerParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (mIsMargin) {
            bannerParams.setMargins(mPageMargin, dp2px(16), mPageMargin, dp2px(16));
        }
        addView(mBannerViewPager, bannerParams);
        mBannerViewPager.addOnPageChangeListener(mPageListener);
        mBannerUrlList = new ArrayList<>();
        mAdapter = new VideoPagerAdapter(context, mBannerUrlList);
        mBannerViewPager.setAdapter(mAdapter);
        mBannerViewPager.setCurrentItem(Integer.MAX_VALUE / 2);
        mBannerViewPager.setPageTransformer(true, new ScalePageTransformer());
        if (mIsMargin) {
            mBannerViewPager.setOffscreenPageLimit(2);
            mBannerViewPager.setPageMargin(mPageMargin / 2);
            mBannerViewPager.setClipChildren(false);
        }
    }

    /**
     * 初始化默认指示器
     */
    private void initIndicatorView(Context context) {
        mIndicator = new RectangleIndicator(context);
        LayoutParams indicatorParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp2px(6));
        indicatorParams.gravity = BOTTOM | CENTER_HORIZONTAL;
        addView(mIndicator, indicatorParams);
    }

    /**
     * 外部传入的指示器
     *
     */
    public void setIndicator(BaseIndicator indicator) {
        removeView(mIndicator);
        mIndicator = indicator;
        LayoutParams indicatorParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp2px(6));
        indicatorParams.gravity = BOTTOM | CENTER_HORIZONTAL;
        addView(mIndicator, indicatorParams);
        invalidate();
    }

    /**
     * ViewPager滑动监听
     */
    ViewPager.OnPageChangeListener mPageListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        int lastPagePosition = 0;

        @SuppressLint("LongLogTag")
        @Override
        public void onPageSelected(int currentPosition) {
            //此处返回的position是大数
            if (currentPosition == 0 || mBannerUrlList.isEmpty()) {
                return;
            }
            int smallPos = currentPosition % mBannerUrlList.size();
            changeSurfaceView(smallPos);
            Log.i("jackyang_onPageSelected 前一个页面lastPagePosition=" + lastPagePosition, " | 当前页面是=" + smallPos);
            mIndicator.setCurrentPosition(smallPos);
            if (mScrollPageListener != null) {
                mScrollPageListener.onPageSelected(smallPos);
            }
            lastPagePosition = currentPosition;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    private void changeSurfaceView(final int smallPos) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mBannerUrlList.size() && (mBannerUrlList.get(i) != null); i++) {
                    mBannerUrlList.get(i).onSelected(i == smallPos, mSurfaceView);
                }
            }
        }, 200);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height;
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        if (hMode == MeasureSpec.UNSPECIFIED || hMode == MeasureSpec.AT_MOST) {
            height = dp2px(200);
        } else {
            height = hSize;
        }
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * 设置Banner图片地址数据
     */
    public void setBannerData(ArrayList<VideoItemView> bannerData, SurfaceView mSurfaceView) {
        int currentPos = MAX_VALUE / 2 - (MAX_VALUE / 2) % getRealCount(bannerData);
        this.mSurfaceView = mSurfaceView;
        mBannerViewPager.setCurrentItem(currentPos);
        mBannerUrlList.clear();
        mBannerUrlList.addAll(bannerData);
        mIndicator.setCellCount(bannerData.size());
        mAdapter.notifyDataSetChanged();
    }

    /**
     * 获取广告页面数量
     */
    public int getRealCount(ArrayList<VideoItemView> bannerData) {
        return bannerData == null ? 0 : bannerData.size();
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    /**
     * 设置是否显示指示器
     */
    public void setHasIndicator(boolean flag) {
        mIndicator.setVisibility(flag ? VISIBLE : GONE);
    }

    /**
     * dp转px
     *
     * @param dpVal dp value
     * @return px value
     */
    public int dp2px(float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal,
                getContext().getResources().getDisplayMetrics());
    }

    /**
     * 滚动监听回调接口
     */

    ScrollPageListener mScrollPageListener;

    public void setScrollPageListener(ScrollPageListener mScrollPageListener) {
        this.mScrollPageListener = mScrollPageListener;
    }

    public interface ScrollPageListener {
        void onPageSelected(int position);
    }
}