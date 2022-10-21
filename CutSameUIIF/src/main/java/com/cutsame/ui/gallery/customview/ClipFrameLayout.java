package com.cutsame.ui.gallery.customview;

import android.animation.Animator;
import android.animation.RectEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public abstract class ClipFrameLayout extends FrameLayout {
    private final RectF mClipRect = new RectF();
    private final Path mClipPath = new Path();

    private final RectF mVisibleClipRect = new RectF();

    private boolean mUseClipRect = false;
    private boolean mUseClipPath = false;
    private boolean mUseVisibleClipRect = false;

    private Rect mFromRect;
    private Rect mVisibleFromRect;
    private float mRadius;
    private final Rect mViewRect = new Rect();
    private final RectEvaluator mFromEvaluator = new RectEvaluator(new Rect());
    private final RectEvaluator mVisibleEvaluator = new RectEvaluator(new Rect());

    public ClipFrameLayout(Context context) {
        this(context, null);
    }

    public ClipFrameLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClipFrameLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected final boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result = false;

        if (mUseVisibleClipRect) {
            canvas.save();
            canvas.clipRect(mVisibleClipRect);
            if (mUseClipPath) {
                canvas.save();
                canvas.clipPath(mClipPath);
                result = drawChildContent(canvas, child, drawingTime);
                canvas.restore();
            } else if (mUseClipRect) {
                canvas.save();
                canvas.clipRect(mClipRect);
                result = drawChildContent(canvas, child, drawingTime);
                canvas.restore();
            } else {
                result = drawChildContent(canvas, child, drawingTime);
            }
            canvas.restore();
        } else {
            if (mUseClipPath) {
                canvas.save();
                canvas.clipPath(mClipPath);
                result = drawChildContent(canvas, child, drawingTime);
                canvas.restore();
            } else if (mUseClipRect) {
                canvas.save();
                canvas.clipRect(mClipRect);
                result = drawChildContent(canvas, child, drawingTime);
                canvas.restore();
            } else {
                result = drawChildContent(canvas, child, drawingTime);
            }
        }
        return result;
    }

    protected boolean drawChildContent(Canvas canvas, View child, long drawingTime) {
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mViewRect.set(0, 0, getWidth(), getHeight());
    }

    private void setClipRect(Rect fromRect, Rect visibleRect, float radius) {
        if (fromRect.equals(this.mViewRect) && visibleRect.equals(this.mViewRect) && radius == 0) {
            mUseClipRect = false;
            mUseClipPath = false;
            mUseVisibleClipRect = false;
            return;
        }

        mClipRect.set(fromRect);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && radius > 0) {
            mClipPath.rewind();
            mClipPath.addRoundRect(mClipRect, radius, radius, Path.Direction.CCW);
            mUseClipPath = true;
        } else {
            mUseClipRect = true;
        }

        mVisibleClipRect.set(visibleRect);
        mUseVisibleClipRect = !mVisibleClipRect.equals(mClipRect);
        invalidate();
    }

    private void computeAnimation(float percent) {
        if (mFromRect == null || this.mVisibleFromRect == null || mViewRect.isEmpty()) {
            return;
        }

        Rect rect = mFromEvaluator.evaluate(percent, mFromRect, mViewRect);
        Rect visibleRect = mVisibleEvaluator.evaluate(percent, mVisibleFromRect, mViewRect);

        setClipRect(rect, visibleRect, mRadius * (1.0f - percent));
    }

    public Animator getClipAnimator(@NonNull final Rect fromRect, @NonNull final Rect visibleFromRect, final float radius, final boolean invert) {
        this.mFromRect = new Rect(fromRect);
        this.mVisibleFromRect = new Rect(visibleFromRect);
        this.mRadius = radius;
        ValueAnimator clipAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        clipAnimator.addUpdateListener(animation -> {
            float percent = (Float) animation.getAnimatedValue();
            computeAnimation(invert ? (1.0f - percent) : percent);
        });
        return clipAnimator;
    }
}