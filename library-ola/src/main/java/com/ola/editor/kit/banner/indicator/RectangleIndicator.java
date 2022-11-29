package com.ola.editor.kit.banner.indicator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * 小矩形指示器
 */
public class RectangleIndicator extends BaseIndicator {

    public RectangleIndicator(Context context) {
        super(context);
    }

    @Override
    protected IndicatorCell getCellView() {
        return new RectangleCell(getContext());
    }

    public class RectangleCell extends IndicatorCell {

        private boolean isSelect = false;

        public RectangleCell(Context context) {
            super(context);
        }

        @Override
        public void select() {
            isSelect = true;
            mPaint.setColor(Color.parseColor("#ccffffff"));
        }

        @Override
        public void unSelect() {
            isSelect = false;
            mPaint.setColor(Color.parseColor("#4dffffff"));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (isSelect) {
                //画圆角矩形
                @SuppressLint("DrawAllocation")
                RectF oval3 = new RectF(0, 0, SELECTED_CELL_WIDTH, CELL_RADIUS * 2);// 设置个新的长方形
                canvas.drawRoundRect(oval3, CELL_RADIUS, CELL_RADIUS, mPaint);//第二个参数是x半径，第三个参数是y半径
            } else {
                canvas.drawCircle(CELL_RADIUS, CELL_RADIUS, CELL_RADIUS, mPaint);
            }
        }
    }
}
