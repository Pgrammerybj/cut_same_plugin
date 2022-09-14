package com.cutsame.ui.gallery.album.adapter

import androidx.lifecycle.LifecycleOwner
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.cutsame.ui.R
import com.cutsame.ui.gallery.album.model.CheckBoxMediaData
import com.cutsame.ui.gallery.album.model.MediaData
import com.cutsame.ui.gallery.viewmodel.GalleryPickerViewModel
import kotlinx.android.synthetic.main.activity_default_picker_item.view.*
import kotlin.collections.ArrayList
import com.bytedance.ies.cutsame.util.MediaUtil
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.utils.showErrorTipToast
import com.ss.android.ugc.cut_ui.MediaItem
import java.util.*

class GalleryMaterialListAdapter(
    private val context: Context,
    lifecycleOwner: LifecycleOwner,
    private val galleryPickerViewModel: GalleryPickerViewModel
) : RecyclerView.Adapter<GalleryMaterialListAdapter.MaterialViewHolder>() {

    private val mediaDataList = mutableListOf<CheckBoxMediaData>()
    private val dataLists = mutableListOf<MediaData>()
    private var needFullMask = false

    private var itemClickListener: ItemClickListener? = null

    private var preProcessDataList: List<MediaItem>

    init {
        setHasStableIds(true)
        preProcessDataList = galleryPickerViewModel.preProcessPickItem.value ?: Collections.emptyList()
        galleryPickerViewModel.addItem.observe(lifecycleOwner, Observer {
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

        galleryPickerViewModel.deleteItem.observe(lifecycleOwner, Observer {
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

    override fun onCreateViewHolder(p0: ViewGroup, position: Int): MaterialViewHolder {
        return MaterialViewHolder(p0)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int = mediaDataList.size

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        val data = mediaDataList[position]
        if (data.isSelected) {
            holder.itemView.selectedTv.visibility = View.VISIBLE
        } else {
            holder.itemView.selectedTv.visibility = View.GONE
        }

        bindContent(data, holder)
        bindMask(holder)
        bindListener(data, position, holder)
    }

    private fun bindContent(data: CheckBoxMediaData, holder: MaterialViewHolder) {
        loadThumbnail(data.mediaData, holder.itemView.thumbIv)

        //需要对之前已经选择的素材，显示"已选"标记
        preProcessDataList.forEach {
            if (data.mediaData.path == it.source) {
                holder.itemView.selectedTv.visibility = View.VISIBLE
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
        if (needFullMask && holder.itemView.selectedTv.visibility == View.GONE) {
            holder.itemView.maskView.visibility = View.VISIBLE
        } else {
            holder.itemView.maskView.visibility = View.GONE
        }
    }

    private fun bindListener(data: CheckBoxMediaData, position: Int, holder: MaterialViewHolder) {
        holder.itemView.addLayout.setGlobalDebounceOnClickListener {
            val realVideoMetaDataInfo = MediaUtil.getRealVideoMetaDataInfo(context, data.mediaData.path)
            val minLength = realVideoMetaDataInfo.width.coerceAtMost(realVideoMetaDataInfo.height)
            //暂不支持1080p下的h265的视频
            if(data.mediaData.isVideo() && minLength > 1100 && realVideoMetaDataInfo.codecInfo != "h264"){
                showTipToast(context.resources.getString(R.string.cutsame_pick_not_support_h265))
                return@setGlobalDebounceOnClickListener
            }
            if(needFullMask){
                //最多选择n个素材
                val tipMsg = String.format(
                    context.resources.getString(R.string.cutsame_pick_tip_most_count),
                    galleryPickerViewModel.processPickItem.value?.size
                )
                showTipToast(tipMsg)
                return@setGlobalDebounceOnClickListener
            }

            if (!galleryPickerViewModel.pickOne(data.mediaData)) {
                //素材时长小于要去时长
                val index = galleryPickerViewModel.currentPickIndex.value ?: 0
                if(index >= galleryPickerViewModel.processPickItem.value?.size!!){
                    return@setGlobalDebounceOnClickListener
                }
                galleryPickerViewModel.processPickItem.value?.get(index)?.let {
                    //视频不能小于xs
                    val tipMsg = context.resources.getString(R.string.cutsame_pick_tip_duration_invalid,
                        String.format(Locale.getDefault(), context.resources.getString(R.string.cutsame_common_media_duration_s), it.duration.toFloat() / 1000)
                    )
                    showTipToast(tipMsg)
                }
            }
        }

        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position, dataLists)
        }
    }

    private fun loadThumbnail(media: MediaData, ivThumbnail: ImageView) {
        val previousTag = ivThumbnail.getTag(R.id.thumb)
        if (null == previousTag || previousTag != media.uri) {
            ivThumbnail.setTag(R.id.thumb, media.uri)
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
        if (resWidth > 0 && resHeight > 0) {
            val overrideWidth = Math.min(view.measuredWidth, resWidth)
            val overrideHeight = Math.min(view.measuredHeight, resHeight)
            val requestOptions = RequestOptions().override(overrideWidth, overrideHeight)
            Glide.with(context)
                .load(uri)
                .apply(requestOptions)
                .into(view)
        } else {
            val requestOptions = RequestOptions().override(view.measuredWidth, view.measuredHeight)
            Glide.with(context)
                .load(uri)
                .apply(requestOptions)
                .into(view)
        }
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

    inner class MaterialViewHolder(parent: ViewGroup) : androidx.recyclerview.widget.RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.activity_default_picker_item,
            parent,
            false
        )
    )

    interface ItemClickListener {
        fun onItemClick(position: Int, datas: List<MediaData>)
    }
}
