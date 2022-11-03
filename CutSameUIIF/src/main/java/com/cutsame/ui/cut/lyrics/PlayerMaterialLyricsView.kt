package com.cutsame.ui.cut.lyrics

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cutsame.ui.R
import com.cutsame.ui.customview.SpacesItemDecoration
import com.ola.chat.picker.utils.SizeUtil
import kotlinx.android.synthetic.main.layout_lyrics_edit_view.view.*

/**
 * 歌词样式View
 */
class PlayerMaterialLyricsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val fontDataList = ArrayList<FontItemEntry>()

    init {
        initView(context)
        mockFontDataList()
    }

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.layout_lyrics_edit_view, this, true)
        //歌词字体
        materialLyricsRecycleView.layoutManager =
            object : LinearLayoutManager(context, HORIZONTAL, false) {
                override fun smoothScrollToPosition(
                    recyclerView: RecyclerView?,
                    state: RecyclerView.State?,
                    position: Int
                ) {
                    val linearSmoothScroller = object :
                        androidx.recyclerview.widget.LinearSmoothScroller(recyclerView!!.context) {
                        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                            return super.calculateSpeedPerPixel(displayMetrics) * 2
                        }

                        override fun calculateDxToMakeVisible(
                            view: View?,
                            snapPreference: Int
                        ): Int {
                            val layoutManager = this.layoutManager
                            return if (layoutManager != null && layoutManager.canScrollHorizontally()) {
                                val params =
                                    view!!.layoutParams as RecyclerView.LayoutParams
                                val left = layoutManager.getDecoratedLeft(view) - params.leftMargin
                                val right =
                                    layoutManager.getDecoratedRight(view) + params.rightMargin
                                val start = layoutManager.paddingLeft
                                val end = layoutManager.width - layoutManager.paddingRight
                                return start + (end - start) / 2 - (right - left) / 2 - left
                            } else {
                                0
                            }
                        }
                    }
                    linearSmoothScroller.targetPosition = position
                    startSmoothScroll(linearSmoothScroller)
                }
            }
        materialLyricsRecycleView.setHasFixedSize(true)
        materialLyricsRecycleView.addItemDecoration(
            SpacesItemDecoration(
                0,
                SizeUtil.dp2px(16f),
                rowCountLimit = 1
            )
        )

        //歌词颜色
        val colorList = ArrayList<Int>()
        colorList.add(Color.parseColor("#FF03DAC5"))
        colorList.add(Color.parseColor("#0000ff"))
        colorList.add(Color.parseColor("#ff0000"))
        colorList.add(Color.parseColor("#ffff00"))
        colorList.add(Color.parseColor("#ffffff"))
        colorList.add(Color.parseColor("#fff000"))
        colorList.add(Color.parseColor("#000fff"))
        colorList.add(Color.parseColor("#FF018786"))
        colorList.add(Color.parseColor("#F0BF42"))
        colorList.add(Color.parseColor("#FFBB86FC"))
        colorList.add(Color.parseColor("#ff8800"))
        colorList.add(Color.parseColor("#1AAD19"))
        colorList.add(Color.parseColor("#1C2134"))
        colorList.add(Color.parseColor("#CCF44336"))

        materialColorSelectContainer.setColorList(colorList)
    }

    fun initData(fontItemList: List<FontItemEntry>, itemClickListener: OnLyricsItemClickListener) {
        val adapter = LyricsFontRecyclerViewAdapter(context, fontItemList)
        materialLyricsRecycleView.adapter = adapter
        adapter.setOnItemClickListener(itemClickListener)
    }


    /**
     * 模拟数据
     */
    private fun mockFontDataList() {
        //歌词颜色
        var i = 0
        val image =
            "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fphoto.workercn.cn%2Fhtml%2Ffiles%2F2015-03%2F12%2F20150312084116035546267.jpg&refer=http%3A%2F%2Fphoto.workercn.cn&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=auto?sec=1669971131&t=087fb4a1d3049cd5a60a2904fe69c0c7"
        while (i < 10) {
            fontDataList.add(FontItemEntry(image, "经典", false, false))
            i++
        }

        //模拟调用
        initData(fontDataList, OnLyricsItemClickListener { view, position ->
            Toast.makeText(context, "点击了:$position", Toast.LENGTH_SHORT).show()
        })
    }
}
