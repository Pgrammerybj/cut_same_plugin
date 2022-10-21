package com.ss.ugc.android.editor.main.template

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentStatePagerAdapter
import com.bytedance.ies.nle.editor_jni.*
import com.bytedance.ies.nlemediajava.utils.MediaUtil
import com.ss.android.vesdk.VEFrameAvailableListener
import com.ss.android.vesdk.VEUtils
import com.ss.ugc.android.editor.base.utils.FileUtil
import com.ss.ugc.android.editor.base.viewmodel.BaseEditorViewModel
import com.ss.ugc.android.editor.core.EditorCoreInitializer
import com.ss.ugc.android.editor.core.commitDone
import com.ss.ugc.android.editor.main.EditorActivity
import com.ss.ugc.android.editor.main.EditorActivityDelegate
import com.ss.ugc.android.editor.main.EditorHelper
import com.ugc.andorid.editor.template.ITemplateStateListener
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class TemplateSettingViewModel(activity: FragmentActivity): BaseEditorViewModel(activity) {
    companion object {
        val MAX_FRAME_CACHE = 60 // 存帧的map （frameMap） 里最多可以存的帧的数目。
    }

    var viewpagerFragmentAdapter: FragmentStatePagerAdapter? = null
    var fragmentlists: MutableList<Fragment>? = null
    var stateListener: ITemplateStateListener? = null
    var isVideoPlayingWhenChangeSeekBar = false
    var mainTrackSlotFrameMessageList = mutableListOf<SlotDataITem>()
    var PIPTrackSlotFrameMessageList = mutableListOf<SlotDataITem>()
    var textTrackSlotMessageList = mutableListOf<SlotDataITem>()
    val frameMap: HashMap<String, Bitmap> = HashMap()
    val mHandler = Handler(Looper.getMainLooper())
    var tabTitleList: MutableList<String>? = null
    var isHavePIPTrack: Boolean? = null
    var isHaveTextStickerTrack: Boolean? = null
    var lockItemList:ArrayList<NLETrackSlot>? = ArrayList() // 记录着加锁的槽位

    val timeChangedRunnable: Runnable = object : Runnable {
        override fun run() {
            stateListener!!.onPlayTimeChanged(
                FileUtil.stringForTime(nleEditorContext.videoPlayer.curPosition()),
                FileUtil.stringForTime(nleEditorContext.videoPlayer.totalDuration())
            )
            mHandler.postDelayed(this, 50)
        }
    }


    fun onRestoreTemplate() {
        val enterType = activity.intent.getIntExtra(EditorHelper.EXTRA_KEY_FROM_TYPE, 0)
        if (enterType == EditorHelper.EXTRA_FROM_TEMPLATE ||
            enterType == EditorActivityDelegate.DRAFT_RESTORE
        ) {
            initLockItemListByMutableItem()
        }
    }

    /**
     *  更新相关数据
     */
    fun updateData() {
        initIsHavePIPTrack()
        initIsHaveTextStickerTrack()
        getSlotsData()
        initTabTitleListAndFragmentList()
    }

    /**
     *  初始化isHavePIPTrack字段，以判断是否存在画中画轨道
     */
    fun initIsHavePIPTrack() {
        isHavePIPTrack = nleEditorContext.nleModel.tracks.size != 1
        val result = nleEditorContext.nleModel.tracks.filter {
            it.trackType == NLETrackType.VIDEO && !it.mainTrack
        }
        isHavePIPTrack = result.isNotEmpty()
    }

    fun initIsHaveTextStickerTrack() {
        val result = nleEditorContext.nleModel.tracks.filter {
            it.trackType == NLETrackType.STICKER
                    && (NLESegmentTextSticker.dynamicCast(it.sortedSlots[0].mainSegment) != null
                    || NLESegmentTextTemplate.dynamicCast(it.sortedSlots[0].mainSegment) != null)
        }
        isHaveTextStickerTrack = result.isNotEmpty()
    }

    fun setDefaultMutableItems() {
        getSlotsData()
        setMappingNode()
    }

    private fun findSlotFromMutableItems(slot: NLETrackSlot, trackType: NLETrackType):Boolean {
        val mutableItem = nleEditorContext.templateInfo!!.mutableItemss
        var slotUUID: String? = null
        when(trackType) {
            NLETrackType.VIDEO -> {
                slotUUID = NLESegmentVideo.dynamicCast(slot.mainSegment).uuid
            }
            NLETrackType.STICKER -> {
                slotUUID = slot.mainSegment.uuid
            }
        }

        mutableItem.forEach { mappingNode->
            if (slotUUID == mappingNode.keyUUID) {  // 若当前slot在mutableitem里，返回true
                return true
            }
        }
        return false
    }

    fun initLockItemListByMutableItem() {
        nleEditorContext.nleTemplateModel.tracks.forEach { _track ->
            val trackType = _track.trackType
            _track.sortedSlots.forEach { _slot->
                if (!findSlotFromMutableItems(_slot,trackType)) { // 若当前slot没在mutableitem中，说明该slot是被锁住的
                    lockItemList?.add(_slot)
                }
            }
        }
    }

    /**
     *   构建在可变槽位上展示的数据SlotDataItem，
     *   生成mainTrackSlotFrameMessageList、
     *   PIPTrackSlotFrameMessageList
     *   textTrackSlotMessageList
     *   即获取视频轨的素材信息 和
     *   文本轨的文字信息
     */
    private fun getSlotsData() {
        // 获取轨道的slot的数据
        nleEditorContext.nleModel.tracks.forEach { track ->
            if (track.trackType == NLETrackType.VIDEO) {
                // 获取主轨道的slot的素材信息
                if (track.mainTrack) {
                    track.sortedSlots.forEach {
                        val slotMediaPath =
                            NLESegmentVideo.dynamicCast(it.mainSegment).resource.resourceFile
                        val slotFrameItem = SlotFrameItem(
                            trackId = nleEditorContext.nleModel.mainTrack!!.id,
                            slotId = it.id,
                            mediaPath = slotMediaPath,
                            slotStartTime = it.startTime / 1000,
                            isLocked = getLockStatus(it, NLETrackType.VIDEO)
                        )
                        mainTrackSlotFrameMessageList.add(slotFrameItem)
                    }
                } else { // 获取画中画轨道的slot的素材的信息
                    track.sortedSlots.forEach {
                        val slotMediaPath =
                            NLESegmentVideo.dynamicCast(it.mainSegment).resource.resourceFile
                        val slotFrameItem = SlotFrameItem(
                            trackId = track.id,
                            slotId = it.id,
                            mediaPath = slotMediaPath,
                            slotStartTime = it.startTime / 1000,
                            isLocked = getLockStatus(it, NLETrackType.VIDEO)
                        )
                        PIPTrackSlotFrameMessageList.add(slotFrameItem)
                    }
                }
                // 获取文本轨道的slot的文字信息
            } else if (track.trackType == NLETrackType.STICKER) {
                track.sortedSlots.forEach { slot ->
                    val stickerSegment = NLESegmentTextSticker.dynamicCast(slot.mainSegment)
                    val stickerTextTemplateSegment =
                        NLESegmentTextTemplate.dynamicCast(slot.mainSegment)
                    stickerSegment?.also {
                        if (it.hasContent()) {
                            val textData = it.content
                            val mainTrackSlot = getTextSlotCorrespondingMainTrackSlot(slot)
                            val slotTextItem = SlotTextItem(
                                textTrackId = track.id,
                                textSlotId = slot.id,
                                mainTrackSlotId = mainTrackSlot?.id,
                                textData = textData,
                                mainTrackSlotMediaPath = mainTrackSlot?.mainSegment?.resource?.resourceFile,
                                slotStartTime = slot.startTime
                            )
                            textTrackSlotMessageList.add(slotTextItem)
                        }
                    }

                    stickerTextTemplateSegment?.also {
                        val contentList = it.textClips
                        var textData: String = ""
                        val mainTrackSlot = getTextSlotCorrespondingMainTrackSlot(slot)
                        contentList.forEach { text ->
                            textData += "${text.content}/"
                        }
                        val slotTextItem = SlotTextItem(
                            textTrackId = track.id,
                            textSlotId = slot.id,
                            mainTrackSlotId = mainTrackSlot?.id,
                            textData = textData,
                            mainTrackSlotMediaPath = mainTrackSlot?.mainSegment?.resource?.resourceFile
                        )
                        textTrackSlotMessageList.add(slotTextItem)
                    }
                } // end foreach
            }
        }
    }

    /**
     *  返回文本轨开始时间对应的主轨的slot
     */
    fun getTextSlotCorrespondingMainTrackSlot(textSlot: NLETrackSlot): NLETrackSlot? {
        val textSlotStartTime = textSlot.startTime
        val textSlotEndTime = textSlot.measuredEndTime
        var mainTrackMediaPath: String? = null
        var correspondingMainTrackSlot: NLETrackSlot? = null
        nleEditorContext.nleMainTrack.sortedSlots.forEach {
            // 遍历主轨道上的所有 slot ，找出文本轨的startTime对应主轨slot的素材
            if (it.startTime <= textSlotStartTime && it.measuredEndTime >= textSlotStartTime) {
                mainTrackMediaPath = it.mainSegment.resource.resourceFile
                correspondingMainTrackSlot = it
            }
        }
        return correspondingMainTrackSlot
    }

    /**
     *  初始化 fragmentlists ，这个列表里存着在viewpager中展示的
     *  fragment
     */
    private fun initTabTitleListAndFragmentList() {
        tabTitleList = mutableListOf("主轨道")
        fragmentlists = mutableListOf(SlotsFrameRecyclerViewFragment(mainTrackSlotFrameMessageList))

        if (isHavePIPTrack!!) {
            tabTitleList!!.add("画中画")
            fragmentlists!!.add(SlotsFrameRecyclerViewFragment(PIPTrackSlotFrameMessageList))
        }
        if (isHaveTextStickerTrack!!) {
            tabTitleList!!.add("文字")
            fragmentlists!!.add(SlotsFrameRecyclerViewFragment(textTrackSlotMessageList))
        }
    }

    /**
     * 通过VE接口获取slot的首帧
     * 如果是外部是 视频轨的素材调用，则直接取slot 的第一帧。
     * 如果是 文本轨 ，那么取的则是 文本轨对应起始位置对应主轨道的slot的那一帧
     * 时间单位：毫秒
     */
    fun getSlotFrameByVE(mediaPath: String, targetTime: Int, isVideo: Boolean): Bitmap? {
        var targetFrameBitmap: Bitmap? = null
        var savePath: String? = null
        val selectedMediaInfo = MediaUtil.getRealVideoMetaDataInfo(mediaPath)
        val width = selectedMediaInfo.width
        val height = selectedMediaInfo.height
        if (isVideo) {
            VEUtils.getVideoFrames2(
                mediaPath,
                intArrayOf(targetTime),
                width,
                height,
                false,
                object :
                    VEFrameAvailableListener {
                    override fun processFrame(
                        frame: ByteBuffer?,
                        width: Int,
                        height: Int,
                        ptsMs: Int
                    ): Boolean {
                        if (frame != null) {
                            targetFrameBitmap =
                                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            targetFrameBitmap?.also {
                                it.copyPixelsFromBuffer(frame.position(0))
                                savePath = saveBitmap(it)
                            }
                            return true
                        } else {
                            return false
                        }
                    }
                })
        } else {
            targetFrameBitmap = BitmapFactory.decodeFile(mediaPath)
            targetFrameBitmap?.also {
                savePath = saveBitmap(it)
            }
        }

        return targetFrameBitmap
    }

    /**
     *  检查模版的uuid
     */
    fun checkTemplateID() {
        nleEditorContext.templateInfo?.let {
            if (it.templateId.isNullOrBlank()) {
                it.templateId = UUID.randomUUID().toString()
            }
        }
    }

    /**
     * 通过 id 找相对应的track
     */
    fun getTrackById(id: Long): NLETrack? {
        nleEditorContext.nleModel.tracks.forEach {
            if (it.id == id) return it
        }
        return null
    }

    /**
     * 通过 id 找相对应的slot
     */
    fun getSlotById(id: Long, track: NLETrack?): NLETrackSlot? {
        track?.sortedSlots?.forEach {
            if (it.id == id) return it
        }
        return null
    }

    /**
     *  保存 Bitmap 到一个路径
     */
    private fun saveBitmap(bm: Bitmap): String {
        val savePath =
            EditorCoreInitializer.instance.appContext!!.externalCacheDir!!.path + "/" + System.currentTimeMillis() + ".png"
        val f = File(savePath)
        try {
            val out: FileOutputStream = FileOutputStream(f)
            bm.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.flush()
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return savePath
    }

//    fun saveMutableItem() = setMappingNode()
    fun saveMutableItem() = setMappingNode()
    fun saveLockItemLIst() = setLockItemList()

    /**
     *  点击「保存」后，记录槽位的开锁/解锁状态
     */
    private fun setLockItemList() {
        lockItemList?.clear()
        val slotItemlists = listOf<List<SlotDataITem>>(
            mainTrackSlotFrameMessageList,
            PIPTrackSlotFrameMessageList,
            textTrackSlotMessageList
        )

        slotItemlists.forEach { slotItemList ->
            if (slotItemList.isNotEmpty()) {
                slotItemList.forEach { slotItem->
                    if (slotItem.isLocked) { // 记住上锁状态的 item
                        val track = getTrackById(slotItem.trackId)
                        track?.also { _track->
                            val slot = getSlotById(slotItem.slotId, _track)
                            slot?.also{ _slot ->
                                lockItemList?.add(_slot)
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     *  给 templateInfo 的mutableItems 设置 mappingNode
     *  即记录可变槽位
     */
    private fun setMappingNode() {
        nleEditorContext.templateInfo!!.clearMutableItems()
        val slotItemlists = listOf<List<SlotDataITem>>(
            mainTrackSlotFrameMessageList,
            PIPTrackSlotFrameMessageList,
            textTrackSlotMessageList
        )
        slotItemlists.forEach { slotItemList ->
            if (slotItemList.isNotEmpty()) {
                slotItemList.forEach { slotItem->
                    if (!slotItem.isLocked) { // 记住解锁状态的 item
                        val track = getTrackById(slotItem.trackId)
                        track?.also { _track ->
                            val slot = getSlotById(slotItem.slotId, _track)
                            slot?.also{ _slot ->
                                nleEditorContext.nleModel
                                val mappingNode = nleEditorContext.nleTemplateModel.convertNLEMappingNode(
                                    _slot.mainSegment
                                )
                                nleEditorContext.templateInfo!!.addMutableItems(mappingNode)
                            }
                        }
                    }
                }
            }
        }
        nleEditorContext.nleEditor.commitDone()
    }

    /**
     *  判断当前slot是否被锁住
     */
    private fun isAlreadyBeLocked (curSlot: NLETrackSlot): Boolean {
        this.lockItemList?.forEach { lockedSlot ->
            if (lockedSlot.uuid == curSlot.uuid) return true
        }
        return false
    }
    /**
     *  获取 模版设置 页面中缩略图的上锁的状态（可变/不可变）
     *  由于一些问题，mutableItem记录的是不可变的槽位
     *  return true 表示不可选， false 表示可选
     */
    private fun getLockStatus(slot: NLETrackSlot, type: NLETrackType): Boolean {
        val mutableItem = nleEditorContext.templateInfo!!.mutableItemss
        val enterType = activity.intent.getIntExtra(EditorHelper.EXTRA_KEY_FROM_TYPE, 0)

        if (!nleEditorContext.hasSettingTemplate // 未进入过模版设置页
            && enterType != EditorActivityDelegate.DRAFT_RESTORE // 入口不是草稿箱
            && enterType != EditorHelper.EXTRA_FROM_TEMPLATE     // 入口不是模版库
        )
            return false // 若没有进入过模版设置页面进行设置，则表示处于默认状态，默认状态为可选

        return isAlreadyBeLocked(slot)

//        mutableItem.forEach { mappingNode ->
//            var slotUUID: String? = null
//            when (type) {
//                NLETrackType.VIDEO -> {
//                    slotUUID = NLESegmentVideo.dynamicCast(slot.mainSegment).uuid
//                }
//                NLETrackType.STICKER -> {
//                    val stickerSlot = NLESegmentTextSticker.dynamicCast(slot)
//                    if (stickerSlot.hasContent()) {
//                        slotUUID = NLESegmentTextSticker.dynamicCast(slot.mainSegment).uuid
//                    } else {
//                        slotUUID = NLESegmentTextTemplate.dynamicCast(slot.mainSegment).uuid
//                    }
//                }
//                else -> {
//                    return false
//                }
//            }
//            if (mappingNode.keyUUID == slotUUID) {
//                return false
//            }
//        }
//        return true
    }

    /**
     * 对外提供的获取全部 可选 视频槽位的 slot 的list
     */
    fun getAllUnlockVideoItem(): ArrayList<NLETrackSlot>{
        val unlockItems = ArrayList<NLETrackSlot>()
        val videoTrackList = nleEditorContext.nleTemplateModel.tracks.filter {
            it.trackType == NLETrackType.VIDEO
        }
        videoTrackList.forEach { videoTrack ->
            videoTrack.sortedSlots.forEach { videoSlot ->
                val lockStatus = getLockStatus(videoSlot, NLETrackType.VIDEO)
                if (!lockStatus) unlockItems.add(videoSlot)
            }
        }
        return unlockItems
    }


    /**
     * 把 templateInfo 序列化成JSON
     */
    fun saveTemplateInfoJsonToTemplateModel(): String {
        return nleEditorContext.templateInfo?.store()?:""
    }

}