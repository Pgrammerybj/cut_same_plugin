package com.angelstar.ola.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.angelstar.ola.R;
import com.angelstar.ola.interfaces.IScaleSlideBarAdapter;
import com.angelstar.ola.interfaces.ScaleSlideBarListener;

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/7 18:04
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 带刻度的SlideBar
 */
public class ScaleSlideBar extends View {

    private static final String TAG = "JackYang_SlideBar";

    private RectF mBackgroundPaddingRect;
    private Drawable mBackgroundDrawable;
    private boolean mFirstDraw = true;
    private IScaleSlideBarAdapter mAdapter;
    private int[][] mAnchor;
    private int mCurrentX, mPivotY;
    private boolean mSlide = false;

    private static final int[] STATE_NORMAL = new int[]{};
    private static final int[] STATE_SELECTED = new int[]{android.R.attr.state_selected};
    private static final int[] STATE_PRESS = new int[]{android.R.attr.state_pressed};
    private int[] mState = STATE_SELECTED;
    private int mCurrentItem;

    private int mAnchorWidth, mAnchorHeight;

    private int mPlaceHolderWidth, mPlaceHolderHeight;
    private int mTextMargin;

    private Paint mPaint;
    private int mTextSize;
    private int mTextColor;

    private int mLastX;
    private int mSlideX;

    private int mAbsoluteY;

    private boolean mIsStartAnimation = false, mIsEndAnimation = false;
    private boolean mIsFirstSelect = true, mCanSelect = true;

    private ScaleSlideBarListener gbSlideBarListener;

    private final int defaultTextColor = Color.parseColor("#99ffffff");
    private final int selectedTextColor = Color.parseColor("#ff18d8b5");


    public ScaleSlideBar(Context context) {
        super(context);
        init(null, 0);
    }

    public ScaleSlideBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ScaleSlideBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attributeSet, int defStyleAttr) {
        mBackgroundPaddingRect = new RectF();
        TypedArray a = getContext().obtainStyledAttributes(attributeSet, R.styleable.ScaleSlideBar, defStyleAttr, 0);
        mBackgroundPaddingRect.left = a.getDimension(R.styleable.ScaleSlideBar_slide_paddingLeft, 0.0f);
        mBackgroundPaddingRect.top = a.getDimension(R.styleable.ScaleSlideBar_slide_paddingTop, 0.0f);
        mBackgroundPaddingRect.right = a.getDimension(R.styleable.ScaleSlideBar_slide_paddingRight, 0.0f);
        mBackgroundPaddingRect.bottom = a.getDimension(R.styleable.ScaleSlideBar_slide_paddingBottom, 0.0f);

        mAnchorWidth = (int) a.getDimension(R.styleable.ScaleSlideBar_slide_anchor_width, 50.0f);
        mAnchorHeight = (int) a.getDimension(R.styleable.ScaleSlideBar_slide_anchor_height, 50.0f);

        mPlaceHolderWidth = (int) a.getDimension(R.styleable.ScaleSlideBar_slide_placeholder_width, 20.0f);
        mPlaceHolderHeight = (int) a.getDimension(R.styleable.ScaleSlideBar_slide_placeholder_height, 20.0f);

        mBackgroundDrawable = a.getDrawable(R.styleable.ScaleSlideBar_slide_background);

        mTextSize = a.getDimensionPixelSize(R.styleable.ScaleSlideBar_slide_textSize, 28);
        mTextColor = a.getColor(R.styleable.ScaleSlideBar_slide_textColor, defaultTextColor);

        mTextMargin = (int) a.getDimension(R.styleable.ScaleSlideBar_slide_text_margin, 0.0f);
        a.recycle();
    }

    private void drawBackground() {

        //让最左侧和最右侧都显示完整
        Rect rect = new Rect((int) mBackgroundPaddingRect.left + mAnchorWidth,
                (int) mBackgroundPaddingRect.top,
                (int) (getWidth() - mBackgroundPaddingRect.right - mAnchorWidth),
                (int) (getHeight() - mBackgroundPaddingRect.bottom));
        mBackgroundDrawable.setBounds(rect);

        mAbsoluteY = (int) (mBackgroundPaddingRect.top - mBackgroundPaddingRect.bottom);

        Log.d(TAG, "mAbsoluteY:" + mBackgroundPaddingRect.top + " : " + mBackgroundPaddingRect.bottom + " : " + (mBackgroundPaddingRect.top - mBackgroundPaddingRect.bottom));

        mCurrentX = getWidth() / 2;
        mPivotY = getHeight() / 2;

        mAnchor = new int[getCount()][2];
        for (int i = 0, j = 1; i < getCount(); i++, j++) {
            if (i == 0) {
                mAnchor[i][0] = rect.left;
            } else {
                mAnchor[i][0] = (rect.right - rect.left) / (getCount() - 1) * i + rect.left;
            }
            mAnchor[i][1] = mPivotY + mAbsoluteY / 2;
        }

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextSize(mTextSize);
        mPaint.setColor(mTextColor);
        mPaint.setTextAlign(Paint.Align.CENTER);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFirstDraw) drawBackground();
        if (mBackgroundDrawable != null) mBackgroundDrawable.draw(canvas);
        if (isInEditMode()) return;

        Drawable itemDefault, itemSlide;
        StateListDrawable stateListDrawable;

        if (!mSlide) {
            int distance, minIndex = 0, minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < getCount(); i++) {
                distance = Math.abs(mAnchor[i][0] - mCurrentX);
                if (minDistance > distance) {
                    minIndex = i;
                    minDistance = distance;
                }
            }

            setCurrentItem(minIndex);
            stateListDrawable = mAdapter.getItem(minIndex);
        } else {
            mSlide = false;
            mCurrentX = mAnchor[mCurrentItem][0];
            if (mFirstDraw) {
                mSlideX = mLastX = mCurrentX;
            }
            stateListDrawable = mAdapter.getItem(mCurrentItem);

            mIsFirstSelect = true;
        }
        stateListDrawable.setState(mState);
        itemDefault = stateListDrawable.getCurrent();


        for (int i = 0; i < getCount(); i++) {
            if (i == mCurrentItem) {
                mPaint.setColor(selectedTextColor);
            } else {
                mPaint.setColor(defaultTextColor);
            }
            canvas.drawText(mAdapter.getText(i), i == getCount() - 1 ? mAnchor[i][0] - 10 : mAnchor[i][0], mAnchor[i][1] + (mAnchorHeight * 3 >> 1) + mTextMargin, mPaint);
            stateListDrawable = mAdapter.getItem(i);
            stateListDrawable.setState(STATE_NORMAL);
            itemSlide = stateListDrawable.getCurrent();
            itemSlide.setBounds(
                    mAnchor[i][0] - mPlaceHolderWidth,
                    mAnchor[i][1] - mPlaceHolderHeight,
                    mAnchor[i][0] + mPlaceHolderWidth,
                    mAnchor[i][1] + mPlaceHolderHeight
            );
            itemSlide.draw(canvas);
        }

        itemDefault.setBounds(
                mSlideX - mAnchorWidth,
                mPivotY + mAbsoluteY / 2 - mAnchorHeight,
                mSlideX + mAnchorWidth,
                mPivotY + mAbsoluteY / 2 + mAnchorHeight
        );

        itemDefault.draw(canvas);

        setFirstDraw();

    }


    private void endSlide() {
        if (!mIsEndAnimation && mSlide) {
            mIsEndAnimation = true;
            ValueAnimator mEndAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
            mEndAnim.setDuration(200);
            mEndAnim.setInterpolator(new LinearInterpolator());
            mEndAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mSlideX = (int) ((mCurrentX - mLastX) * animation.getAnimatedFraction() + mLastX);
                    invalidate();
                }
            });
            mEndAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mIsStartAnimation = false;
                    mLastX = mCurrentX;
                    mIsEndAnimation = false;
                    mCanSelect = true;
                    invalidate();
                }
            });
            mEndAnim.start();
        } else {

            mLastX = mCurrentX;
            mSlideX = mCurrentX;
            invalidate();
        }
    }

    private void startSlide() {
        if (!mIsStartAnimation && !mSlide && mCanSelect) {
            mIsStartAnimation = true;
            ValueAnimator mStartAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
            mStartAnim.setDuration(200);
            mStartAnim.setInterpolator(new LinearInterpolator());
            mStartAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mSlideX = (int) ((mCurrentX - mLastX) * animation.getAnimatedFraction() + mLastX);

                    invalidate();
                }
            });
            mStartAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {

                    mLastX = mCurrentX;
                    mIsStartAnimation = false;
                    mCanSelect = true;
                    invalidate();
                }
            });
            mStartAnim.start();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (mCanSelect) {
            int action = event.getAction();
            //获取当前坐标
            mCurrentX = getNormalizedX(event);

            mSlide = action == MotionEvent.ACTION_UP;

            if (!mSlide && mIsFirstSelect) {
                startSlide();
                mIsFirstSelect = false;

            } else if (!mIsStartAnimation && !mIsEndAnimation) {
                endSlide();
            }


            mState = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ? STATE_SELECTED : STATE_PRESS;

            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    return true;
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "Down " + event.getX());
                    return true;
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "Up " + event.getX());
                    mCanSelect = false;
                    invalidate();
                    return true;
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(event);
    }

    private int getNormalizedX(MotionEvent event) {
        return Math.min(Math.max((int) event.getX(), mAnchorWidth), getWidth() - mAnchorWidth);
    }

    private void setFirstDraw() {
        mFirstDraw = false;
    }

    private int getCount() {
        return isInEditMode() ? 3 : mAdapter.getCount();
    }

    private void setCurrentItem(int currentItem) {
        if (mCurrentItem != currentItem && gbSlideBarListener != null) {
            gbSlideBarListener.onPositionSelected(currentItem);
        }
        mCurrentItem = currentItem;
    }

    public void setAdapter(IScaleSlideBarAdapter adapter) {
        mAdapter = adapter;
    }

    public void setPosition(int position) {
        position = Math.max(position, 0);
        position = position > mAdapter.getCount() ? mAdapter.getCount() - 1 : position;
        mCurrentItem = position;
        mSlide = true;
        invalidate();
    }

    public void setOnGbSlideBarListener(ScaleSlideBarListener listener) {
        this.gbSlideBarListener = listener;
    }
}
