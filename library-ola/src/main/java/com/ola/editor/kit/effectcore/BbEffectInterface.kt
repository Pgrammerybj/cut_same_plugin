package com.ola.editor.kit.effectcore

import android.content.Context

interface BbEffectInterface {

    // 1需要与Release成对存在
    fun createEffectCore(applicationContext: Context, handler: BbEffectCoreEventHandler)

    //1.初始化需要
    fun initialize(element: Int)

    /**
     * path：伴奏本地路径
     * pos：伴奏起始播放时间
     */
    fun setAudioMixingFilePath(path: String, pos: Int)

    //1.初始化,append默认false
    fun setAudioRecordFilePath(path: String, append: Boolean)

    /**
     *耳返 bool enabled
     */
    fun enableInEarMonitoring(enabled: Boolean)

    //1.初始化和设置都需要
    fun setAudioEffectPreset(effectIndex: Int)

    /**
     * int profile
     */
    fun setAudioProfile(profile: Int)

    /**
     * seekTo
     */
    fun setAudioMixingPosition(pos: Int)

    fun start()

    fun stop()

    fun resume()

    fun pause()

    fun release()

    /**
     * 伴奏音量 0-100
     * 节点：tunerModel.audioMixingVolume
     */
    fun adjustAudioMixingVolume(volume: Int)

    /**
     * 人声音量 0-100
     * 节点：tunerModel.recordSignalVolume
     */
    fun adjustRecordingSignalVolume(volume: Int)

    /// 调整本地播放的音乐文件的音调。
    /// 按半音音阶调整本地播放的音乐文件的音调，默认值为 0，即不调整音调。
    // 取值范围为 [-12,12]，每相邻两个值的音高距离相差半音。取值的绝对值越大，音调升高或降低得越多。
    fun setAudioMixingPitch(pitch: Int)

    fun getAudioMixingDuration(): Int

    fun getAudioMixingCurrentPosition(): Int

    /**
     * volume 设置播放音量 0 - 100
     * 节点:tunerModel.playbackSignalVolume
     */
    fun adjustPlaybackSignalVolume(volume: Int)

    /// 设置耳返音量
    fun setInEarMonitoringVolume(volume: Int)

    ///1设置混响资源json,取值effectJson
    fun setAudioEffectDataSource(json: String)

    //1.ai降噪，初始化和设置降噪都需要，
    // setParameters("{\"che.audio.ainoise.level\": ${tunerModel?.aiIndex ?? -1}");
    fun setParameters(parameters: String)
}