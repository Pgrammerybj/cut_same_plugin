package com.ola.chat.picker.customview.progressbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.ola.chat.picker.utils.SizeUtil;

import java.text.DecimalFormat;

/**
 * @Author：yangbaojiang
 * @Date: 2022/11/02 14:21
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 自定义矩形边框进度条
 */
public class SquareProgressView extends View {

    private double progress;
    private Paint progressBarPaint;
    private Paint outlinePaint;
    private Paint textPaint;

    private float widthInDp = 10;
    private float strokeWidth = 0;
    private Canvas canvas;

    private boolean outline = false;
    private boolean startLine = false;
    private boolean showProgress = false;
    private boolean centerLine = false;

    private boolean roundedCorners = false;
    private float roundedCornersRadius = 10;

    private PercentStyle percentSettings = new PercentStyle(Align.CENTER, SizeUtil.INSTANCE.sp2px(50), true);
    private boolean clearOnHundred = false;
    private boolean isIndeterminate = false;
    private int indeterminate_count = 1;

    private float indeterminate_width = 20.0f;

    public SquareProgressView(Context context) {
        super(context);
        initializePaints(context);
    }

    public SquareProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initializePaints(context);
    }

    public SquareProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializePaints(context);
    }

    private void initializePaints(Context context) {
        progressBarPaint = new Paint();
        progressBarPaint.setColor(context.getResources().getColor(android.R.color.white));
        progressBarPaint.setStrokeWidth(SizeUtil.INSTANCE.dp2px(widthInDp));
        progressBarPaint.setAntiAlias(true);
        progressBarPaint.setStyle(Style.STROKE);

        outlinePaint = new Paint();
        outlinePaint.setColor(context.getResources().getColor(android.R.color.white));
        outlinePaint.setStrokeWidth(1);
        outlinePaint.setAntiAlias(true);
        outlinePaint.setStyle(Style.STROKE);

        textPaint = new Paint();
        textPaint.setColor(context.getResources().getColor(android.R.color.white));
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Style.FILL_AND_STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        this.canvas = canvas;
        super.onDraw(canvas);
        strokeWidth = SizeUtil.INSTANCE.dp2px(widthInDp);
        int cW = getWidth();
        int cH = getHeight();
        float scope = (2 * cW) + (2 * cH) - (4 * strokeWidth);
        float hSw = strokeWidth / 2;

        if (isOutline()) {
            drawOutline();
        }

        if (isStartLine()) {
            drawStartLine();
        }

        if (isShowProgress()) {
            drawPercent(percentSettings);
        }

        if (isCenterLine()) {
            drawCenterLine(strokeWidth);
        }

        if ((isClearOnHundred() && progress == 100.0) || (progress <= 0.0)) {
            return;
        }

        if (isIndeterminate()) {
            @SuppressLint("DrawAllocation") Path path = new Path();
            DrawStop drawEnd = getDrawEnd((scope / 100) * Float.parseFloat(String.valueOf(indeterminate_count)), canvas);

            if (drawEnd.place == Place.TOP) {
                path.moveTo(drawEnd.location - indeterminate_width - strokeWidth, hSw);
                path.lineTo(drawEnd.location, hSw);
                canvas.drawPath(path, progressBarPaint);
            }

            if (drawEnd.place == Place.RIGHT) {
                path.moveTo(cW - hSw, drawEnd.location - indeterminate_width);
                path.lineTo(cW - hSw, strokeWidth
                        + drawEnd.location);
                canvas.drawPath(path, progressBarPaint);
            }

            if (drawEnd.place == Place.BOTTOM) {
                path.moveTo(drawEnd.location - indeterminate_width - strokeWidth,
                        cH - hSw);
                path.lineTo(drawEnd.location, cH
                        - hSw);
                canvas.drawPath(path, progressBarPaint);
            }

            if (drawEnd.place == Place.LEFT) {
                path.moveTo(hSw, drawEnd.location - indeterminate_width
                        - strokeWidth);
                path.lineTo(hSw, drawEnd.location);
                canvas.drawPath(path, progressBarPaint);
            }

            indeterminate_count++;
            if (indeterminate_count > 100) {
                indeterminate_count = 0;
            }
            invalidate();
        } else {
            @SuppressLint("DrawAllocation") Path path = new Path();
            DrawStop drawEnd = getDrawEnd((scope / 100) * Float.parseFloat(String.valueOf(progress)), canvas);

            if (drawEnd.place == Place.TOP) {
                if (drawEnd.location > (cW / 2) && progress < 100.0) {
                    path.moveTo(cW / 2, hSw);
                    path.lineTo(drawEnd.location, hSw);
                } else {
                    path.moveTo(cW / 2, hSw);
                    path.lineTo(cW - hSw, hSw);
                    path.lineTo(cW - hSw, cH - hSw);
                    path.lineTo(hSw, cH - hSw);
                    path.lineTo(hSw, hSw);
                    path.lineTo(strokeWidth, hSw);
                    path.lineTo(drawEnd.location, hSw);
                }
                canvas.drawPath(path, progressBarPaint);
            }

            if (drawEnd.place == Place.RIGHT) {
                path.moveTo(cW >> 1, hSw);
                path.lineTo(cW - hSw, hSw);
                path.lineTo(cW - hSw, 0
                        + drawEnd.location);
                canvas.drawPath(path, progressBarPaint);
            }

            if (drawEnd.place == Place.BOTTOM) {
                path.moveTo(cW >> 1, hSw);
                path.lineTo(cW - hSw, hSw);
                path.lineTo(cW - hSw, cH - hSw);
                path.lineTo(cW - strokeWidth, cH - hSw);
                path.lineTo(drawEnd.location, cH - hSw);
                canvas.drawPath(path, progressBarPaint);
            }

            if (drawEnd.place == Place.LEFT) {
                path.moveTo(cW >> 1, hSw);
                path.lineTo(cW - hSw, hSw);
                path.lineTo(cW - hSw, cH - hSw);
                path.lineTo(hSw, cH - hSw);
                path.lineTo(hSw, cH - strokeWidth);
                path.lineTo(hSw, drawEnd.location);
                canvas.drawPath(path, progressBarPaint);
            }
        }
    }

    private void drawStartLine() {
        Path outlinePath = new Path();
        outlinePath.moveTo(canvas.getWidth() >> 1, 0);
        outlinePath.lineTo(canvas.getWidth() >> 1, strokeWidth);
        canvas.drawPath(outlinePath, outlinePaint);
    }

    private void drawOutline() {
        Path outlinePath = new Path();
        outlinePath.moveTo(0, 0);
        outlinePath.lineTo(canvas.getWidth(), 0);
        outlinePath.lineTo(canvas.getWidth(), canvas.getHeight());
        outlinePath.lineTo(0, canvas.getHeight());
        outlinePath.lineTo(0, 0);
        canvas.drawPath(outlinePath, outlinePaint);
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
        this.invalidate();
    }

    public void setColor(int color) {
        progressBarPaint.setColor(color);
        this.invalidate();
    }

    public void setWidthInDp(int width) {
        this.widthInDp = width;
        progressBarPaint.setStrokeWidth(SizeUtil.INSTANCE.dp2px(widthInDp));
        this.invalidate();
    }

    public boolean isOutline() {
        return outline;
    }

    public void setOutline(boolean outline) {
        this.outline = outline;
        this.invalidate();
    }

    public boolean isStartLine() {
        return startLine;
    }

    public void setStartLine(boolean startLine) {
        this.startLine = startLine;
        this.invalidate();
    }

    private void drawPercent(PercentStyle setting) {
        textPaint.setTextAlign(setting.getAlign());
        if (setting.getTextSize() == 0) {
            textPaint.setTextSize((canvas.getHeight() / 10) << 2);
        } else {
            textPaint.setTextSize(setting.getTextSize());
        }

        String percentString = new DecimalFormat("###").format(getProgress());
        if (setting.isPercentSign()) {
            percentString = percentString + percentSettings.getCustomText();
        }

        textPaint.setColor(percentSettings.getTextColor());

        canvas.drawText(
                percentString, canvas.getWidth() >> 1,
                (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint
                        .ascent()) / 2)), textPaint);
    }

    public boolean isShowProgress() {
        return showProgress;
    }

    public void setShowProgress(boolean showProgress) {
        this.showProgress = showProgress;
        this.invalidate();
    }

    public void setPercentStyle(PercentStyle percentSettings) {
        this.percentSettings = percentSettings;
        this.invalidate();
    }

    public PercentStyle getPercentStyle() {
        return percentSettings;
    }

    public void setClearOnHundred(boolean clearOnHundred) {
        this.clearOnHundred = clearOnHundred;
        this.invalidate();
    }

    public boolean isClearOnHundred() {
        return clearOnHundred;
    }

    private void drawCenterLine(float strokeWidth) {
        float centerOfStrokeWidth = strokeWidth / 2;
        Path centerLinePath = new Path();
        centerLinePath.moveTo(centerOfStrokeWidth, centerOfStrokeWidth);
        centerLinePath.lineTo(canvas.getWidth() - centerOfStrokeWidth, centerOfStrokeWidth);
        centerLinePath.lineTo(canvas.getWidth() - centerOfStrokeWidth, canvas.getHeight() - centerOfStrokeWidth);
        centerLinePath.lineTo(centerOfStrokeWidth, canvas.getHeight() - centerOfStrokeWidth);
        centerLinePath.lineTo(centerOfStrokeWidth, centerOfStrokeWidth);
        canvas.drawPath(centerLinePath, outlinePaint);
    }

    public boolean isCenterLine() {
        return centerLine;
    }

    public void setCenterLine(boolean centerLine) {
        this.centerLine = centerLine;
        this.invalidate();
    }

    public boolean isIndeterminate() {
        return isIndeterminate;
    }

    public void setIndeterminate(boolean indeterminate) {
        isIndeterminate = indeterminate;
        this.invalidate();
    }

    public DrawStop getDrawEnd(float percent, Canvas canvas) {
        DrawStop drawStop = new DrawStop();
        strokeWidth = SizeUtil.INSTANCE.dp2px(widthInDp);
        float halfOfTheImage = canvas.getWidth() >> 1;

        // top right
        if (percent > halfOfTheImage) {
            float second = percent - (halfOfTheImage);

            // right
            if (second > (canvas.getHeight() - strokeWidth)) {
                float third = second - (canvas.getHeight() - strokeWidth);

                // bottom
                if (third > (canvas.getWidth() - strokeWidth)) {
                    float forth = third - (canvas.getWidth() - strokeWidth);

                    // left
                    if (forth > (canvas.getHeight() - strokeWidth)) {
                        float fifth = forth - (canvas.getHeight() - strokeWidth);

                        // top left
                        drawStop.place = Place.TOP;
                        if (fifth != halfOfTheImage) {
                            drawStop.location = strokeWidth + fifth;
                        } else {
                            drawStop.location = halfOfTheImage;
                        }
                    } else {
                        drawStop.place = Place.LEFT;
                        drawStop.location = canvas.getHeight() - strokeWidth - forth;
                    }

                } else {
                    drawStop.place = Place.BOTTOM;
                    drawStop.location = canvas.getWidth() - strokeWidth - third;
                }
            } else {
                drawStop.place = Place.RIGHT;
                drawStop.location = strokeWidth + second;
            }

        } else {
            drawStop.place = Place.TOP;
            drawStop.location = halfOfTheImage + percent;
        }

        return drawStop;
    }

    public void setRoundedCorners(boolean roundedCorners, float radius) {
        this.roundedCorners = roundedCorners;
        this.roundedCornersRadius = radius;
        if (roundedCorners) {
            progressBarPaint.setPathEffect(new CornerPathEffect(roundedCornersRadius));
        } else {
            progressBarPaint.setPathEffect(null);
        }
        this.invalidate();
    }

    public boolean isRoundedCorners() {
        return roundedCorners;
    }

    private static class DrawStop {

        private Place place;
        private float location;

        public DrawStop() {

        }
    }

    public enum Place {
        TOP, RIGHT, BOTTOM, LEFT
    }
}
