package com.angelstar.ybj.xbanner.indicator;

import android.content.Context;
import android.util.TypedValue;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 指示器基类，封装指示器切换逻辑
 */
public abstract class BaseIndicator extends LinearLayout implements Indicator {

    private int mCellCount;
    private int mCurrentPos;
    private List<IndicatorCell> mCellViews;

    //选中的视频指示条长度
    protected final float SELECTED_CELL_WIDTH = dp2px(12);
    //未选中的视频指示圆点的半径
    protected final float CELL_RADIUS = dp2px(3);
    //指示器圆点之间的间距
    protected final float CELL_MARGIN = dp2px(6);

    public BaseIndicator(Context context) {
        super(context);
        init();
    }

    public void init() {
        mCellViews = new ArrayList<>();
    }

    /**
     * 设置指示器
     */
    @Override
    public void setCellCount(int cellCount) {
        mCellCount = cellCount;
        int i = 1;
        while (i <= mCellCount) {
            IndicatorCell view = getCellView();
            mCellViews.add(view);
            addView(view);
            i++;
        }
    }

    @Override
    public void setCurrentPosition(int currentPosition) {
        mCurrentPos = currentPosition;
        invalidateCell();
    }


    protected abstract IndicatorCell getCellView();

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 重新测量当前指示器的宽度
        float width = getPaddingLeft() + getPaddingRight() + (CELL_RADIUS * 2 + CELL_MARGIN) * (mCellCount - 1) + SELECTED_CELL_WIDTH;
        float height = getPaddingTop() + getPaddingBottom() + CELL_RADIUS;
        width = resolveSize((int) width, widthMeasureSpec);
        height = resolveSize((int) height, heightMeasureSpec);
        setMeasuredDimension((int) width, (int) height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        invalidateCell();
    }

    public void invalidateCell() {
        for (int i = 0; i < mCellCount; i++) {
            IndicatorCell view = mCellViews.get(i);
            float left;
            left = (CELL_RADIUS * 2 + CELL_MARGIN) * i;
            if (i == mCurrentPos) {
                view.select();
            } else {
                //选中态的指示条宽度不一样,如果当前的小圆点在选中指示条后面，需要特殊处理一下间距
                left += i > mCurrentPos ? (SELECTED_CELL_WIDTH - CELL_RADIUS * 2) : 0;
                view.unSelect();
            }
            view.getLayoutParams().height = (int) (CELL_RADIUS * 2);
            view.setLeft((int) left);
            view.invalidate();
        }
        invalidate();
    }

    /**
     * dp转px
     *
     * @param dpVal dp value
     * @return px value
     */
    protected int dp2px(float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal,
                getContext().getResources().getDisplayMetrics());
    }
}
