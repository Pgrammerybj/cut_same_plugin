package com.ss.ugc.android.editor.track

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.bytedance.ies.nle.editor_jni.*
import com.bytedance.ies.nle.editor_jni.NLETrackType.EFFECT
import com.bytedance.ies.nle.editor_jni.NLETrackType.VIDEO
import com.bytedance.ies.nleeditor.getMainSegment
import com.bytedance.ies.nlemediajava.Log
import com.bytedance.ies.nlemediajava.NLEPlayer
import com.bytedance.ies.nlemediajava.ifCanLog
import com.ss.ugc.android.editor.base.extensions.dp
import com.ss.ugc.android.editor.base.extensions.visible
import com.ss.ugc.android.editor.base.imageloder.ImageLoader
import com.ss.ugc.android.editor.base.imageloder.ImageOption
import com.ss.ugc.android.editor.base.theme.ThemeStore
import com.ss.ugc.android.editor.base.utils.BitmapUtil
import com.ss.ugc.android.editor.base.utils.FileUtil
import com.ss.ugc.android.editor.base.utils.Logger
import com.ss.ugc.android.editor.base.utils.runOnUiThread
import com.ss.ugc.android.editor.core.getRecordSegmentCount
import com.ss.ugc.android.editor.core.isEffectSlot
import com.ss.ugc.android.editor.core.track
import com.ss.ugc.android.editor.core.utils.DLog
import com.ss.ugc.android.editor.track.R.layout
import com.ss.ugc.android.editor.track.TrackState.*
import com.ss.ugc.android.editor.track.frame.FrameRequest
import com.ss.ugc.android.editor.track.frame.VideoFluencyHelper
import com.ss.ugc.android.editor.track.frame.VideoFrameCache
import com.ss.ugc.android.editor.track.fuctiontrack.*
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.KeyframeStateDelegate
import com.ss.ugc.android.editor.track.holder.VideoTrackHolder
import com.ss.ugc.android.editor.track.utils.DataHolder
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.utils.TrackAdsorptionHelper
import com.ss.ugc.android.editor.track.utils.getTrackByVideoEffect
import com.ss.ugc.android.editor.track.widget.*
import kotlinx.android.synthetic.main.track_pannel_layout.view.*
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

class TrackPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ITrackPanel, CoroutineScope {
    init {
        TrackSdk.init(context.applicationContext) //方便整个模块获取context
    }

    private var recordPosition: Long = 0
    var isRecording: Boolean = false
    private val TAG: String = "TrackPanel"
    private var nleModel: NLEModel? = null
    private var lastSeekTimeStamp = 0L
    private var timestamp: Long = 0L
    private var nleMainTrack: NLETrack? = null
    var trackPanelActionListener: TrackPanelActionListener? = null
    private lateinit var multiTrackAdapter: MultiTrackAdapter
    private var currentUIState: TrackState = TrackState.NORMAL
    private var scale = 1.0
    private var enableOriginalVoiceIcon = R.drawable.ic_volume_ori
    private var disableOriginalVoiceIcon = R.drawable.ic_volume_mute

    private var scaleListener = object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(view: View, detector: ScaleGestureDetector): Boolean {
            if ((scale == 0.1 && detector.scaleFactor < 1) || (scale == 10.0 && detector.scaleFactor > 1)) {
                return true
            }
            detector.apply {
                updateScale(detector.scaleFactor)
                trackPanelActionListener?.onScale(detector.scaleFactor)
            }
            return true
        }

        override fun onScaleBegin(view: View, detector: ScaleGestureDetector): Boolean {
            multiTrack.startScale()
            trackPanelActionListener?.onScaleBegin()
            return true
        }

        override fun onScaleEnd(view: View, detector: ScaleGestureDetector) {
            multiTrack.endScale()
            trackPanelActionListener?.onScaleEnd()
        }
    }
    var videoTrackHolder: VideoTrackHolder? = null
    var enableAutoSelectNext: Boolean = false
    var selectedTrackStyle: MultiTrackLayout.TrackStyle
        get() {
            return multiTrack.selectedTrackStyle
        }
        set(value) {
            multiTrack.selectedTrackStyle = value
        }
    var isSelectAgainToRoot: Boolean
        get() {
            return multiTrack.isSelectAgainToRoot
        }
        set(value) {
            multiTrack.isSelectAgainToRoot = value
        }
    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()
    private val fluencyHelper: VideoFluencyHelper by lazy {
        VideoFluencyHelper()
    }
    private val videoFrameCache: VideoFrameCache by lazy {
        VideoFrameCache(
            coroutineContext, Size(TrackConfig.THUMB_WIDTH, TrackConfig.THUMB_HEIGHT)
        ) { executing ->
            launch(Dispatchers.Default) {
                fluencyHelper.setVEFrameTaskExecuting(executing)
            }
        }
    }

    private var isCoverMode = false

    fun addFrameRequest(request: FrameRequest) {
        videoFrameCache.addRequest(request)
    }

    fun refreshFrameCache(onlyRefreshFile: Boolean = false) {
        videoFrameCache.refresh(onlyRefreshFile)
    }

    fun getFrameBitmap(path: String, timestamp: Int): Bitmap? {
        return videoFrameCache.mainThreadGet(path, timestamp)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (isInEditMode) return
        LayoutInflater.from(context).inflate(layout.track_pannel_layout, this)
        TrackConfig.FRAME_DURATION = TrackConfig.DEFAULT_FRAME_DURATION
        scrollContainer.setTimelineScale(TrackConfig.PX_MS)
        val scaleGestureDetector = ScaleGestureDetector(scaleListener)
        scrollContainer.scaleGestureDetector = scaleGestureDetector
        videoTrackHolder = VideoTrackHolder(this, multiTrack)
        cover_group.visible = ThemeStore.trackUIConfig.showCoverSettingUI
        initSubTrack()
        initListener()
        initCustomUI()
        alignMainTrackControlUI()
    }

    private fun initCustomUI() {
        // 定制化添加素材按钮的icon
        ThemeStore.getCustomAddMediaIconRes()?.apply {
            ThemeStore.setCustomAddMediaIconRes(ivAdd)
        }
        //定制化「开启原声」、「关闭原声」的icon
        ThemeStore.getCustomEnableOriginalVoiceIcon()?.apply { enableOriginalVoiceIcon = this }
        ThemeStore.getCustomDisableOriginalVoiceIcon()?.apply { disableOriginalVoiceIcon = this }
    }

    /**
     * 调整maintrack_cl  view 始终对齐轨道中央，
     */
    private fun alignMainTrackControlUI() {
        maintrack_cl.post {
            val width = frameScroller.width
            if (width > 0) {
                val lp = (maintrack_cl.layoutParams as MarginLayoutParams)
                lp.marginStart = ((width * 0.5f) - maintrack_cl.width).toInt() - 16f.dp().toInt()
            }

        }
    }

    private val groupActionListener: TrackGroupActionListener = object : TrackGroupActionListener {
        override fun onSlotSelect(nleTrackSlot: NLETrackSlot?) {
            nleTrackSlot?.let {
                val trackBySlot =
                    nleModel?.getTrackBySlot(it) ?: nleModel?.getTrackByVideoEffect(it)
                videoTrackHolder?.selectSlot(-1, forceSelect = false, onlyRefreshUI = true)
                DLog.d("onSlotSelect", trackBySlot?.toJsonString() ?: "")
                trackPanelActionListener?.onSegmentSelect(trackBySlot, it)
                currentSelectSlot = it
            } ?: run {
                currentSelectSlot = null
                trackPanelActionListener?.onSegmentSelect(null, null)
            }
        }

        override fun onClip(slot: NLETrackSlot, start: Long, timelineOffset: Long, duration: Long) {
            trackPanelActionListener?.onClip(slot, start * 1000, duration * 1000)
        }
    }

    private fun initSubTrack() {
        val playController = object : PlayController {
            override fun pause() {}
        }
        val frameCallback = object : KeyframeStateDelegate {
            override fun getActiveKeyframe(): NLETrackSlot? {
                return trackPanelActionListener?.getSelectedKeyframe()
            }

            override fun shouldDrawIcon(): Boolean {
                return true
            }
        }
        multiTrackAdapter = MultiTrackAdapter(
            context as AppCompatActivity,
            this,
            subTrackGroup,
            scrollContainer,
            playController,
            frameCallback
        )
        subTrackGroup.trackGroupActionListener = groupActionListener
    }

    private val trackGroupCallback: TrackGroup.Callback by lazy {
        object : TrackGroup.Callback(scrollContainer) {
            override fun clipTo(px: Int) {
            }

            override fun onStartAdsorption(nleTrackSlot: NLETrackSlot) {
                val playHeadTimestamp =
                    ceil((subTrackGroup.scrollX / TrackConfig.PX_MS)).toLong()
                nleModel?.let {
                    TrackAdsorptionHelper.updateTimePoint(playHeadTimestamp, nleTrackSlot, it)
                }
            }

            override fun onCancelAdsorption() {
                TrackAdsorptionHelper.clearTimePoint()
            }

            override fun doAdsorptionOnClip(
                touchPositionInTime: Long,
                handlePositionInTime: Long
            ): Long? {
                return TrackAdsorptionHelper.doAdsorptionOnClip(
                    touchPositionInTime,
                    handlePositionInTime
                )
            }

            override fun doAdsorptionOnDrag(
                nleTrackSlot: NLETrackSlot,
                dragState: HorizontallyState,
                startTime: Long,
                endTime: Long,
                currentStartTime: Long,
                currentEndTime: Long
            ): Long {
                return TrackAdsorptionHelper.doAdsorptionOnDrag(
                    dragState,
                    startTime,
                    endTime,
                    currentStartTime,
                    currentEndTime
                )
            }

            override fun onSelectKeyframe(keyframe: NLETrackSlot?) {
                trackPanelActionListener?.onKeyframeSelected(keyframe)
            }

            override fun onDeselectKeyframe() {
                trackPanelActionListener?.onKeyframeSelected(null)
            }

            override fun onClickKeyframe(playHead: Long) {
                val state =
                    PlayPositionState(playHead * 1000, isSeek = false, seekSyncTrackScroll = false)
                updatePlayState(state, true)
            }

            override fun getMainVideoDuration(): Long {
                return nleModel?.maxTargetEnd ?: 3000L
            }
        }

    }

    private var isAllMute: Boolean = false
    private fun updateMuteState() {
        tvMute.text = if (isAllMute) {
            resources.getString(R.string.ck_unmute_original_audio)
        } else {
            resources.getString(R.string.ck_mute_original_audio)
        }
        val top =
            resources.getDrawable(if (isAllMute) disableOriginalVoiceIcon else enableOriginalVoiceIcon)
        tvMute.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null)
    }

    private fun initListener() {
        subTrackGroup.setCallback(trackGroupCallback)
        ivAdd.setOnClickListener {
            trackPanelActionListener?.onAddResourceClick()
        }

        cover_group.setOnClickListener {
            unSelectCurrentSlot()
            trackPanelActionListener?.onUpdateVideoCover()
        }

        scrollContainer.setOnBlankClickListener(OnClickListener {
            unSelectCurrentSlot()
        })

        val seekTo: (Int, Int) -> Unit = { scrollX, deltaX ->
            val now = System.currentTimeMillis()
            val deltaDuration = if (lastSeekTimeStamp == 0L) {
                1L
            } else {
                now - lastSeekTimeStamp
            }
            val seekPxSpeed = deltaX.toFloat() / deltaDuration
            val seekDurationSpeed = deltaX.toFloat() / TrackConfig.PX_MS / deltaDuration
            timestamp = ceil(scrollX / TrackConfig.PX_MS.toDouble()).toLong()
            lastSeekTimeStamp = System.currentTimeMillis()
            trackPanelActionListener?.onVideoPositionChanged(
                SeekInfo(
                    timestamp * 1000,
                    seekPxSpeed = seekPxSpeed,
                    seekDurationSpeed = seekDurationSpeed
                )
            )
            unselectedSlotIfNeeded()
        }
        frameScroller.setScrollChangeListener(seekTo)
        frameScroller.setMustUpdateScrollXListener {
            videoTrackHolder?.updateScrollX(it, onlyRefreshFile = true)
        }

        val seekOptimization: (scrollX: Int) -> Unit = {
            val position = (it / TrackConfig.PX_MS).toLong()
            trackPanelActionListener?.onVideoPositionChanged(
                SeekInfo(
                    position * 1000
                )
            )
        }
        scrollContainer.setOnScrollStateChangeListener(object : OnScrollStateChangeListener {
            override fun onScrollStateChanged(state: ScrollState, scrollX: Int, scrollY: Int) {
                if (state == ScrollState.IDLE) {
                    seekOptimization(scrollX)
                    // when scroll IDLE, must refresh
                    videoTrackHolder?.updateScrollX(scrollX, mustRefresh = true)
                } else if (state == ScrollState.DRAGGING) {
                    videoTrackHolder?.updateScrollX(scrollX, onlyRefreshFile = true)
                }
            }
        })

        tvMute.setOnClickListener {
            isAllMute = !isAllMute
            trackPanelActionListener?.onAudioMuteClick(isAllMute, true)
        }

        multiTrack.setMultiTrackListener(object : MultiTrackLayout.MultiTrackListener {
            override fun onStartAndDuration(
                slot: NLETrackSlot,
                start: Int,
                duration: Int,
                side: Int
            ) {
                // move this code to tob activity
//                com.ss.ugc.android.editbase.logger.TrackLog.d("clip", "onStartAndDuration $start $duration $side")
                trackPanelActionListener?.onStartAndDuration(slot, start, duration, side)
            }

            override fun onTransitionClick(slot: NLETrackSlot, nextSlot: NLETrackSlot) {
                trackPanelActionListener?.onTransitionClick(slot, nextSlot)
            }

            override fun onDragStart() {
            }

            override fun onDragComplete(
                slot: NLETrackSlot,
                fromIndex: Int,
                targetIndex: Int
            ) {
                if (fromIndex == targetIndex) {
                    // 这种情况其实是没有真正修改
//                    multiTrack.requestLayout()
                    nleModel?.let {
                        updateNLEModel(it, true)
                    }
                } else {
                    unSelectCurrentSlot()
                    trackPanelActionListener?.onMainTrackMoveSlot(slot, fromIndex, targetIndex)
                }
            }

            override fun shouldDrawIcon(): Boolean {
                return true
            }

            override fun onDeselectSegment() {
            }

            override fun getParentScrollX(): Int {
                return frameScroller.scrollX
            }

            override fun onSegmentClick(slot: NLETrackSlot?) {
                subTrackGroup.resetSelected()
                currentSelectSlot = null
                trackPanelActionListener?.onSegmentSelect(
                    if (slot == null) null else nleMainTrack,
                    slot
                )
            }

            override fun onKeyFrameSelect(frame: NLETrackSlot?) {
                trackPanelActionListener?.onKeyframeSelected(frame)
            }

            override fun onKeyFrameClick(playHead: Long) {
                val state =
                    PlayPositionState(playHead * 1000, isSeek = false, seekSyncTrackScroll = false)
                updatePlayState(state, true)
            }

            override fun onKeyFrameDeselect() {
                trackPanelActionListener?.onKeyframeSelected(null)
            }

            override fun getCurrentKeyframe(): NLETrackSlot? {
                return trackPanelActionListener?.getSelectedKeyframe()
            }

            override fun scrollBy(
                x: Int,
                y: Int,
                invokeChangeListener: Boolean,
                disablePruneX: Boolean,
                disablePruneY: Boolean
            ) {
                scrollContainer.scrollBy(
                    x,
                    y,
                    invokeChangeListener,
                    disablePruneX,
                    disablePruneY
                )
            }

            override fun smoothScrollHorizontallyBy(x: Int, invokeChangeListener: Boolean) {
                scrollContainer.smoothScrollHorizontallyBy(x, invokeChangeListener)
            }

            override fun assignMaxScrollX(maxScrollX: Int) {}
        })
    }

    override fun selectCurrentSlot() {
        var selectIndex = -1
        nleMainTrack?.sortedSlots?.forEachIndexed { index, nleTrackSlot ->
            run {
                if (timestamp in nleTrackSlot.startTime / 1000..nleTrackSlot.measuredEndTime / 1000) {
                    selectIndex = index
                    return@forEachIndexed
                }
            }
        }
        runOnUiThread {
            videoTrackHolder?.selectSlot(
                selectIndex,
                forceSelect = true
            )
        }
    }

    private val subTrackList: ArrayList<TrackInfo> = arrayListOf()
    private val stickerTrackList: ArrayList<TrackInfo> = arrayListOf()
    private val filterTrackList: ArrayList<TrackInfo> = arrayListOf()
    private val adjustTrackList: ArrayList<TrackInfo> = arrayListOf()
    private val textTrackList: ArrayList<TrackInfo> = arrayListOf()
    private val videoEffectList: ArrayList<TrackInfo> = arrayListOf()
    private val audioTrackList: ArrayList<TrackInfo> = arrayListOf()
    private val trackList: ArrayList<TrackInfo> = arrayListOf()

    private var isFirst = true

    private fun updateNLEModel(nleModel: NLEModel, forceRefresh: Boolean = false) {
        this.nleModel = nleModel
        DataHolder.nleModel = nleModel
        if (nleModel.tracks.size <= 0) {
            return
        }
        this.nleMainTrack = nleModel.tracks.filter { it.mainTrack }[0] //从 nleModel中取得 主轨 mainTrack
        filterTracks(nleModel)
//        ifCanLog { Logger.d(TAG, "update-track: ${nleModel.toJsonString()}") }//日志耗时50ms，慎用

        //根据slot中的enableAudio 来展示是关闭原声/开启原声
        nleMainTrack?.slots?.filter {
            it.getMainSegment<NLESegmentVideo>()?.enableAudio ?: true
        }?.apply {
            isAllMute = this.isEmpty()
            // 更新关闭原声/开启原声图标
            updateMuteState()
            trackPanelActionListener?.onAudioMuteClick(isAllMute, false)
        }

        nleMainTrack?.let {
            runOnUiThread {
                try {
                    videoTrackHolder?.reloadVideoTrack(it, forceRefresh)
                    timeRuler.durationTime = nleModel.maxTargetEnd / 1000
                    if (isCoverMode) {
                        nleMainTrack?.let {
                            scrollContainer.updateTotalDuration(it.maxEnd / 1000)
                        }
                    } else {
                        scrollContainer.updateTotalDuration(nleModel.maxTargetEnd / 1000)
                    }
                    when (currentUIState) {
                        NORMAL -> {
                            if (isCoverMode) {
                                initOriginUIForCoverMode()
                            } else {
                                adjustAudioLayer()
                                trackList.addAll(subTrackList)
                                trackList.addAll(audioTrackList)
                                if (trackList.isEmpty()) {
                                    changeTopMargin(59f)
                                } else {
                                    changeTopMargin(14f)
                                }
                            }
                        }

                        PIP -> {
                            trackList.addAll(subTrackList)
                        }

                        AUDIO -> {
                            trackList.addAll(audioTrackList)
                        }

                        TrackState.AUDIORECORD -> {
                            trackList.addAll(audioTrackList)
                        }

                        TEXT, STICKER -> {
                            trackList.addAll(stickerTrackList)
                            trackList.addAll(textTrackList)
                        }

                        TrackState.VIDEOEFFECT -> {
                            trackList.addAll(videoEffectList)
                        }

                        TrackState.ADJUST -> {
                            trackList.addAll(adjustTrackList)
                        }

                        TrackState.FILTER -> {
                            trackList.addAll(filterTrackList)
                        }
                    }

                    multiTrackAdapter.setTrackList(trackList)
                    currentSelectSlot?.let {
                        val trackBySlot =
                            nleModel.getTrackBySlot(it) ?: nleModel.getTrackByVideoEffect(it)
                        if (trackBySlot != null) {
                            selectSlot(it, false)
                        } else {  //若一个slot被移除，就会触发
                            //Toaster.show("slot delete")
                            // 选中的slot 被删除了
                            subTrackGroup.resetSelected()
                            currentSelectSlot = null
                            // 通知上层被删除了
                            trackPanelActionListener?.onSegmentSelect(null, null)
                        }
                    } ?: subTrackGroup.resetSelected()

                    if (timestamp > nleModel.maxTargetEnd / 1000) {
                        updatePlayState(PlayPositionState(nleModel.maxTargetEnd))
                    }
                    // 用一种 trick的方式修复一个副轨缩略图不显示的问题
                    if (isFirst) {
                        if (trackList.isNotEmpty()) {
                            runOnUiThread(500) {
                                updatePlayState(PlayPositionState(1000000))
                                updatePlayState(PlayPositionState(0))
                            }
                        }
                        isFirst = false
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "update model failed catch exception $e")
                    e.printStackTrace()
                }
            }
        }
    }

    fun compressSubTrack(compress: Boolean) {
        if (compress) {
            changeTopMargin(14f)
            ConstraintSet().apply {
                clone(scrollContainer)
                clear(R.id.subTrackGroup, ConstraintSet.BOTTOM)
                constrainHeight(R.id.subTrackGroup, SizeUtil.dp2px(41F))
            }.applyTo(scrollContainer)
        } else {
            ConstraintSet().apply {
                clone(scrollContainer)
                subTrackGroup.scrollY = min(subTrackGroup.scrollY, subTrackGroup.maxScrollY)
                constrainHeight(R.id.subTrackGroup, SizeUtil.dp2px(0F))
                connect(
                    R.id.subTrackGroup,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
            }.applyTo(scrollContainer)
        }
    }

    private fun switchRecordUI() {
        changeTopMargin(14f)
        ConstraintSet().apply {
            clone(scrollContainer)
            clear(R.id.subTrackGroup, ConstraintSet.BOTTOM)
            constrainHeight(R.id.subTrackGroup, SizeUtil.dp2px(41F))

        }.applyTo(scrollContainer)
    }

    private fun adjustAudioLayer() {
        if (subTrackList.isEmpty() || audioTrackList.isEmpty()) {
            return
        }
        val baseLayer = subTrackList.findLast { it.slotList.isNotEmpty() }?.layer ?: return
        //判断画中画轨道是否有跟音轨重叠，若重叠则在音频轨添加空白轨道规避开
        if (audioTrackList.firstOrNull { it.slotList.isNotEmpty() }?.layer ?: 0 <= baseLayer) {
            val map = ArrayList<TrackInfo>()
            //添加空白轨道规避开
            for (i in 0..baseLayer) {
                map.add(TrackInfo(i, NLETrackType.AUDIO, arrayListOf()))
            }
            //原音轨layer往下顺移(空白轨道自动清理避免列表一直增大)
            audioTrackList.filter { it.slotList.isNotEmpty() }.forEachIndexed { index, trackInfo ->
                map.add(
                    TrackInfo(
                        index + baseLayer + 1,
                        trackInfo.trackType,
                        trackInfo.slotList
                    )
                )
            }
            audioTrackList.clear()
            audioTrackList.addAll(map)
        }
    }

    private fun filterTracks(nleModel: NLEModel) {
        subTrackList.clear()
        stickerTrackList.clear()
        filterTrackList.clear()
        adjustTrackList.clear()
        audioTrackList.clear()
        textTrackList.clear()
        videoEffectList.clear()
        // 有特效
        val videoEffectMap = mutableMapOf<Int, ArrayList<NLETrackSlot>>()
        /**
         *  把 tracks 中的所有轨道 进行分类
         */
        nleModel.tracks.forEach {
            when (it.extraTrackType) {
                NLETrackType.STICKER -> {
                    stickerTrackList.add(TrackInfo(it.layer, NLETrackType.STICKER, it.sortedSlots))
                }
                VIDEO -> {
                    if (!it.mainTrack) {
                        subTrackList.add(TrackInfo(it.layer, NLETrackType.VIDEO, it.sortedSlots))
                    }
                    if (it.videoEffects.isNotEmpty()) {

                        it.videoEffects.forEach { slot ->
                            if (videoEffectMap[slot.layer]?.isNotEmpty() == true) videoEffectMap[slot.layer]?.add(
                                slot
                            ) else videoEffectMap[slot.layer] =
                                arrayListOf<NLETrackSlot>().apply { add(slot) }
                        }
                    }
                }
                NLETrackType.AUDIO -> {
                    audioTrackList.add(TrackInfo(it.layer, NLETrackType.AUDIO, it.sortedSlots))
                }

                EFFECT -> {
                    it.slots.forEach { slot ->
                        if (videoEffectMap[slot.layer]?.isNotEmpty() == true) videoEffectMap[slot.layer]?.add(
                            slot
                        ) else videoEffectMap[slot.layer] =
                            arrayListOf<NLETrackSlot>().apply { add(slot) }
                    }
                }

                NLETrackType.FILTER -> {
                    if (it.slots.size > 0) {
                        it.slots.first().takeIf { it.filters.isNotEmpty() }?.let { slot ->
                            val isAdjust =
                                slot.filters[0].segment.effectSDKFilter.resourceType == NLEResType.ADJUST
                            if (isAdjust) {
                                adjustTrackList.add(
                                    TrackInfo(
                                        it.layer,
                                        NLETrackType.FILTER,
                                        it.sortedSlots
                                    )
                                )
                            } else {
                                filterTrackList.add(
                                    TrackInfo(
                                        it.layer,
                                        NLETrackType.FILTER,
                                        it.sortedSlots
                                    )
                                )
                            }
                        }
                    }
                }

                else -> {
                    Logger.d(TAG, "found unknown track type ${it.trackType}")
                }
            }
        }

        // video特效 map 转化成 video 特效 list
        videoEffectMap.forEach { (index, slots) ->
            videoEffectList.add(
                TrackInfo(
                    index,
                    NLETrackType.EFFECT,
                    slots
                )
            )
        }

        // sort by layer
        subTrackList.sortBy { it.layer }
        stickerTrackList.sortBy { it.layer }
        adjustTrackList.sortBy { it.layer }
        filterTrackList.sortBy { it.layer }
        audioTrackList.sortBy { it.layer }
        textTrackList.sortBy { it.layer }
        videoEffectList.sortBy { it.layer }
        trackList.clear()
    }

    private fun changeTopMargin(top: Float) {
        val layoutParams =
            frameScroller.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.topMargin = SizeUtil.dp2px(top)
        frameScroller.layoutParams = layoutParams
    }

    override fun unSelectCurrentSlot() {
//        com.ss.ugc.android.editbase.logger.TrackLog.d(TAG, "unSelectCurrentSlot")
        unSelectCurrentSlot(false)
    }

    private fun unSelectCurrentSlot(onlyRefreshUI: Boolean) {
        videoTrackHolder?.selectSlot(-1, false, onlyRefreshUI)
        subTrackGroup.resetSelected()
        currentSelectSlot = null
    }

    override fun unSelectTransition() {
        multiTrack.setTransitionUnselected()
    }

    override fun getCurrentSlotInfo(): CurrentSlotInfo {
        var selectIndex = -1
        var slot: NLETrackSlot? = null
        nleMainTrack?.sortedSlots?.forEachIndexed { index, nleTrackSlot ->
            run {
                if (timestamp in nleTrackSlot.startTime / 1000..nleTrackSlot.measuredEndTime / 1000) {
                    selectIndex = index
                    slot = nleTrackSlot
                    return@forEachIndexed
                }
            }
        }
        return CurrentSlotInfo(selectIndex, slot, timestamp * 1000)
    }

    override fun switchUIState(state: TrackState) {
        if (currentUIState == state) {
            return
        }
        if (currentUIState == AUDIORECORD && state == AUDIO) {
            // 需要恢复一下
            // 恢复一下滚动距离
            ConstraintSet().apply {
                clone(scrollContainer)
                subTrackGroup.scrollY = min(subTrackGroup.scrollY, subTrackGroup.maxScrollY)
                constrainHeight(R.id.subTrackGroup, SizeUtil.dp2px(0F))
                connect(
                    R.id.subTrackGroup,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
            }.applyTo(scrollContainer)
            this.currentUIState = AUDIO
            return
        }
        this.currentUIState = state
        when (currentUIState) {
            AUDIO -> {
                changeTopMargin(14f)
                // 恢复 trackGroup
            }
            PIP, TEXT, STICKER, VIDEOEFFECT, ADJUST, FILTER -> {
                changeTopMargin(14f)
            }
            AUDIORECORD -> {
                switchRecordUI()
                trackList.addAll(audioTrackList)
            }
        }

        nleModel?.let {
            updateNLEModel(it)
        }
    }

    private var currentSelectSlot: NLETrackSlot? = null
    fun currentSelectTrack() = currentSelectSlot?.track(nleModel)
    override fun selectSlot(nleTrackSlot: NLETrackSlot) {
        selectSlot(nleTrackSlot, true)
    }

    override fun adjustAnimation(animation: NLEVideoAnimation?) {
        multiTrack.setVideoAnimChange(multiTrack.selectIndex, animation)
    }

    override fun onUndoRedo(operationType: Int, operationResult: Boolean) {
        if (operationResult) {
            // 用户undo redo 后判断
            currentSelectSlot?.let {
                getTrackBySlot(it) ?: kotlin.run {
                    trackPanelActionListener?.onSegmentSelect(
                        null,
                        null
                    )
                    currentSelectSlot = null
                }
            }
        }
    }

    var wavePoints: MutableList<Float>? = null
    override fun updateRecordWavePoint(recordWavePoints: MutableList<Float>) {
        wavePoints = recordWavePoints

        wavePoints?.let {
            multiTrackAdapter.updateRecordWavePoints(it)
            subTrackGroup.postInvalidate()
        }
    }

    override fun startRecordAudio(position: Long, recordLayer: Int) {
        DLog.d("audio-record", "startRecordAudio $position")
        this.isRecording = true
        this.recordPosition = position
        val recordSegmentCount = nleModel!!.getRecordSegmentCount() + 1
        multiTrackAdapter.startRecord(isRecording, recordPosition, recordLayer, recordSegmentCount)
        switchUIState(AUDIORECORD)
    }

    override fun stopRecordAudio() {
        this.isRecording = false
        multiTrackAdapter.audioTrackAdapter.isRecord = false
    }

    override fun getKeyframeTimeRange(): Float =
        (TrackConfig.KEYFRAME_ICON_WIDTH / TrackConfig.PX_MS) * 1000

    private fun getTrackBySlot(slot: NLETrackSlot): NLETrack? {
        nleModel?.getTrackBySlot(slot)?.apply {
            return this
        }
        if (slot.isEffectSlot()) {
            return nleModel?.getTrackByVideoEffect(slot)
        }

        return null
    }

    private fun selectSlot(nleTrackSlot: NLETrackSlot, isFromUser: Boolean) {
        runOnUiThread {
            Logger.d(TAG, "selectSlot $nleTrackSlot")
            currentSelectSlot = nleTrackSlot
            nleModel?.let {
                val trackBySlot =
                    it.getTrackBySlot(nleTrackSlot) ?: it.getTrackByVideoEffect(nleTrackSlot)
                trackBySlot ?: return@runOnUiThread
                var selectSlot: NLETrackSlot? = null

                trackBySlot.slots?.forEach { slot ->
                    if (slot.id == nleTrackSlot.id) {
                        selectSlot = slot
                    }
                }
                if (selectSlot == null) {
                    trackBySlot.videoEffects.forEach { slot ->
                        if (slot.id == nleTrackSlot.id) {
                            selectSlot = slot
                        }
                    }
                }
//                nleTrackSlot.isEffectSlot()
                when (trackBySlot.trackType) {
                    NLETrackType.STICKER, NLETrackType.AUDIO, EFFECT, NLETrackType.FILTER -> {
                        subTrackGroup.selectSegment(nleTrackSlot, isFromUser)
                    }
                    VIDEO -> {
                        if (trackBySlot.mainTrack && !nleTrackSlot.isEffectSlot()) {
                            videoTrackHolder?.selectSlot(
                                trackBySlot.getSlotIndex(nleTrackSlot),
                                isFromUser
                            )
                        } else {
                            subTrackGroup.selectSegment(nleTrackSlot, isFromUser)
                        }
                    }
                    else -> {
                    }
                }
                if (isFromUser) {
                    trackPanelActionListener?.onSegmentSelect(trackBySlot, selectSlot)
                }
            }
        }
    }

    override fun updateNLEModel(nleModel: NLEModel) {
        val startTime = System.currentTimeMillis()
        updateNLEModel(nleModel, false)
        val endTime = System.currentTimeMillis()
        Log.d(TAG, "trackPanel updateNLEModel timeCost total = ${(endTime - startTime) * 1F} ms")
    }

    fun updatePlayState(playState: PlayPositionState) {
        updatePlayState(playState, false)
    }

    override fun updatePlayState(playState: PlayPositionState, isFromUser: Boolean) {
        runOnUiThread {
            DLog.d(TAG, "updatePlayState $playState ")
            val position = playState.position / 1000
            if (frameScroller.scrollX != (position * TrackConfig.PX_MS).toInt()) {
                scrollContainer.scrollToHorizontally((position * TrackConfig.PX_MS).toInt())
            }
            videoTrackHolder?.updateScrollX(frameScroller.scrollX)
            timestamp = position
            trackPanelActionListener?.onVideoPositionChanged(
                SeekInfo(
                    timestamp * 1000,
                    isFromUser = isFromUser
                )
            )
            unselectedSlotIfNeeded()

        }
    }

    private fun unselectedSlotIfNeeded() {
        val selectIndex = multiTrack.selectIndex
        if (selectIndex != -1 && selectIndex in 0 until (nleMainTrack?.sortedSlots?.size ?: 0)) {
            val nleTrackSlot = nleMainTrack!!.sortedSlots[selectIndex]
            val endTime = (nleTrackSlot.measuredEndTime / 1000f).roundToInt()
            if (timestamp != 0L && timestamp !in nleTrackSlot.startTime / 1000..endTime) {
                ifCanLog { DLog.d("unselectedSlotIfNeeded $timestamp ${nleTrackSlot.startTime / 1000} $endTime ") }
                if (enableAutoSelectNext) {

                    val nextSlotIndex = getCurrentIndex(nleTrackSlot, selectIndex)
                    if (nextSlotIndex in 0 until nleMainTrack!!.sortedSlots.size) {
                        val nextNleTrackSlot = nleMainTrack!!.sortedSlots[nextSlotIndex]
                        selectSlot(nextNleTrackSlot, isFromUser = false)
                    }
                } else {
                    unSelectCurrentSlot(trackPanelActionListener?.ignoreSelectSlotOnPlay() ?: false)
                }
            }
        }
    }

    private fun getCurrentIndex(nleTrackSlot: NLETrackSlot, selectIndex: Int): Int {
        val next = if (timestamp < nleTrackSlot.startTime / 1000) {
            selectIndex - 1
        } else {
            (selectIndex + 1) % nleMainTrack!!.slots.size
        }
        if (next in 0 until nleMainTrack!!.slots.size) {
            val nextSlot = nleMainTrack!!.sortedSlots[next]
            //if time no in next slot,find correct one
            if (timestamp * 1000 !in nextSlot.startTime..nextSlot.endTime) {
                nleMainTrack?.sortedSlots?.forEachIndexed { index, slot ->
                    if (timestamp * 1000 in slot.startTime..slot.endTime) {
                        return index
                    }
                }
            }
        }
        return next
    }

    private fun updateScale(factor: Float) {
        runOnUiThread {
            scale *= factor
            if (scale <= 0.1) {
                scale = 0.1
            }
            if (scale >= 10) {
                scale = 10.0
            }
            TrackConfig.FRAME_DURATION = (TrackConfig.DEFAULT_FRAME_DURATION / scale).toInt()
            videoTrackHolder?.setScaleSize(scale)
            multiTrack.resizeAdjustLayout()
            timeRuler.requestLayout()
            val playPosition = timestamp
            if (scrollContainer.scrollX != (playPosition * TrackConfig.PX_MS).toInt()) {
                scrollContainer.scrollToHorizontally((playPosition * TrackConfig.PX_MS).toInt())
            }
            videoTrackHolder?.updateScrollX(frameScroller.scrollX, mustRefresh = true)
            multiTrack.requestLayout()
            scrollContainer.setTimelineScale(TrackConfig.PX_MS)
        }
    }
    private var retryCoverCount = 0

    fun initVideoCover(nlePlayer: NLEPlayer) {
        nleModel?.let {
            if (it.cover?.snapshot?.resourceFile.isNullOrBlank() || !FileUtil.isFileExist(it.cover?.snapshot?.resourceFile)) {
                DLog.d("cover trace ", "try get bitmap ")
                launch(Dispatchers.IO) {
                    val bitmap = nlePlayer.getCurrentDisplayImage()
                    if (bitmap != null) {
                        runOnUiThread {
                            trackPanelActionListener?.onSaveSnapShot(bitmap, true)
                        }
                    } else if (retryCoverCount < 5) {
                        runOnUiThread(100) {
                            DLog.w("cover trace ", "retry get bitmap $retryCoverCount")
                            retryCoverCount++
                            if (retryCoverCount < 5) {
                                initVideoCover(nlePlayer)
                            }
                        }
                    }
                }
            } else {
                DLog.d("cover trace ", "load bitmap ${it.cover.snapshot.resourceFile} ")
                ImageLoader.loadBitmap(
                    context,
                    it.cover.snapshot.resourceFile,
                    cover_image,
                    ImageOption.Builder().scaleType(ImageView.ScaleType.CENTER_CROP).build()
                )
            }
        }
    }

    fun updateVideoCover(
        nlePlayer: NLEPlayer,
        refreshOnly: Boolean = false,
        tempBitmap: Bitmap? = null
    ) {
        launch {
            val bitmap: Bitmap? = tempBitmap ?: withContext(Dispatchers.IO) { nlePlayer.getCurrentDisplayImage() }
            bitmap?.let {
                trackPanelActionListener?.onSaveSnapShot(bitmap, false, refreshOnly)
            }
        }
    }

    fun getDraftCover(nlePlayer: NLEPlayer, coverFile: File) {
        launch(Dispatchers.IO) {
            val bitmap = nlePlayer.getCurrentDisplayImage()
            bitmap?.let {
                BitmapUtil.compressBitmap(bitmap, coverFile, 300F)
            }
        }
    }

    fun setCoverImage(bitmap: Bitmap) {
        ImageLoader.loadImageBitmap(
            context,
            bitmap,
            cover_image,
            ImageOption.Builder().scaleType(ImageView.ScaleType.CENTER_CROP).build()
        )
    }

    private fun initOriginUIForCoverMode() {
        scrollContainer.setBackgroundColor(Color.TRANSPARENT)
        val layoutParams = this.frameScroller.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.topMargin = SizeUtil.dp2px(20f)
        frameScroller.layoutParams = layoutParams
        timeRulerScroller.visibility = View.GONE
        cover_group.visibility = View.INVISIBLE
        ivAdd.visibility = View.GONE
        tvMute.visibility = View.INVISIBLE
        iv_time.visibility = View.INVISIBLE
        cover_seek_signal.visibility = View.VISIBLE
        cover_text.visibility = View.VISIBLE
        alignMainTrackControlUI()
    }

    fun setIsCoverMode(isCoverMode: Boolean) {
        this.isCoverMode = isCoverMode
        multiTrack.setIsCoverMode(isCoverMode = isCoverMode)
    }

    /**
     *  隐藏 TrackPanel
     * */
    fun hide() {
        this.visibility = View.GONE
    }

    /**
     * 隐藏TrackPanel后，重新展示TrackPanel
     */
    fun show() {
        this.visibility = View.VISIBLE
    }
}



