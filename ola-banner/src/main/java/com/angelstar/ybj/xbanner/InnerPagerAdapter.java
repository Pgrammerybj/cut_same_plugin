package com.angelstar.ybj.xbanner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/27 11:08
 * @E-Mail: pgrammer.ybj@outlook.com
 * TODO:内部Adapter，包装setAdapter传进来的adapter，设置getCount返回Integer.MAX_VALUE
 */
public class InnerPagerAdapter extends PagerAdapter {

    private final Context mContext;
    /**
     * 轮播图地址集合
     */
    private final List<String> mBannerUrlList;

    private OnItemClickListener mOnItemClickListener;

    public InnerPagerAdapter(Context context, List<String> bannerUrlList) {
        this.mBannerUrlList = bannerUrlList;
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        if (getRealCount() == 0) {
            return null;
        }
        View itemView;
        itemView = LayoutInflater.from(mContext).inflate(R.layout.layout_video_view, container, false);
        if (mOnItemClickListener != null && !mBannerUrlList.isEmpty()) {
            final int finalPosition = position;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onItemClick(v, finalPosition);
                    Toast.makeText(mContext, "点击了：" + finalPosition, Toast.LENGTH_SHORT).show();
                }
            });
        }
        ImageView ivVideoCover = itemView.findViewById(R.id.iv_video_cover);
        TextView tvPagePosition = itemView.findViewById(R.id.tv_page_position);
        FrameLayout surfaceViewContainer = itemView.findViewById(R.id.fl_surfaceView_container);

        int size = mBannerUrlList.size();
        position %= size;
        if (position < 0) {
            position = size + position;
        }
        Glide.with(mContext).load(mBannerUrlList.get(position)).into(ivVideoCover);
        tvPagePosition.setText(String.valueOf(position));

        container.addView(itemView);
        return itemView;
    }

    public int getRealCount() {
        return mBannerUrlList == null ? 0 : mBannerUrlList.size();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }
}
