package com.ss.ugc.android.editor.track

import android.graphics.Bitmap
import com.bytedance.ies.nle.editor_jni.NLEModel
import com.bytedance.ies.nle.editor_jni.NLETrack
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.bytedance.ies.nle.editor_jni.NLEVideoAnimation

interface ITrackPanel {


    /**
     * 更新model nle 数据更新后，通过这个接口来更新track视图
     */
    fun updateNLEModel(nleModel: NLEModel)

    /**
     * preview在播放的时候需要更新track的位置，调用这里即可
     */
    fun updatePlayState(playState: PlayPositionState, isFromUser: Boolean = false)



    /**
     * 选中当前片断，进行剪辑相关操作
     * 目前只会选中主轨
     */
    fun selectCurrentSlot()


    /**
     * 取消当前片断的选择中态
     */
    fun unSelectCurrentSlot()

    /**
     * 取消转场按钮的选中态
     */
    fun unSelectTransition()

    /**
     * 获取当前轨道上滑动到的片断的信息
     */
    fun getCurrentSlotInfo(): CurrentSlotInfo


    /**
     * 切换轨道视图
     * [state] [TrackState.STICKER] 贴纸  [TrackState.NORMAL] 普通模式
     */
    fun switchUIState(state: TrackState)

    /**
     * 选中某个slot
     * 比如在添加画中画或者文本后主动调用此方法选中添加的slot
     *
     */
    fun selectSlot(nleTrackSlot: NLETrackSlot)

    /**
     * 设置正在调节的动画数据 更新进度
     * 为null 的时候说明调节完成
     */
    fun adjustAnimation(animation: NLEVideoAnimation?)

    /**
     * 用户操作undo redo 完成后，调用该方法通知trackPanel
     * [operationType] 0 :undo ,1: redo
     * [operationResult] undo redo 的结果
     */
    fun onUndoRedo(operationType: Int, operationResult: Boolean)


    /**
     * 录音的时候需要更新多轨道面板
     */
    fun updateRecordWavePoint(recordWavePoints: MutableList<Float>)

    fun startRecordAudio(position: Long, recordLayer: Int)


    fun stopRecordAudio()

    //返回trackPanel中 keyframe关键帧icon对应的时间长度 ，时间单位为微秒us
    fun getKeyframeTimeRange(): Float
}

/**
 * 轨道视图状态定义
 * NORMAL 普通状态显示视频主轨和画中画
 * STICKER 显示贴纸轨道
 * TEXT    显示文本轨道
 * PIP     画中画
 * AUDIO   音频
 * VIDEOEFFECT 视频特效
 * AUDIORECORD 音频录音
 *
 */
enum class TrackState {
    NORMAL, STICKER, TEXT, PIP, AUDIO, VIDEOEFFECT, AUDIORECORD , ADJUST, FILTER
}


interface TrackPanelActionListener {

    /**
     * 视频进度更新
     *
     * [seekInfo] 视频seek 进度信息
     */
    fun onVideoPositionChanged(seekInfo: SeekInfo)

    /**
     * 轨道上选中了某类slot
     */
    fun onSegmentSelect(nleTrack: NLETrack?, nleTrackSlot: NLETrackSlot?)

    /**
     * 点击了两个slot之间的转场
     */
    fun onTransitionClick(segment: NLETrackSlot, nextSegment: NLETrackSlot)


    /**
     * 用户点击添加素材
     */
    fun onAddResourceClick()

    /**
     *   轨道开始缩放回调
     */
    fun onScaleBegin()

    /**
     *  缩放结束
     */
    fun onScaleEnd()


    /**
     * 轨道缩放过程回调
     */
    fun onScale(scaleRatio: Float)


    /**
     * 调整某一个slot的时间的回调
     * [slot] 在调整的slot
     * [startDiff]  单位us  调整后的start diff 时间，50 表示开始时间相对之前增加了50us
     * [duration] 单位us 调整后整个slot 的时间
     * 假如之前slot 的开始时间是start,那么调整后是
     * 开始时间：newStart =  start+startDiff
     * 结束时间：end =  newStart + duration
     */
    fun onClip(
        slot: NLETrackSlot,
        startDiff: Long,
        duration: Long
    )

    /**
     * 移动某一个slot 回调，比如画中画，音轨
     * [fromTrackIndex]  移动在面板上纵向的位置
     * [toTrackIndex]    移动后在面板上纵向的位置
     * [slot]            正在移动的slot
     * [newStart]        slot 移动后的starTime   单位us
     * [currPosition]    当前播放的进度           单位us
     */
    fun onMove(
        fromTrackIndex: Int,
        toTrackIndex: Int,
        slot: NLETrackSlot,
        newStart: Long,
        currPosition: Long
    )

    /**
     * 主轨长按移动视频片断
     * [nleTrackSlot] 移动的片断
     * [fromIndex]    移动前的index
     * [toIndex]      移动后的index
     */
    fun onMainTrackMoveSlot(nleTrackSlot: NLETrackSlot, fromIndex: Int, toIndex: Int)

    /**
     * 主轨上裁剪视频的长度
     * [slot] 正在裁剪操作的slot
     * [start] 裁剪后的start time  ms
     * [duration] 裁剪后的长度时间  ms
     * [side] 左边裁剪还是右边裁剪
     */
    fun onStartAndDuration(slot: NLETrackSlot, start: Int, duration: Int, side: Int)

    /**
     * 用户点击设置视频封面
     */
    fun onUpdateVideoCover()

    /**
     * 关闭原声/开启原声 被点击
     */
    fun onAudioMuteClick(isAllMute: Boolean , needUpdate: Boolean)

    /**
     * 更新保存在nlemodel里的视频封面图片
     */
    fun onSaveSnapShot(bitmap: Bitmap, isInit: Boolean, refreshOnly: Boolean = false)

    /**
     * 当滑到keyframe时，或滑出keyframe时回调
     * 滑出时keyframeId为null
     */
    fun onKeyframeSelected(keyframe: NLETrackSlot?)

    /**
     * 获取当前选中的keyframe
     */
    fun getSelectedKeyframe(): NLETrackSlot?

    /**
     * 是否需要忽略播放时自动点击下一个slot
     */
    fun ignoreSelectSlotOnPlay(): Boolean
}

/**
 *  播放状状态的定义
 */
data class PlayPositionState(
    val position: Long,   // 时间戳 单位us
    val isSeek: Boolean = false,
    val seekSyncTrackScroll: Boolean = false
)

data class SeekInfo(
    val position: Long,  // 时间戳 单位us
    val autoPlay: Boolean = false, // seek 后是否要自动播放
    val seekFlag: Int = Seek.SEEK_FLAG_ONGOING, // seek 类型
    val seekPxSpeed: Float = 0F, // seek的速度
    val seekDurationSpeed: Float = 0F, // 整个滑动的时间长度
    val isFromUser: Boolean = true // 是否有用户触发
)


object Seek {
    /**
     * on going seek
     */
    const val SEEK_FLAG_ONGOING = 0

    /**
     * the last seek
     */
    const val SEEK_FLAG_LAST_SEEK = 1

    /**
     * seek to nearest I frame
     */
    const val SEEK_FLAG_TO_I_FRAME = 2

    /**
     * the last seek, and update In point
     */
    const val SEEK_FLAG_LAST_UPDATE_IN = 3

    /**
     * the last seek, and update Out point
     */
    const val SEEK_FLAG_LAST_UPDATE_OUT = 4

    /**
     * the last seek, and update In/Out point,
     * In point will be set as seek time
     * Out point will be set as seek time + (orignal outpoint - inpoint),
     * which means the duration is not changed
     */
    const val SEEK_FLAG_LAST_UPDATE_IN_OUT = 5

    const val SEEK_FLAG_FLUSH_CMD = 6

    const val SEEK_FLAG_LAST_ACCURATE_CLEAR = 7
}


data class SelectSlotInfo(val trackId: Long, val slotId: Long, val slotIndex: Int)

/**
 * 获取当前时间轴滑到区域slot 的信息
 * [index]        当前片断的index
 * [slot] 当前片断信息
 * [playTime] 当前视频播放到的时间 单位us
 */
data class CurrentSlotInfo(val index: Int, val slot: NLETrackSlot?, val playTime: Long)