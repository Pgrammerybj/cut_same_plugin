package com.angelstar.ola.effectcore

class BbEffectConstants {
    companion object {
        const val stateAudioFailed = -1
        const val stateAudioStop = 0
        const val stateAudioPlaying = 1
        // 伴奏结束时回调，此时录音结束生成目标文件,主动调用Stop函数会有此回调
        const val stateAudioFinish: Int = 2
    }
}