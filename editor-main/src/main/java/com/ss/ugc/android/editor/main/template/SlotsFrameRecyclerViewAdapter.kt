package com.ss.ugc.android.editor.main.template

import android.graphics.Bitmap
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bytedance.ies.nle.editor_jni.*
import com.ss.ugc.android.editor.base.utils.runOnUiThread
import com.ss.ugc.android.editor.core.vm.EditViewModelFactory
import com.ss.ugc.android.editor.main.R
import com.ss.ugc.android.editor.track.utils.VideoFrameUtil.getVideoFrame
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SlotsFrameRecyclerViewAdapter(
    var fragment: SlotsFrameRecyclerViewFragment,
    var slotsFramePathList:MutableList<SlotDataITem>):
    RecyclerView.Adapter<SlotsFrameRecyclerViewAdapter.SlotsFrameViewHolder>() ,View.OnClickListener {

    private val templateSettingViewModel by lazy {
        EditViewModelFactory.viewModelProvider(fragment).get(TemplateSettingViewModel::class.java)
    }

    /**
     * 用于给文本轨道抽帧的线程池
     */
    var mExecutorService :ExecutorService? = null
    init {
        mExecutorService = Executors.newFixedThreadPool(10)
    }

    /**
     *  当recyclerView数据过多时，刷新数据会导致闪烁，
     *  闪烁是 notifyDataSetChange 造成的。由于适配器不知道整个数据集中的哪些内容已经存在，
     *  在重新匹配 ViewHolder 时发生的。
     *  因此重写该方法来给每个 Item 一个唯一的ID。
     */
    override fun getItemId(position: Int): Long {
        return slotsFramePathList[position].hashCode().toLong()
    }

    class SlotsFrameViewHolder(itemview:View):RecyclerView.ViewHolder(itemview){
        var frame:ImageView = itemview.findViewById(R.id.slot_frame_iv)
        var lockView:View = itemView.findViewById(R.id.lockView)
        var textView:TextView = itemView.findViewById(R.id.text_content_tv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): SlotsFrameViewHolder {
        val itemView = LayoutInflater.from(fragment.context).inflate(R.layout.slot_frame_item,parent,false)
        return SlotsFrameViewHolder(itemView)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: SlotsFrameViewHolder, position: Int) {
        val dataItem = slotsFramePathList[position]
        val track = templateSettingViewModel.getTrackById(dataItem.trackId)
        holder.itemView.setTag(holder.itemView.id, dataItem.slotId)
        track?.also { dataTrack ->
            getFrameFromTrackSlot(dataItem, dataTrack) { targetBitmap ->
                runOnUiThread {
                    if (fragment.isDetached || !fragment.isAdded
                        || holder.itemView.getTag(holder.itemView.id) != dataItem.slotId
                    ) {
                        return@runOnUiThread
                    }
                    if (targetBitmap != null) {
                        holder.frame.setImageBitmap(targetBitmap)
                    }
                    holder.frame.scaleType = ImageView.ScaleType.CENTER_CROP
                    holder.frame.setOnClickListener(this)
                    holder.lockView.setOnClickListener(this)
                    holder.frame.setTag(holder.frame.id,dataItem)
                    if ((dataItem as SlotDataITem).isSelected) {
                        holder.frame.foreground = fragment.context?.getDrawable(R.drawable.bound_shape)
                        holder.textView.setTextColor(fragment.resources.getColor(R.color.white))
                    } else {
                        holder.frame.foreground = null
                        holder.textView.setTextColor(fragment.resources.getColor(R.color.transparent_50p_white))
                    }

                    holder.lockView.setTag(holder.lockView.id,dataItem)
                    if ((dataItem as SlotDataITem).isLocked) {
                        holder.lockView.setBackgroundResource(R.drawable.lock)
                    } else {
                        holder.lockView.setBackgroundResource(R.drawable.unlock)
                    }

                    // init textView
                    if (track.trackType == NLETrackType.STICKER) {
                        val rawTextData = (dataItem as SlotTextItem).textData
                        var showTextData = rawTextData
//                        if (rawTextData.length > 11) {
//                            showTextData = rawTextData.subSequence(0,11).toString()
//                            showTextData += "..."
//                        }
                        holder.textView.text = showTextData
                        holder.textView.visibility = View.VISIBLE
                    }
                }
            }

        }
    }

    private fun getMediaTime (textDataItem: SlotTextItem ,stickerSlot: NLETrackSlot): Int? {
        var mainTrackSlot: NLETrackSlot? = null
        textDataItem.mainTrackSlotId?.also { mainTrackSlotId ->
            mainTrackSlot = templateSettingViewModel.getSlotById(
                mainTrackSlotId,
                templateSettingViewModel.nleEditorContext.nleMainTrack
            )
        }
        val mainTrackSlotStartTime = mainTrackSlot?.startTime
        return if (mainTrackSlotStartTime != null) {
            (stickerSlot.startTime / 1000 - mainTrackSlotStartTime / 1000).toInt() // 当前文本轨道的起始时间相当于 主轨slot素材的时间
        } else {
            null
        }
    }


    /**
     *  从 slot 中获取缩略图的帧
     */
    private fun getFrameFromTrackSlot(dataItem: SlotDataITem, track:NLETrack, onGetFrame: (Bitmap: Bitmap?)->Unit) {
        val slot = templateSettingViewModel.getSlotById(dataItem.slotId,track)
        slot?.also { dataSlot ->

                when (track.trackType) {
                    NLETrackType.VIDEO -> {
                        val resourcePath = dataSlot.mainSegment.resource.resourceFile
                        // 这里的isImage可以设置为true，因为对于视频轨而言，无论素材是视频还是图片，抽的都是第一帧，所以getVideoFrame这个接口的isImage参数为
                        // true 还是 false 其实都可以
                        getVideoFrame(    // 1. 先从trackPanel的缓存中尝试取帧
                            resourcePath = resourcePath,
                            timeStamp = dataItem.slotStartTime.toInt(),
                            isImage = dataSlot.mainSegment.type == NLEResType.IMAGE
                        ) {
                            if (it != null) {
                                onGetFrame(it)
                            } else {    // 2. 若从trackpanel的缓存中无法取到所需的帧，则尝试从 frameMap这个本模块的缓存中取

                                val frameKey = "${dataItem.slotId}#${0}"  // 产生帧的key，视频帧生成key的逻辑是用slot的id 加上0 ，因为视频帧取的是slot的第一帧
                                val getFrame = templateSettingViewModel.frameMap[frameKey]

                                if (getFrame == null) {
                                    NLESegmentVideo.dynamicCast(dataSlot.mainSegment)?.also { segment ->
                                        // 若在两个缓存中都找不到目标帧，则使用线程池进行抽帧
                                        mExecutorService?.execute(
                                            GetSlotFrameThread(
                                                templateSettingViewModel,
                                                resourcePath,
                                                segment.timeClipStart.toInt() / 1000,
                                                dataSlot.mainSegment.type.name == "VIDEO") { targetBitmap ->

                                                targetBitmap?.also {
                                                    templateSettingViewModel.frameMap[frameKey] = targetBitmap // 把抽到的帧放入frameMap缓存
                                                }
                                                onGetFrame(targetBitmap)
                                            }
                                        )
                                    }
                                } else {
                                    onGetFrame(getFrame)
                                }
                            }

                        }
                    }

                    NLETrackType.STICKER -> {
                        val textDataItem = dataItem as SlotTextItem
                        val mediaTime = getMediaTime(textDataItem, slot)
                        // 判断帧是否在 hashmap 中
                        val frameKey = "${textDataItem.mainTrackSlotMediaPath}#${mediaTime}"
                        if (templateSettingViewModel.frameMap[frameKey] == null) {
                            getFrameFromTextTrackSlot(textDataItem, slot, mediaTime) {
                                onGetFrame(it)
                                if (it != null) {
                                    templateSettingViewModel.frameMap[frameKey] = it
                                }
                            }
                        } else {
                            onGetFrame(templateSettingViewModel.frameMap[frameKey])
                        }
                    }
                    else -> {
                        // todo
                    }
                }

        }
        onGetFrame(null)
    }

    /**
     *  用于给 文本轨道/文本模版轨 抽帧的接口
     */
    private fun getFrameFromTextTrackSlot(textDataItem: SlotTextItem, textSlot:NLETrackSlot?,mediaTime: Int?  ,onGetFrame: (Bitmap: Bitmap?) -> Unit){
        val targetBitmap: Bitmap? = null
        var mainTrackSlot:NLETrackSlot? = null
        textDataItem.mainTrackSlotId?.also { mainTrackSlotId ->
            mainTrackSlot = templateSettingViewModel.getSlotById(mainTrackSlotId,templateSettingViewModel.nleEditorContext.nleMainTrack)
        }
        textSlot?.also { stickerSlot ->
        if (mediaTime == null || mainTrackSlot == null) return@also
            NLESegmentTextSticker.dynamicCast(stickerSlot.mainSegment)?.also { textSegment ->
                if (!textSegment.hasContent()) return@also
                // 获取 文本轨道 起始位置 对应的 主轨道 素材的 那一帧
                getStickerSlotCorrespondingMainTrackSlotBitMap(
                    mainTrackSlot!!,
                    textDataItem,
                    mediaTime
                ) {
                    onGetFrame(it)
                }
            }

            NLESegmentTextTemplate.dynamicCast(stickerSlot.mainSegment)?.also { textTemplateSegment ->
                // 获取 文本模版轨道 起始位置 对应的 主轨道 素材的 那一帧
                getStickerSlotCorrespondingMainTrackSlotBitMap(
                    mainTrackSlot!!,
                    textDataItem,
                    mediaTime
                ) {
                    onGetFrame(it)
                }
            }
            onGetFrame(targetBitmap)
        }

    }

    /**
     * 获取 文字贴纸（包括文本轨和文字模版轨）对应主轨道的slot的帧
     * 获取规则：贴纸轨道的起始位置对应主轨道的那一帧
     */
    private fun getStickerSlotCorrespondingMainTrackSlotBitMap(
        mainTrackSlot: NLETrackSlot,
        textDataItem: SlotTextItem,
        mediaTime: Int,
        onGetFrame: (Bitmap: Bitmap?) -> Unit
    ) {
        val mediaType = mainTrackSlot.mainSegment.type.name // 判断对应主轨slot的素材是图片还是视频
        val isVideo = mediaType == "VIDEO"

        mExecutorService?.execute(
            GetSlotFrameThread(
                templateSettingViewModel,
                textDataItem.mainTrackSlotMediaPath!!,
                mediaTime,
                isVideo) {
                onGetFrame(it)
            }
        )
    }

    /**
     *  用于给抽帧的线程。
     *  对于文本轨道：
     *  规则是 抽 文本轨道/文本轨道 起始时间对应主轨道的slot的那帧
     *  对于视频 轨，则抽slot的第一帧
     */
    class GetSlotFrameThread(
        private val templateSettingViewModel: TemplateSettingViewModel, val resource: String,
        private val time: Int, private val isVideo: Boolean, val onComplete: (Bitmap: Bitmap?) -> Unit
    ) : Runnable {
        var targetBitmap: Bitmap? = null
        override fun run() {
            targetBitmap = templateSettingViewModel.getSlotFrameByVE(resource, time, isVideo)
            onComplete(targetBitmap)
        }
    }

    override fun getItemCount(): Int {
        return slotsFramePathList.size
    }


    /**
     *  点击事件
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onClick(v: View?) {
        when(v!!.id) {
            R.id.slot_frame_iv-> {
                val item:SlotDataITem = v.getTag(v.id) as SlotDataITem
                val track = templateSettingViewModel.getTrackById(item.trackId)
                val slot = templateSettingViewModel.getSlotById(item.slotId,track)
                slot?.also {
                    templateSettingViewModel.nleEditorContext.videoPlayer.playRange((it.startTime/1000).toInt(),(it.measuredEndTime/1000).toInt())
                }
                slotsFramePathList.forEach {
                    (it as SlotDataITem).isSelected = it.slotId == item.slotId
                }

                notifyDataSetChanged()
            }
            R.id.lockView -> {
                val item:SlotDataITem = v.getTag(v.id) as SlotDataITem
                slotsFramePathList.forEach {
                    if ((it as SlotDataITem).slotId == item.slotId) {
                        it.isLocked = !it.isLocked
                    }
                }
                notifyDataSetChanged()
            }
        }
    }
}
