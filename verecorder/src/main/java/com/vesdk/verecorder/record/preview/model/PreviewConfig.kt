package com.vesdk.verecorder.record.preview.model

import android.os.Parcel
import android.os.Parcelable
import com.ss.android.vesdk.VEPreviewSettings

/**
 * 相机预览配置
 */

public data class PreviewConfig(
    val uiStyle: Int,
    val enableGl3: Boolean = false,
    val enableRefactorRecorder: Boolean = false,
    val enableAudioRecorder: Boolean = true,
    val enableEffectAmazing: Boolean = false,
    val recordType: Int? = null,
    val enable3buffer: Boolean = false,
    val enableEffectRT: Boolean = false,
    val stopPrePlay: Boolean = false,
    val enableFollowShotIndependentThread: Boolean = false,
    val autoChangeDisplay: Boolean = true,
    val saveAlbum: Boolean = true,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
    )


    class Builder {
        private var uiStyle: Int = UI_STYLE_DEFAULT
        private var enableGl3 = false
        private var enableRefactorRecorder = false
        private var enableAudioRecorder: Boolean = true
        private var enableEffectAmazing = false
        private var recordType: Int? = null
        private var enable3buffer = false
        private var enableEffectRT = false
        private var stopPrePlay: Boolean = false
        private var enableFollowShotIndependentThread: Boolean = false
        private var autoChangeDisplay: Boolean = false
        private var saveAlbum: Boolean = true

        companion object {
            const val RECORD_TYPE_ORIGIN = 0
            const val RECORD_TYPE_EFFECT = 1
            const val RECORD_TYPE_FULL = 2
            const val RECORD_TYPE_INTERMEDIATE = 3

            const val UI_STYLE_DEFAULT = 0
            const val UI_STYLE_CUT_SAME = 1
            const val UI_STYLE_DUT = 2//合拍
        }

        fun uiStyle(uiStyle: Int) = apply { this.uiStyle = uiStyle }
        fun enableGl3(enable: Boolean) = apply { this.enableGl3 = enable }
        fun enableRefactorRecorder(enable: Boolean) = apply { this.enableRefactorRecorder = enable }
        fun enableAudioRecorder(enable: Boolean) = apply { this.enableAudioRecorder = enable }
        fun enableEffectAmazing(enable: Boolean) = apply { this.enableEffectAmazing = enable }
        fun recordType(type: Int) = apply { this.recordType = type }
        fun enable3buffer(enable: Boolean) = apply { this.enable3buffer = enable }
        fun enableEffectRT(enable: Boolean) = apply { this.enableEffectRT = enable }
        fun stopPrePlay(enable: Boolean) = apply { this.stopPrePlay = enable }
        fun enableFollowShot(enable: Boolean) =
            apply { this.enableFollowShotIndependentThread = enable }

        fun autoChangeDisplay(enable: Boolean) =
            apply { this.autoChangeDisplay = enable }

        fun saveAlbum(saveAlbum: Boolean) =
            apply { this.saveAlbum = saveAlbum }

        fun build(): PreviewConfig {
            return PreviewConfig(
                uiStyle,
                enableGl3,
                enableRefactorRecorder,
                enableAudioRecorder,
                enableEffectAmazing,
                recordType,
                enable3buffer,
                enableEffectRT,
                stopPrePlay,
                enableFollowShotIndependentThread,
                autoChangeDisplay,
                saveAlbum
            )
        }
    }

    fun getVERecordContentType(): VEPreviewSettings.VERecordContentType {
        return when (recordType) {
            Builder.RECORD_TYPE_ORIGIN -> VEPreviewSettings.VERecordContentType.RecordOriginContent
            Builder.RECORD_TYPE_EFFECT -> VEPreviewSettings.VERecordContentType.RecordEffectContent
            Builder.RECORD_TYPE_FULL -> VEPreviewSettings.VERecordContentType.RecordFullContent
            Builder.RECORD_TYPE_INTERMEDIATE -> VEPreviewSettings.VERecordContentType.RecordIntermediateContent
            else -> VEPreviewSettings.VERecordContentType.RecordFullContent
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (enableGl3) 1 else 0)
        parcel.writeByte(if (enableRefactorRecorder) 1 else 0)
        parcel.writeByte(if (enableAudioRecorder) 1 else 0)
        parcel.writeByte(if (enableEffectAmazing) 1 else 0)
        parcel.writeValue(recordType)
        parcel.writeByte(if (enable3buffer) 1 else 0)
        parcel.writeByte(if (enableEffectRT) 1 else 0)
        parcel.writeByte(if (enableFollowShotIndependentThread) 1 else 0)
        parcel.writeByte(if (autoChangeDisplay) 1 else 0)
        parcel.writeByte(if (saveAlbum) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PreviewConfig> {
        override fun createFromParcel(parcel: Parcel): PreviewConfig {
            return PreviewConfig(parcel)
        }

        override fun newArray(size: Int): Array<PreviewConfig?> {
            return arrayOfNulls(size)
        }
    }
}