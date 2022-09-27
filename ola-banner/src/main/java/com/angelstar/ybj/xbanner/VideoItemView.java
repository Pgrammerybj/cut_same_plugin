package com.angelstar.ybj.xbanner;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/27 17:51
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
public class VideoItemView extends CardView {
    private Context context;
    private ImageView bannerIv;
    private TextView textView;

    public VideoItemView(@NonNull Context context) {
        this(context, null);
    }

    public VideoItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public VideoItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private VideoItemView init(Context context) {
        this.context = context;
        setCardElevation(5);
        setRadius(18);
        //图片
        bannerIv = new ImageView(context);
        bannerIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bannerIv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        //surfaceView

        //文字
        textView = new TextView(context);
        textView.setTextColor(context.getResources().getColor(android.R.color.holo_blue_light));
        textView.setTextSize(30);
        addView(bannerIv);
        addView(textView);
        return this;
    }

    public VideoItemView bindData(String url, String text) {
        Glide.with(this.context).load(url).into(bannerIv);
        textView.setText(text);
        return this;
    }

    public void onSelected(boolean isSelected, SurfaceView mSurfaceView) {
        if (isSelected) {
            //选中添加mSurfaceView
            //防止在添加的时候，别的View还持有
            CardView parent = (CardView) mSurfaceView.getParent();
            if (null != parent) {
                parent.removeView(mSurfaceView);
            }
            addView(mSurfaceView);
        } else {
            //非选中态需要移除mSurfaceView
            removeView(mSurfaceView);
        }
    }
}
