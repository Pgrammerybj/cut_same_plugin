package com.cutsame.ui.cut.videoedit.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.bytedance.ies.cutsame.util.SizeUtil
import com.cutsame.ui.R
import com.cutsame.ui.customview.SpacesItemDecoration
import com.cutsame.ui.cut.preview.VideoListAdapter
import com.cutsame.ui.utils.UniversalHorizontalLayoutManager
import com.cutsame.ui.utils.runOnUiThread
import com.ss.android.ugc.cut_ui.MediaItem
import kotlinx.android.synthetic.main.layout_video_edit_view.view.*
import kotlin.math.min

class PlayerMaterialVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var clickPosition = -1
    private var clickProgress = -1L

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.layout_video_edit_view, this, true)
        materialVideoRecycleView.layoutManager = UniversalHorizontalLayoutManager(context)
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

    fun onPlayerProgress(progress: Long) {
        runOnUiThread {
            materialVideoRecycleView.adapter?.apply {
                this as VideoListAdapter
                val indexSet = if (clickPosition >= 0 && clickProgress == progress) {
                    val position = clickPosition
                    clickPosition = -1
                    updatePosition(position)
                } else {
                    update(progress)
                }
                indexSet?.maxOrNull()?.let { max ->
                    if (!materialVideoRecycleView.isPressed
                        && materialVideoRecycleView.scrollState != RecyclerView.SCROLL_STATE_DRAGGING
                        && materialVideoRecycleView.scrollState != RecyclerView.SCROLL_STATE_SETTLING
                    ) {
                        materialVideoRecycleView.smoothScrollToPosition(min(itemCount, max))
                    }
                }
            }
        }
    }

    fun onPlayerMediaItemUpdate(item: MediaItem) {
        materialVideoRecycleView?.adapter?.apply { // 数据变更，不代表view已经准备好，可能为 null
            this as VideoListAdapter
            updateOne(item)
        }
    }
}
