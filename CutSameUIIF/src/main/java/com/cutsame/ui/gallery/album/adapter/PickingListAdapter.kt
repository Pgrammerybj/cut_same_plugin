package com.cutsame.ui.gallery.album.adapter

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.cutsame.ui.R
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.gallery.viewmodel.GalleryPickerViewModel
import com.ss.android.ugc.cut_ui.MediaItem
import kotlinx.android.synthetic.main.activity_default_picker_segment.view.*
import java.util.*
import kotlin.math.min

class PickingListAdapter(
    private val galleryPickerViewModel: GalleryPickerViewModel,
    lifeCycleOwner: LifecycleOwner
) : RecyclerView.Adapter<PickingListAdapter.PickingItemHolder>() {
    private val list = mutableListOf<MediaItem>()
    private var currentPickIndex = 0
    private var deleteClickListener: DeleteClickListener? = null
    private var itemClickListener: ItemClickListener? = null

    init {
        for (mediaItem in galleryPickerViewModel.processPickItem.value!!) {
            list.add(mediaItem)
        }

        galleryPickerViewModel.processPickItem.observe(lifeCycleOwner) {
            list.clear()
            it?.apply { list.addAll(this) }
            notifyItemRangeChanged(0, list.size)
        }

        galleryPickerViewModel.currentPickIndex.observe(lifeCycleOwner) {
            notifyItemChanged(currentPickIndex)//刷新上次位置
            currentPickIndex = it ?: 0
            notifyItemChanged(currentPickIndex)//刷新新选中位置
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickingItemHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.activity_picker_segment,
            parent,
            false
        )
        return PickingItemHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: PickingItemHolder, position: Int) {
        val data = list[position]
        holder.itemView.deleteLayout.tag = position
        holder.itemView.tag = position

        loadThumbnail(data, holder.itemView.thumb) {
            val hasMedia = data.getUri() != Uri.EMPTY
            if (hasMedia) {
                holder.itemView.deleteLayout.visibility = View.VISIBLE
            } else {
                holder.itemView.deleteLayout.visibility = View.GONE
            }
            holder.itemView.isSelected = currentPickIndex == position
                    && galleryPickerViewModel.pickFull.value != true
            holder.itemView.durationTv.isSelected = hasMedia
        }

        holder.itemView.durationTv.text =
            String.format(Locale.getDefault(), holder.itemView.context.resources.getString(
                    R.string.cutsame_common_media_duration_s
                ), data.duration.toFloat() / 1000)

        holder.itemView.setGlobalDebounceOnClickListener {
            itemClickListener?.onItemClick(position, data.getUri() == Uri.EMPTY)
        }

        holder.itemView.deleteLayout.setGlobalDebounceOnClickListener {
            deleteClickListener?.onDeleteLayoutClick(position)
            galleryPickerViewModel.pickFull.value = galleryPickerViewModel.isFull(list)
        }
    }

    fun setDeleteClickListener(newDeleteClickListener: DeleteClickListener?) {
        deleteClickListener = newDeleteClickListener
    }

    fun setItemClickListener(newItemClickListener: ItemClickListener?) {
        itemClickListener = newItemClickListener
    }

    private fun loadThumbnail(
        media: MediaItem,
        ivThumbnail: ImageView,
        //图片加载有延迟，搞一个回调
        endAction: () -> Unit,
    ) {
        val uri = media.getUri()
        if (uri == Uri.EMPTY) {
            ivThumbnail.setTag(R.id.thumb, null)
            ivThumbnail.setImageResource(0)
            endAction.invoke()
        } else {
            val previousTag = ivThumbnail.getTag(R.id.thumb)
            if (null == previousTag || previousTag != uri) {
                ivThumbnail.setTag(R.id.thumb, uri)
                load(
                    uri,
                    ivThumbnail,
                    ivThumbnail.measuredWidth,
                    ivThumbnail.measuredHeight,
                    endAction
                )
            } else {
                endAction.invoke()
            }
        }
    }

    fun load(
        uri: Uri,
        view: ImageView,
        resWidth: Int,
        resHeight: Int,
        endAction: () -> Unit
    ): Boolean {
        val requestOptions: RequestOptions = if (resWidth > 0 && resHeight > 0) {
            val overrideWidth = min(view.measuredWidth, resWidth)
            val overrideHeight = min(view.measuredHeight, resHeight)
            RequestOptions().override(overrideWidth, overrideHeight)
        } else {
            RequestOptions().override(view.measuredWidth, view.measuredHeight)
        }
        Glide.with(view)
            .asBitmap()
            .load(uri)
            .apply(requestOptions)
            .into(object : CustomViewTarget<ImageView, Bitmap>(view) {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    endAction.invoke()
                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    view.setImageBitmap(resource)
                    endAction.invoke()
                }

                override fun onResourceCleared(placeholder: Drawable?) {

                }
            })
        return true
    }

    fun getData(): List<MediaItem> {
        return list
    }

    inner class PickingItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

interface DeleteClickListener {
    fun onDeleteLayoutClick(position: Int)
}

interface ItemClickListener {
    fun onItemClick(position: Int, empty: Boolean)
}


