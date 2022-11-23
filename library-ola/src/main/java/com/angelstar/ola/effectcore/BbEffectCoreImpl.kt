package com.angelstar.ola.effectcore

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.angelstar.ola.BuildConfig
import com.angelstar.ola.entity.AudioMixingEntry.TunerModel
import com.angelstar.ola.interfaces.ITemplateAudioPlayListener
import com.angelstar.ola.utils.ThreadUtils
import com.voice.core.BBEffectCore
import java.util.*

@SuppressLint("StaticFieldLeak")
object BbEffectCoreImpl : BbEffectInterface {

    private var bbEffectCore: BBEffectCore? = null
    private lateinit var appCtx: Context
    var currentReverbIndex: Int = -1
    var currentEqualizerIndex: Int = -1
    var currentBoardEffectIndex: Int = -1
    private lateinit var handler: BbEffectCoreEventHandler
    private lateinit var timer: Timer
    private lateinit var audioProgressListener: ITemplateAudioPlayListener

    override fun createEffectCore(applicationContext: Context, handler: BbEffectCoreEventHandler) {
        appCtx = applicationContext
        this.handler = handler
        // 确保单实例
        innerRelease()
        bbEffectCore = BBEffectCore(appCtx)
        bbEffectCore!!.setEventHandler(handler.eventHandler)
        handler.engine = bbEffectCore
    }

    override fun initialize(element: Int) {
        bbEffectCore!!.initialize(element)
    }

    override fun setAudioMixingFilePath(path: String, pos: Int) {
        bbEffectCore!!.setAudioMixingFilePath(path, pos)
    }

    override fun setAudioRecordFilePath(path: String, append: Boolean) {
        bbEffectCore!!.setAudioRecordFilePath(path, append)
    }

    override fun enableInEarMonitoring(enabled: Boolean) {
        bbEffectCore!!.enableInEarMonitoring(enabled)
    }

    override fun setAudioEffectPreset(effectIndex: Int) {
        bbEffectCore!!.audioEffectPreset = effectIndex
    }

    override fun setAudioMixingPosition(pos: Int) {
        bbEffectCore!!.setAudioMixingPosition(pos)
    }

    override fun setAudioProfile(profile: Int) {
        if (profile > 0) {
            bbEffectCore!!.setAudioProfile(profile)
        }
    }

    override fun start() {
        bbEffectCore!!.start()
        //开始调用定时器
        startSchedule()
    }

    override fun stop() {
        bbEffectCore!!.stop()
        stopSchedule()
    }

    private fun stopSchedule() {
        timer.cancel()
    }

    override fun resume() {
        bbEffectCore!!.resume()
    }

    override fun pause() {
        bbEffectCore!!.pause()
    }

    override fun release() {
        innerRelease()
    }

    override fun adjustAudioMixingVolume(volume: Int) {
        if (volume >= 0) {
            bbEffectCore!!.adjustAudioMixingVolume(volume)
        }
    }

    override fun adjustRecordingSignalVolume(volume: Int) {
        if (volume >= 0) {
            bbEffectCore!!.adjustRecordingSignalVolume(volume)
        }
    }

    override fun setAudioMixingPitch(pitch: Int) {
        bbEffectCore!!.setAudioMixingPitch(pitch)
    }

    override fun getAudioMixingDuration(): Int {
        return bbEffectCore!!.audioMixingDuration
    }

    override fun getAudioMixingCurrentPosition(): Int {
        return bbEffectCore!!.audioMixingCurrentPosition
    }

    override fun adjustPlaybackSignalVolume(volume: Int) {
        if (volume >= 0) {
            bbEffectCore!!.adjustPlaybackSignalVolume(volume)
        }
    }

    override fun setInEarMonitoringVolume(volume: Int) {
        if (volume >= 0) {
            bbEffectCore!!.setInEarMonitoringVolume(volume)
        }
    }

    override fun setAudioEffectDataSource(json: String) {
        bbEffectCore!!.setAudioEffectDataSource(json)
    }

    override fun setParameters(parameters: String) {
        bbEffectCore!!.setParameters(parameters)
    }

    private fun innerRelease() {
        if (bbEffectCore != null) {
            bbEffectCore!!.setEventHandler(null)
            bbEffectCore!!.release()
            bbEffectCore = null
            timer = Timer()
        }
    }

    private fun startSchedule() {
        timer.schedule(object : TimerTask() {
            override fun run() {
                val currentProgress: Int = bbEffectCore!!.audioMixingCurrentPosition
                if (BuildConfig.DEBUG) {
                    Log.i("startSchedule", "currentProgress:$currentProgress")
                }
                ThreadUtils.uiHandler.post {
                    audioProgressListener.onPlayTimeChanged(currentProgress.toFloat())
                }
            }
        }, 0, 200)
    }

    /**
     * 实力唱将[16] = 音乐厅 + 温柔
     * 天籁之音[17] = 空间 + 明亮
     * 浑厚饱满[18] = 录音棚 + 浑厚
     *
     * @param index 选中的外层推荐调音标号
     */
    fun changeReverbAndEqualizer(index: Int, tunerModel: TunerModel) {
        var reverbIndex = -1
        var equalizerIndex = -1
        var reverbName = ""
        var equalizerName = ""
        when (index) {
            16 -> {
                reverbIndex = 3
                equalizerIndex = 12
                reverbName = "音乐厅"
                equalizerName = "温柔"
            }
            17 -> {
                reverbIndex = 5
                equalizerIndex = 11
                reverbName = "空间"
                equalizerName = "明亮"
            }
            18 -> {
                reverbIndex = 2
                equalizerIndex = 13
                reverbName = "录音棚"
                equalizerName = "浑厚"
            }
            else -> {
            }
        }
        tunerModel.reverbIndex = reverbIndex
        tunerModel.reverbName = reverbName
        tunerModel.equalizerIndex = equalizerIndex
        tunerModel.equalizerName = equalizerName

        currentReverbIndex = reverbIndex
        currentEqualizerIndex = equalizerIndex

    }

    /**
     * 实力唱将[16] = 音乐厅[3] + 温柔[12]
     * 天籁之音[17] = 空间[5] + 明亮[11]
     * 浑厚饱满[18] = 录音棚[2] + 浑厚[13]
     * 只有满足上述条件的时候我们才需要更新
     * type 0:reverb,1:equalizer
     */
    fun changeAudioAndBoardEffect(type: Int, index: Int): Int {
        if (type == 0) {
            //混响被修改
            currentReverbIndex = index
            //计算最终的偏移格式eg:300
        } else {
            //均衡器被修改
            currentEqualizerIndex = index
            //计算最终的偏移格式eg:130
        }
        if (currentReverbIndex == 3 && currentEqualizerIndex == 12) {
            //实力唱将=音乐厅+温柔
            currentBoardEffectIndex = 16
        } else if (currentReverbIndex == 5 && currentEqualizerIndex == 11) {
            //天籁之音=空间+明亮
            currentBoardEffectIndex = 17
        } else if (currentReverbIndex == 2 && currentEqualizerIndex == 13) {
            //浑厚饱满=录音棚+浑厚
            currentBoardEffectIndex = 18
        }

        // 1️⃣ 调用SDK设置音效
        setAudioEffectPreset(
            formatReverbAndEqualizer(
                currentReverbIndex.toString(),
                currentEqualizerIndex.toString()
            )
        )
        // 2️⃣ 返回符合BoardEffect的值刷新BoardEffectAdapter选项
        return currentBoardEffectIndex
    }

    /**
     * 按照要求将reverb和equalizer都需要改为3位的格式然后拼接在一起
     * eg:300+120 => 300120
     */
    private fun formatReverbAndEqualizer(reverb: String, equalizer: String): Int {
        val reverbFormat = String.format("%-3s", reverb).replace(" ", "0")
        val equalizerFormat = String.format("%-3s", equalizer).replace(" ", "0")
        return (reverbFormat + equalizerFormat).toInt()
    }

    fun setAudioProgressListener(audioProgressListener: ITemplateAudioPlayListener) {
        this.audioProgressListener = audioProgressListener
    }
}