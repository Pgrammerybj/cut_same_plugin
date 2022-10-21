package com.ola.chat.picker.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import com.ola.chat.picker.R

@SuppressLint("AppCompatCustomView")
class RoundCornerImageView : ImageView {

    private val path = Path()
    private val radius: Int

    constructor(context: Context) : super(context) {
        radius = getRadius(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        radius = getRadius(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        radius = getRadius(context, attrs)
    }

    private fun getRadius(context: Context, attrs: AttributeSet?): Int {
        val ta = context.theme.obtainStyledAttributes(attrs, R.styleable.RoundCornerView, 0, 0)
        val radius = ta.getDimensionPixelSize(R.styleable.RoundCornerView_cutRadius, 0)
        ta.recycle()
        return radius
    }

    override fun draw(canvas: Canvas) {
        if (radius > 0) {
            path.addRoundRect(
                RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat()),
                radius.toFloat(),
                radius.toFloat(),
                Path.Direction.CW
            )
            canvas.clipPath(path)
        }

        try {
            super.draw(canvas)
        } catch (ignore: Throwable) {
            Log.e("RoundCornerImageView", "draw error", ignore)
        }
    }
}