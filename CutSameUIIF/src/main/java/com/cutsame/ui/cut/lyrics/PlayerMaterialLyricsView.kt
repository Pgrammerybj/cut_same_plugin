package com.cutsame.ui.cut.lyrics

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.bytedance.ies.cutsame.util.SizeUtil
import com.cutsame.ui.R
import com.cutsame.ui.customview.SpacesItemDecoration
import com.cutsame.ui.cut.preview.VideoListAdapter
import com.cutsame.ui.utils.runOnUiThread
import com.ss.android.ugc.cut_ui.MediaItem
import kotlinx.android.synthetic.main.layout_video_edit_view.view.*
import kotlin.math.min

class PlayerMaterialLyricsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.layout_video_edit_view, this, true)
        materialVideoRecycleView.layoutManager =
            object : androidx.recyclerview.widget.LinearLayoutManager(context, HORIZONTAL, false) {
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
        materialVideoRecycleView.setHasFixedSize(true)
        materialVideoRecycleView.addItemDecoration(
            SpacesItemDecoration(
                0,
                SizeUtil.dp2px(16f),
                rowCountLimit = 1
            )
        )
    }

    fun initData(
        mutableMediaItemList: List<MediaItem>,
        itemClickListener: VideoListAdapter.ItemClickListener,
        currentIndexChangeListener: VideoListAdapter.CurrentIndexChangeListener
    ) {
        val adapter = VideoListAdapter(mutableMediaItemList)
        materialVideoRecycleView.adapter = adapter
        adapter.setItemClickListener(itemClickListener)
        adapter.setCurrentIndexChangeListener(currentIndexChangeListener)
    }
}
