package com.ola.chat.picker.album.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.ola.chat.picker.R
import com.ola.chat.picker.album.model.CheckBoxMediaData
import com.ola.chat.picker.album.model.MediaData
import com.ola.chat.picker.customview.RoundCornerImageView
import com.ola.chat.picker.customview.setGlobalDebounceOnClickListener
import com.ola.chat.picker.entry.MediaItem
import com.ola.chat.picker.utils.SizeUtil
import com.ola.chat.picker.utils.showErrorTipToast
import com.ola.chat.picker.viewmodel.GalleryPickerViewModel
import kotlinx.android.synthetic.main.activity_ola_picker_item.view.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

/**
 * 素材列表Adapter，负责适配具体的Item样式，
 * 优先使用自定义ViewHolder，如果没有则使用默认ViewHolder
 * 如果使用自定义ViewHolder，需要在MaterialListViewConfig中实现bindViewHolder接口
 */
class GalleryMaterialListAdapter(
    private val context: Context,
    lifecycleOwner: LifecycleOwner,
    private val galleryPickerViewModel: GalleryPickerViewModel,
    private val isCutSameScene: Boolean
) : RecyclerView.Adapter<GalleryMaterialListAdapter.MaterialViewHolder>() {

    private val mediaDataList = mutableListOf<CheckBoxMediaData>()
    private val dataLists = mutableListOf<MediaData>()
    private var needFullMask = false

    private var itemClickListener: ItemClickListener? = null
    private var itemClickOpenClipListener: ItemClickOpenClipListener? = null


    private var preProcessDataList: List<MediaItem>

    init {
        setHasStableIds(true)
        preProcessDataList =
            galleryPickerViewModel.preProcessPickItem.value ?: Collections.emptyList()
        galleryPickerViewModel.addItem.observe(lifecycleOwner, {
            var addIndex = -1
            mediaDataList.forEachIndexed { index, t ->
                if (it?.source == t.mediaData.path) {
                    mediaDataList[index].isSelected = true
                    addIndex = index
                }
            }
            if (addIndex >= 0) {
                notifyItemChanged(addIndex)
            }
        })

        galleryPickerViewModel.deleteItem.observe(lifecycleOwner, {
            var deleteIndex = -1
            mediaDataList.forEachIndexed { index, t ->
                if (it?.source == t.mediaData.path) {
                    //如果已选择列表中已经不存在当前MediaData，则更新该数据
                    val whetherExistPickData =
                        galleryPickerViewModel.whetherExistPickData(mediaDataList[index].mediaData)
                    if (!whetherExistPickData) {
                        mediaDataList[index].isSelected = false
                        deleteIndex = index
                    }
                }
            }
            if (deleteIndex >= 0) {
                notifyItemChanged(deleteIndex)
            }
        })

        galleryPickerViewModel.pickFull.observe(lifecycleOwner, Observer {
            if (needFullMask == it) {
                return@Observer
            }
            needFullMask = it ?: false
            notifyDataSetChanged()
        })

        notifyItemRangeChanged(0, itemCount, 1)
    }

    fun update(dataList: List<MediaData>) {
        dataLists.clear()
        dataLists.addAll(dataList)
        mediaDataList.clear()
        mediaDataList.addAll(dataToCheckboxData(dataList))
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): MaterialViewHolder {
        return MaterialViewHolder(parent)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int = mediaDataList.size

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        handleCutSameSceneView(holder)
        val data = mediaDataList[position]
        if (data.isSelected) {
            holder.itemView.selectImageView.visibility = View.VISIBLE
            holder.itemView.selectedMask.visibility = View.VISIBLE
        } else {
            holder.itemView.selectImageView.visibility = View.GONE
            holder.itemView.selectedMask.visibility = View.GONE
        }

        bindContent(data, holder)
        bindMask(holder)
        bindListener(data, position, holder)
    }

    private fun bindContent(data: CheckBoxMediaData, holder: MaterialViewHolder) {
        loadThumbnail(data.mediaData, holder.itemView.iVThumb)

        //需要对之前已经选择的素材，显示"已选"标记
        preProcessDataList.forEach {
            if (data.mediaData.path == it.source) {
                holder.itemView.selectImageView.visibility = View.VISIBLE
                holder.itemView.selectedMask.visibility = View.VISIBLE
                return@forEach
            }
        }

        if (data.mediaData.isVideo()) {
            holder.itemView.durationTv.visibility = View.VISIBLE
            holder.itemView.durationTv.text = with(data.mediaData.duration / 1000) {
                val min = this / 60
                val sec = this % 60
                "${if (min < 10) "0$min" else "$min"}:${if (sec < 10) "0$sec" else "$sec"}"
            }
        } else {
            holder.itemView.durationTv.visibility = View.GONE
        }
    }

    private fun bindMask(holder: MaterialViewHolder) {
        if (needFullMask && holder.itemView.selectImageView.visibility == View.GONE) {
            holder.itemView.notSelectMask.visibility = View.VISIBLE
        } else {
            holder.itemView.notSelectMask.visibility = View.GONE
        }
    }

    private fun bindListener(data: CheckBoxMediaData, position: Int, holder: MaterialViewHolder) {
        holder.itemView.setGlobalDebounceOnClickListener {
            if (isCutSameScene) {
                chooseMixItem(data)
            } else {
                itemClickOpenClipListener?.openImageClip(data.mediaData)
            }
        }

        holder.itemView.previewImageView.setOnClickListener {
            itemClickListener?.onItemClick(position, dataLists)
        }
    }

    private fun chooseMixItem(data: CheckBoxMediaData) {
        if (needFullMask) {
            //最多选择n个素材
            val tipMsg = String.format(
                context.resources.getString(R.string.pick_tip_most_count),
                galleryPickerViewModel.processPickItem.value?.size
            )
            showTipToast(tipMsg)
            return
        }

        if (!galleryPickerViewModel.pickOne(data.mediaData)) {
            //素材时长小于要去时长
            val index = galleryPickerViewModel.currentPickIndex.value ?: 0
            if (index >= galleryPickerViewModel.processPickItem.value?.size!!) {
                return
            }
            galleryPickerViewModel.processPickItem.value?.get(index)?.let {
                //视频不能小于xs
                val tipMsg = context.resources.getString(
                    R.string.cutsame_pick_tip_duration_invalid,
                    String.format(
                        Locale.getDefault(),
                        context.resources.getString(R.string.cutsame_common_media_duration_s),
                        it.duration.toFloat() / 1000
                    )
                )
                showTipToast(tipMsg)
            }
        }
    }

    private fun loadThumbnail(media: MediaData, ivThumbnail: RoundCornerImageView) {
        val previousTag = ivThumbnail.getTag(R.id.iVThumb)
        if (null == previousTag || previousTag != media.uri) {
            ivThumbnail.setTag(R.id.iVThumb, media.uri)
            load(
                media.uri,
                ivThumbnail,
                ivThumbnail.measuredWidth,
                ivThumbnail.measuredHeight
            )
        }
    }

    private fun load(
        uri: Uri,
        view: ImageView,
        resWidth: Int,
        resHeight: Int
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
                }

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    view.setImageBitmap(resource)
                }

                override fun onResourceCleared(placeholder: Drawable?) {
                }
            })
        return true
    }

    private fun showTipToast(tipMsg: String) {
        showErrorTipToast(context, tipMsg)
    }

    private fun dataToCheckboxData(dataList: List<MediaData>): List<CheckBoxMediaData> {
        val checkBoxMediaDataList = ArrayList<CheckBoxMediaData>()
        dataList.forEach { mediaData ->
            val checkBoxMediaData = CheckBoxMediaData(mediaData, false)
            checkBoxMediaDataList.add(checkBoxMediaData)
        }
        return checkBoxMediaDataList
    }

    fun setItemClickListener(listener: ItemClickListener?) {
        this.itemClickListener = listener
    }

    fun setItemClickOpenClipListener(listener: ItemClickOpenClipListener?) {
        this.itemClickOpenClipListener = listener
    }

    inner class MaterialViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(getItem(parent))

    private fun getItem(parent: ViewGroup): View {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_ola_picker_item, parent, false)
        //动态设置图片大小 保证宽高相等，条目中间分割的间距,一行3个所以2个间距
        val itemDecorationSize = SizeUtil.dp2px(8F) * 2
        val itemPaddingHorizontal = SizeUtil.dp2px(18F) * 2   //计算的部分放到外面，这样可以优化耗时
        val ivSize =
            (SizeUtil.getScreenWidth(context) - itemDecorationSize - itemPaddingHorizontal) / 3
        itemView.layoutParams.height = ivSize
        return itemView
    }

    private fun handleCutSameSceneView(holder: MaterialViewHolder) {
        val shouldShow = if (isCutSameScene) View.VISIBLE else View.GONE
        holder.itemView.selectedMask.visibility = shouldShow
        holder.itemView.selectImageView.visibility = shouldShow
        holder.itemView.previewImageView.visibility = shouldShow
    }

    interface ItemClickListener {
        fun onItemClick(position: Int, datas: List<MediaData>)
    }

    interface ItemClickOpenClipListener {
        fun openImageClip(mediaData: MediaData)
    }
}
