package com.angelstar.ybj.xbanner;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
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
public class VideoItemView extends FrameLayout implements View.OnClickListener {
    private Context context;
    //视频封面
    private ImageView mIvVideoCover;
    //当前视频播放状态
    private ImageView mIvVideState;
    //当前视频时长
    private TextView mTvVideoTime;
    //视频编辑按钮
    private TextView mTvEditVideo;
    //surfaceView的插入视图
    private FrameLayout mFlSurfaceViewContainer;
    //下载到本地的文件路径
    public String videoFilePath;

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

    private void init(Context context) {
        this.context = context;
        FrameLayout cardView = (FrameLayout) inflate(context, R.layout.layout_item_video, this);
        if (null == cardView) {
            return;
        }
        mIvVideoCover = cardView.findViewById(R.id.iv_video_cover);
        mIvVideState = cardView.findViewById(R.id.iv_video_player_state);
        mTvVideoTime = cardView.findViewById(R.id.tv_video_time);
        mTvEditVideo = cardView.findViewById(R.id.tv_edit_video);
        mFlSurfaceViewContainer = cardView.findViewById(R.id.fl_surfaceView_container);
    }

    public VideoItemView bindData(String url, String videoFilePath) {
        Glide.with(this.context).load(url).into(mIvVideoCover);
        this.videoFilePath = videoFilePath;
        mTvVideoTime.setText("00:30");
        mIvVideState.setOnClickListener(this);
        mTvEditVideo.setOnClickListener(this);
        return this;
    }

    public ImageView getVideStateView(){
        return mIvVideState;
    }

    public void onSelected(boolean isSelected, SurfaceView mSurfaceView) {
        if (isSelected) {
            //选中添加mSurfaceView
            //防止在添加的时候，别的View还持有
            FrameLayout parent = (FrameLayout) mSurfaceView.getParent();
            if (null != parent) {
                parent.removeView(mSurfaceView);
            }
            if (null != mFlSurfaceViewContainer) {
                mFlSurfaceViewContainer.addView(mSurfaceView);
            }
        } else {
            //非选中态需要移除mSurfaceView
            removeView(mSurfaceView);
        }
    }


    private OnClickPlayStateListener mOnClickPlayStateListener;

    public void setOnClickPlayListener(OnClickPlayStateListener mOnClickPlayStateListener) {
        this.mOnClickPlayStateListener = mOnClickPlayStateListener;
    }

    public interface OnClickPlayStateListener{
        void onVideoClick(View view);
        void onEditVideoClick(View view);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_video_player_state) {
            mOnClickPlayStateListener.onVideoClick(v);
        } else if (id == R.id.tv_edit_video) {
            mOnClickPlayStateListener.onEditVideoClick(v);
        }
    }
}
