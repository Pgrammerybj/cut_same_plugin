package com.cutsame.ui.cut.lyrics.colorselect

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import com.ola.chat.picker.utils.SizeUtil

class ColorSelectContainer : AutoCenterScrollView {

    private var colorList: List<Int>? = null
    private lateinit var linearLayoutContainer: LinearLayout

    private var colorItemViewLeftMargin = SizeUtil.dp2px(0F)
    private var colorItemViewRightMargin = SizeUtil.dp2px(20F)
    private val colorItemViewWidth = SizeUtil.dp2px(25F)
    private val colorItemViewHeight = SizeUtil.dp2px(25F)

    private var colorSelectedListener: OnColorSelectedListener? = null

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        linearLayoutContainer = LinearLayout(context)
        linearLayoutContainer.gravity = Gravity.CENTER_VERTICAL
        val containerLayoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        addView(linearLayoutContainer, containerLayoutParams)
    }

    //@drawable/selector_lyric_color_press
    fun setColorList(colorList: List<Int>) {
        this.colorList = colorList
        linearLayoutContainer.removeAllViews()
        this.colorList?.forEach { colorItem ->
            val colorItemView = ColorCircleView(context = context)
            val layoutParams = LinearLayout.LayoutParams(colorItemViewWidth, colorItemViewHeight)
            layoutParams.leftMargin = colorItemViewLeftMargin
            layoutParams.rightMargin = colorItemViewRightMargin
            colorItemView.setColor(colorItem)
            linearLayoutContainer.addView(colorItemView, layoutParams)

            colorItemView.setOnClickListener {
                colorSelectedListener?.onColorSelected(colorItem)
                smoothScrollToCenter(it)
            }
        }
        requestLayout()
    }

    fun setColorSelectedListener(colorSelectedListener: OnColorSelectedListener) {
        this.colorSelectedListener = colorSelectedListener
    }

    fun smoothScrollToFirst() {
        if (linearLayoutContainer.childCount > 0) {
            scrollToLeft(linearLayoutContainer.getChildAt(0))
        }
    }
}

interface OnColorSelectedListener {
    fun onColorSelected(colorItem: Int)
}