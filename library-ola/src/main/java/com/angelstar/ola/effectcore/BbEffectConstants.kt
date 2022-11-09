package com.angelstar.ola.effectcore

object BbEffectConstants {
    const val TAG = "EffectCore"
}

val audioEffectCoreAudition: Int = 0;
val audioEffectCoreRecord = 1
val audioEffectCoreEdit = 2
val audioEffectCoreProduct = 3
val audioEffectCoreRecordSingle = 4
val audioEffectCoreMark = 5

enum class ErrorCode {
    Zero,
    UnInitialized,
    SetInitialize,
    NativeInitialize,
    InValidLyrics,
    AudioEffectPreset,
    AudioProfile,
    AdjustAudioMixingVolume,
    AdjustRecordingSignalVolume,
    AudioMixingPosition,
    SwitchAccompany,
    SetTolerance,
    SetNoiseLevel,
    Set3AType,
    PushAudioDataMark,
    SetAudioEffectDataSourceType
}