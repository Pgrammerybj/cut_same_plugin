package com.angelstar.ybj.xbanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.viewpager.widget.ViewPager;

import com.angelstar.ybj.xbanner.indicator.BaseIndicator;
import com.angelstar.ybj.xbanner.indicator.RectangleIndicator;
import com.angelstar.ybj.xbanner.transformers.ScalePageTransformer;
import com.angelstar.ybj.xbanner.utils.SizeUtils;

import java.util.ArrayList;

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
    private Context mContext;

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
        mContext = context;
        initBannerViewPager(context, attrs);
        initIndicatorView(context);
    }

    private void handleStyleable(Context context, AttributeSet attrs, int defStyle) {
        TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.BannerView, defStyle, 0);
        try {
            mPageMargin = ta.getDimensionPixelSize(R.styleable.BannerView_banner_page_margin, 0);
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
        mBannerViewPager = new ViewPager(context, attrs);//卡片大小定位260dp*460dp
        LayoutParams bannerParams = new LayoutParams(SizeUtils.dp2px(260, context), SizeUtils.dp2px(460, context));
        if (mIsMargin) {
            bannerParams.setMargins(mPageMargin, 0, mPageMargin, 0);
        }
        bannerParams.gravity = CENTER_HORIZONTAL;
        addView(mBannerViewPager, bannerParams);
        mBannerUrlList = new ArrayList<>();
        mAdapter = new VideoPagerAdapter(mBannerUrlList);
        mBannerViewPager.setAdapter(mAdapter);
        mBannerViewPager.setCurrentItem(Integer.MAX_VALUE / 2);
        mBannerViewPager.setPageTransformer(true, new ScalePageTransformer());
        mBannerViewPager.addOnPageChangeListener(mPageListener);
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
        LayoutParams indicatorParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, SizeUtils.dp2px(6, context));
        indicatorParams.gravity = BOTTOM | CENTER_HORIZONTAL;
        addView(mIndicator, indicatorParams);
    }

    /**
     * 外部传入的指示器
     */
    public void setIndicator(BaseIndicator indicator) {
        removeView(mIndicator);
        mIndicator = indicator;
        LayoutParams indicatorParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, SizeUtils.dp2px(6, mContext));
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
            lastPagePosition = currentPosition;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    private void changeSurfaceView(final int smallPos) {
        for (int i = 0; i < mBannerUrlList.size() && (mBannerUrlList.get(i) != null); i++) {
            mBannerUrlList.get(i).onSelected(i == smallPos, mSurfaceView);
            if (i == smallPos && mScrollPageListener != null) {
                mScrollPageListener.onPageSelected(smallPos, mBannerUrlList.get(i));
            }
        }
    }

    /**
     * 设置Banner图片地址数据
     */
    public void setBannerData(final ArrayList<VideoItemView> bannerData, final SurfaceView mSurfaceView) {
        int currentPos = MAX_VALUE / 2 - (MAX_VALUE / 2) % getRealCount(bannerData);
        this.mSurfaceView = mSurfaceView;
        mBannerViewPager.setCurrentItem(currentPos);
        mBannerUrlList.clear();
        mBannerUrlList.addAll(bannerData);
        mIndicator.setCellCount(bannerData.size());

        if (bannerData.size() != 0) {
            //首次进入mSurfaceView应添加到默认显示的条目0
            final VideoItemView videoItemView = bannerData.get(0);
            Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
                @Override
                public boolean queueIdle() {
                    mScrollPageListener.onPageSelected(0, videoItemView);
                    videoItemView.onSelected(true, mSurfaceView);
                    return false;
                }
            });
        }

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
        mBannerViewPager.removeOnPageChangeListener(mPageListener);
    }

    /**
     * 滚动监听回调接口
     */

    ScrollPageListener mScrollPageListener;

    public void setScrollPageListener(ScrollPageListener mScrollPageListener) {
        this.mScrollPageListener = mScrollPageListener;
    }

    public interface ScrollPageListener {
        void onPageSelected(int position, VideoItemView videoItemView);
    }
}
