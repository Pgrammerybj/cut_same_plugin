package com.ola.editor.kit.cutsame.cut.preview

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

import com.ola.editor.kit.R
import com.ola.editor.kit.cutsame.customview.setGlobalDebounceOnClickListener
import com.ss.android.ugc.cut_ui.MediaItem
import kotlinx.android.synthetic.main.fragment_cut_video_list_item.view.*

private const val TAG = "OlaCut.VideoList"

class VideoListAdapter(
    items: List<MediaItem>
) : RecyclerView.Adapter<VideoListAdapter.VideoListItemHolder>() {

    private var itemClickListener: ItemClickListener? = null
    private var currentIndexChangeListener: CurrentIndexChangeListener? = null
    private val allVideoItems = mutableListOf<MediaItem>()
    private val currentIndexSet = mutableSetOf<Int>()
    private var currentIndex: Int = 0

    init {
        allVideoItems.addAll(items)
        notifyItemRangeChanged(0, itemCount, 1)
    }

    // 替换素材
    fun updateOne(item: MediaItem) {
        allVideoItems.forEachIndexed { index, mediaItem ->
            if (mediaItem.materialId == item.materialId) {
                allVideoItems[index] = item
                notifyItemChanged(index)
                return
            }
        }
    }

    // 如果有变更返回最小index；否则返回-1
    fun update(playProgress: Long): MutableSet<Int>? {
        val oldSet = HashSet(currentIndexSet)
        currentIndexSet.clear()
        run loop@{
            allVideoItems.forEachIndexed { index, mediaItem ->
                if (playProgress >= mediaItem.targetStartTime) {
                    if (index == allVideoItems.size - 1 || playProgress < mediaItem.targetStartTime + mediaItem.duration
                    ) {
                        currentIndexSet.clear()
                        currentIndexSet.add(index)
                    }
                } else {
                    // targetStartTime 是排序的
                    // 所以一旦出现 playProgress 小于 mediaItem.targetStartTime 即可退出循环
                    return@loop
                }
            }
        }

        // 求差
        val subTract = oldSet.toMutableSet().also {
            it.removeAll(currentIndexSet)
            it.addAll(currentIndexSet.toMutableSet().apply { removeAll(oldSet) })
        }

        if (subTract.isNotEmpty()) {
            val minIndex = subTract.minOrNull() ?: 0
            val maxIndex = subTract.maxOrNull() ?: allVideoItems.size - 1
            notifyItemRangeChanged(
                minIndex,
                maxIndex - minIndex + 1,
                0
            ) // payload means only update indicator
            return currentIndexSet
        }

        return null
    }

    fun updatePosition(position: Int): MutableSet<Int>? {
        val oldSet = HashSet(currentIndexSet)
        currentIndexSet.clear()
        currentIndexSet.add(position)

        // 求差
        val subTract = oldSet.toMutableSet().also {
            it.removeAll(currentIndexSet)
            it.addAll(currentIndexSet.toMutableSet().apply { removeAll(oldSet) })
        }

        if (subTract.isNotEmpty()) {
            val minIndex = subTract.minOrNull() ?: 0
            val maxIndex = subTract.maxOrNull() ?: allVideoItems.size - 1
            notifyItemRangeChanged(
                minIndex,
                maxIndex - minIndex + 1,
                0
            ) // payload means only update indicator
            return currentIndexSet
        }

        return null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoListItemHolder {
        return VideoListItemHolder(parent)
    }

    override fun getItemCount(): Int = allVideoItems.size

    override fun onBindViewHolder(holder: VideoListItemHolder, position: Int) {
        val segment = allVideoItems[position]
        val uri = segment.getUri()
        if (uri == Uri.EMPTY) {
            holder.itemView.thumb.setTag(R.id.thumb, null)
            holder.itemView.thumb.setImageResource(0)
        } else {
            if (holder.itemView.thumb.getTag(R.id.thumb) != uri) {
                holder.itemView.thumb.setTag(R.id.thumb, uri)
                load(
                    uri,
                    holder.itemView.thumb,
                    holder.itemView.measuredWidth,
                    holder.itemView.measuredHeight
                )
            }
        }

        if (currentIndexSet.contains(position)) {
            holder.itemView.selectedView.visibility = View.VISIBLE
            holder.itemView.durationView.visibility = View.GONE
            changeCurrentIndex(position)
        } else {
            if (segment.isMutable) {
                holder.itemView.durationView.visibility = View.VISIBLE
                val duration = (1f * segment.duration / 1000f)
                holder.itemView.durationView.text = holder.itemView.context.resources.getString(
                    R.string.cutsame_common_media_duration, duration
                )
            } else {
                holder.itemView.durationView.visibility = View.GONE
            }
            holder.itemView.selectedView.visibility = View.INVISIBLE
        }

        holder.itemView.setGlobalDebounceOnClickListener {
            if (segment.isMutable) {
                itemClickListener?.onItemClick(
                    segment,
                    holder.adapterPosition,
                    isSelect(holder.adapterPosition)
                )
            }
        }
    }


    fun load(
        uri: Uri,
        view: ImageView,
        resWidth: Int,
        resHeight: Int
    ): Boolean {
        Log.d(TAG, "load : $uri ${view} ")

        if (resWidth > 0 && resHeight > 0) {
            val overrideWidth = Math.min(view.measuredWidth, resWidth)
            val overrideHeight = Math.min(view.measuredHeight, resHeight)
            val requestOptions = RequestOptions().override(overrideWidth, overrideHeight)
            Glide.with(view)
                .load(uri)
                .apply(requestOptions)
                .into(view)
        } else {
            val requestOptions = RequestOptions().override(view.measuredWidth, view.measuredHeight)
            Glide.with(view)
                .load(uri)
                .apply(requestOptions)
                .into(view)
        }
        return true
    }

    fun isSelect(position: Int): Boolean = currentIndexSet.contains(position)

    private fun changeCurrentIndex(index: Int) {
        if (this.currentIndex != index) {
            currentIndex = index
            currentIndexChangeListener?.onCurrentIndexChange(allVideoItems[index], index)
        }
    }

    fun setItemClickListener(listener: ItemClickListener) {
        this.itemClickListener = listener
    }

    fun setCurrentIndexChangeListener(listener: CurrentIndexChangeListener) {
        this.currentIndexChangeListener = listener
    }

    inner class VideoListItemHolder(parent: ViewGroup) :
        RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.fragment_cut_video_list_item,
                parent,
                false
            )
        )

    fun interface ItemClickListener {
        fun onItemClick(item: MediaItem, position: Int, isSelect: Boolean)
    }

    fun interface CurrentIndexChangeListener {
        fun onCurrentIndexChange(item: MediaItem, index: Int)
    }
}