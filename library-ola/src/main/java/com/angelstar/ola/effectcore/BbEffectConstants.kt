package com.angelstar.ola.effectcore

class BbEffectConstants {
    companion object {
        const val TAG = "Ola-EffectCore"
        const val audioEffectCoreAudition = 0;
        const val audioEffectCoreRecord = 1
        const val audioEffectCoreEdit = 2
        const val audioEffectCoreProduct = 3
        const val audioEffectCoreRecordSingle = 4
        const val audioEffectCoreMark = 5

        const val stateAudioEffectFailed = -1
        const val stateAudioEffectOk = 0
        const val stateAudioEffectPlaying = 1

        // 伴奏结束时回调，此时录音结束生成目标文件,主动调用Stop函数会有此回调
        const val stateAudioEffectFinish: Int = 2
    }
}