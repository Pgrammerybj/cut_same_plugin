package com.ss.ugc.android.editor.core.api.impl

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bytedance.ies.nle.editor_jni.*
import com.bytedance.ies.nle.editor_jni.NLESeekFlag.EDITOR_SEEK_FLAG_LastSeek
import com.bytedance.ies.nle.mediapublic.NLEPlayerPublic
import com.ss.android.vesdk.*
import com.ss.ugc.android.editor.core.*
import com.ss.ugc.android.editor.core.Constants.Companion.STATE_PAUSE
import com.ss.ugc.android.editor.core.Constants.Companion.STATE_PLAY
import com.ss.ugc.android.editor.core.Constants.Companion.STATE_SEEK
import com.ss.ugc.android.editor.core.api.IPlayer
import com.ss.ugc.android.editor.core.api.VariableKeys
import com.ss.ugc.android.editor.core.utils.LiveDataBus

class DefaultPlayer(val editorContext: IEditorContext) : IPlayer {
    companion object {
        const val TAG = "DefaultPlayer"
    }

    @Volatile
    private var playAfterSeekDone: Boolean = false

    @Volatile
    private var currentSeekId: Int? = null
    private val _curPlayPosition = MutableLiveData<Long>()
    private val _playEndEvent = MutableLiveData<Unit>()
    override val curPlayPosition: LiveData<Long> = _curPlayPosition
    override val videoPlayEndEvent: LiveData<Unit> = _playEndEvent

    override var isPlaying: Boolean = false

    private val statusListenerLock = Any()
    private val playHandler = Handler(Looper.getMainLooper())
    private lateinit var nlePlayer: INLEPlayer
    override var isActive: Boolean = true
    private var isRangePlay = false
    private var rangeEndTime = 0L
    private var rangeStartTime = 0L
    private var shouldKeepRangePlay = false
    private var recordPlayPosition: Long? = null

    private var videoOutputListeners = mutableListOf<INLEListenerVideoOutput>()

    private var playListener: INLEListenerVideoOutput = INLEListenerVideoOutput { source, time ->
        synchronized(statusListenerLock) {

            videoOutputListeners.forEach {
                it.onRefresh(source, time)
            }
            if (!isActive) return@INLEListenerVideoOutput

            // source --0 播放   source -> 1 seek
            if (source == 0) {
                if (isRangePlay && time + 33000 >= rangeEndTime) {

                    pause()
                    playHandler.post {
//                        seekAndMoveTrackAsync(rangeEndTime - 1000) //需要误差1ms来触发seek到当前段的尾帧
                        isRangePlay = shouldKeepRangePlay
                    }
                    return@INLEListenerVideoOutput
                }
                if (isPlaying) {
                    _curPlayPosition.postValue(time)
                }
            }
        }
    }
    private var playInfoListener: INLEListenerCommon = INLEListenerCommon { type, ext, f, msg ->
        //播放到头监听
        if (VECommonCallbackInfo.TE_INFO_EOF == type) {
            _playEndEvent.postValue(Unit)
            this.pause()
        }
    }

    override fun init(play: INLEPlayer) {
        nlePlayer = play
        nlePlayer.setVideoOutputListener(playListener)
        nlePlayer.addOnInfoListener(playInfoListener)
    }

    override fun play() {
        if (shouldKeepRangePlay) {
            playInternalRange()
        } else {
            playInternal()
        }
    }

    override fun playAfterSeekDone() {
        if (currentSeekId == null) {
            play()
        } else {
            playAfterSeekDone = true
        }
    }

    /**
     * 时间单位： us 微秒
     * @param shouldKeep: 表示是否保持rangePlay，默认为false，即一次区间播放完后会自动将
     * isRangePlay 置为false，取消区间播放
     * 为true的话，播放完会seek到区间开头，只播放区间里的内容
     *
     * 手动seek会取消区间播放
     */
    override fun playRange(startTime: Long, endTime: Long, shouldKeep: Boolean) {
        seek(startTime.toMilli())
        isRangePlay = true
        rangeStartTime = startTime
        rangeEndTime = endTime
        shouldKeepRangePlay = shouldKeep
        playInternal()
    }

    override fun setPlayRange(startTime: Long, endTime: Long, shouldKeep: Boolean) {
//        seekAndMoveTrackAsync(startTime)
        isRangePlay = true
        rangeStartTime = startTime
        rangeEndTime = endTime
        shouldKeepRangePlay = shouldKeep
    }

    override fun cancelPlayRange() {
        isRangePlay = false
        shouldKeepRangePlay = false
    }

    private fun playInternal() {
        if (editorContext.hasInitialized) {
            isPlaying = true
            if (totalDuration() - curPosition() < 50) { //track轨道滑到最后，track返回的position不是末尾的值 做了此处理
                refreshCurrentFrame()
                seek(0)
            }
            nlePlayer.play()
            LiveDataBus.getInstance().with(Constants.KEY_MAIN, Int::class.java).postValue(STATE_PLAY)
        }
    }

    private fun playInternalRange() {
        if (editorContext.hasInitialized && isRangePlay) {
            isPlaying = true
            if (rangeEndTime.toMilli() - curPosition() < 30) {
                seek(rangeStartTime.toMilli())
            }
            nlePlayer.play()
            LiveDataBus.getInstance().with(Constants.KEY_MAIN, Int::class.java).postValue(STATE_PLAY)
        }
    }

    override fun resume() {
        if (editorContext.hasInitialized) {
            val isCompilingVideo = editorContext.getVariable(VariableKeys.IS_COMPILING_VIDEO_EVENT, false)
            if (!isCompilingVideo) {
                if (editorContext.getVariable<Boolean>(VariableKeys.ACTIVITY_RESULT_FLAG) == true) {
                    //复位
                    editorContext.setVariable(VariableKeys.ACTIVITY_RESULT_FLAG, false)
                    return
                }
                if ((editorContext.getVariable(VariableKeys.PLAY_POSITION_WHEN_EXIT_EDIT) ?: 0L) > 0L) {
                    return
                }
                val seekPosition = editorContext.getVariable(VariableKeys.PLAY_POSITION_WHEN_COMPILING) ?: 0L
                if (seekPosition > 0) {
                    seek(0)
                    seek(seekPosition)
                } else {
                    seek(recordPlayPosition ?: 0)
                }
            }
        }
    }

    override fun pause() {
        if (editorContext.hasInitialized) {
            editorContext.setVariable(VariableKeys.IS_PLAY_WHILE_EXIT_EDIT, isPlaying, true)
            isPlaying = false
            playAfterSeekDone = false
            recordPlayPosition = curPosition()
            nlePlayer.pause()
            LiveDataBus.getInstance().with(Constants.KEY_MAIN, Int::class.java).postValue(STATE_PAUSE)
        }
    }

    override fun prepare() {
        if (editorContext.hasInitialized) {
            nlePlayer.prepare()
        }
    }

    override fun release() {
        if (editorContext.hasInitialized) {
            pause()
            destroy()
            editorContext.hasInitialized = false
        }
    }

    /**
     * 播放某一段slot
     * 注意需要去除交叠转场的开始结束时间
     */
    override fun playCurrentSlot() {
        if (editorContext.hasInitialized) {
            val slotTimeRange = LongArray(2)
            playRange(slotTimeRange[0], slotTimeRange[1])
        }
    }

    override fun destroy() {
        if (editorContext.hasInitialized) {
            nlePlayer.removeOnInfoListener(playInfoListener)
            nlePlayer.releaseResource()
            nlePlayer.destroy()
        }
    }

    override fun setCanvasMinDuration(duration: Int, needPrepare: Boolean) {
        // NLE层时间单位是us
        editorContext.nleSession.mediaRuntimeApi.setCanvasMinDuration(duration.toLong(), needPrepare)
    }

    override fun addVideoOutPutListener(listener: INLEListenerVideoOutput) {
        videoOutputListeners.add(listener)
    }

    override fun removeVideoOutputListener(listener: INLEListenerVideoOutput) {
        videoOutputListeners.remove(listener)
    }

    /**
     * 使用时请注意，seekTime会有延迟，且素材不同，seekTime 所花时间不同，
     * 该方法中轨道会等待seek成功回调后，再滑动轨道到相应的位置，
     *
     * 如需异步执行seek与move track，请使用 seekAndMoveTrackAsync
     */
    override fun seekToPosition(position: Long, ifMoveTrack: Boolean?, onAfterSeekAction: (() -> Unit)?) {
        if (editorContext.hasInitialized) {
            seek(position) {
                if (ifMoveTrack == true) {
                    LiveDataBus.getInstance().with(Constants.KEY_MAIN, Int::class.java).postValue(STATE_SEEK)
                    onAfterSeekAction?.invoke()
                }
            }
        }
    }

    //ms
    override fun seek(position: Long) {
        seek(position) { ret ->
        }
    }

    //ms
    override fun seek(position: Long, listener: INLEListenerSeek) {
        if (editorContext.hasInitialized) {
            isPlaying = false
            isRangePlay = false
            playAfterSeekDone = false
            val listenerWrapper = object : ListenerSeek(this) {
                override fun onSeekDone(ret: Int) {
                    super.onSeekDone(ret)
                    listener.onSeekDone(ret)
                }
            }
            currentSeekId = listenerWrapper.hashCode()
            val code = nlePlayer.seekTime(position.toMicro(), EDITOR_SEEK_FLAG_LastSeek, listenerWrapper)
            if (code != 0) {
                listenerWrapper.onSeekDone(-1)
            }
        }
    }

    // 单位：毫秒
    override fun curPosition(): Long {
        if (editorContext.hasInitialized) {
            return nlePlayer.currentPosition / 1000
        }
        return 0
    }

    // 单位：毫秒
    override fun totalDuration(): Long {
        if (editorContext.hasInitialized) {
            return nlePlayer.duration / 1000
        }
        return 0
    }

    override fun refreshCurrentFrame(mode: Int?) {
        if (editorContext.hasInitialized) {
            if (mode == null) {
                nlePlayer.refreshCurrentFrame()
            } else {
                nlePlayer.refreshCurrentFrame(mode)
            }
        }
    }

    private open class ListenerSeek(private val player: DefaultPlayer) : INLEListenerSeek {
        override fun onSeekDone(ret: Int) {
            if (player.currentSeekId == hashCode()) {
                player.currentSeekId = null
                if (player.playAfterSeekDone) {
                    player.play()
                }
            }
        }
    }

    override fun dumpVeSeq(): String {
        if (nlePlayer is NLEPlayerPublic) {
            return (nlePlayer as NLEPlayerPublic).dumpVESequence()
        }
        return ""
    }

    override fun dumpVeModel():String{
        if (nlePlayer is NLEPlayerPublic) {
            return (nlePlayer as NLEPlayerPublic).dumpVEModel()
        }
        return ""
    }
}