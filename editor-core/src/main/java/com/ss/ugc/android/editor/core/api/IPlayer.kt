package com.ss.ugc.android.editor.core.api

import androidx.lifecycle.LiveData
import com.bytedance.ies.nle.editor_jni.INLEListenerSeek
import com.bytedance.ies.nle.editor_jni.INLEListenerVideoOutput
import com.bytedance.ies.nle.editor_jni.INLEPlayer


interface IPlayer {

    var isPlaying: Boolean
    var isActive:Boolean

    val curPlayPosition: LiveData<Long>
    val videoPlayEndEvent: LiveData<Unit>

    fun init(play: INLEPlayer)

    fun play()

    fun playAfterSeekDone()

    fun prepare()

    fun release()

    fun playCurrentSlot()

    fun playRange(startTime: Long, endTime: Long, shouldKeep: Boolean = false)

    fun setPlayRange(startTime: Long, endTime: Long, shouldKeep: Boolean = false)

    fun cancelPlayRange()

    fun seekToPosition(position: Long, ifMoveTrack: Boolean? = true, onAfterSeekAction: (() -> Unit)? = null)

    fun seek(position: Long)

    fun seek(position: Long,listener:INLEListenerSeek)

    fun curPosition(): Long

    fun totalDuration(): Long

    fun refreshCurrentFrame(mode: Int?=null)

    //--------------生命周期方法--------------
    fun resume()

    fun pause()

    fun destroy()

    fun setCanvasMinDuration(duration: Int, needPrepare: Boolean)

    fun addVideoOutPutListener(listener: INLEListenerVideoOutput)

    fun removeVideoOutputListener(listener: INLEListenerVideoOutput)

    fun dumpVeSeq():String

    fun dumpVeModel():String
}