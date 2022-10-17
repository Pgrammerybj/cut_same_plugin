package com.ola.chat.picker.entry

import android.net.Uri
import android.os.Parcelable
import android.webkit.URLUtil
import kotlinx.android.parcel.Parcelize
import java.io.File
import kotlin.jvm.internal.Intrinsics

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/17 16:00
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
@Parcelize
data class MediaItem(
    var materialId: String,
    var targetStartTime: Long,
    var isMutable: Boolean,
    var alignMode: String,
    var isSubVideo: Boolean,
    var isReverse: Boolean,
    var cartoonType: Int,
    var gamePlayAlgorithm: String,
    var width: Int,
    var height: Int,
    var duration: Long,
    var oriDuration: Long,
    var source: String,
    var sourceStartTime: Long,
    var cropScale: Float,
    var crop: ItemCrop,
    var type: String,
    var mediaSrcPath: String,
    var targetEndTime: Long,
    var volume: Float
) : Parcelable {
    companion object {
        const val TYPE_PHOTO = "photo"
        const val TYPE_VIDEO = "video"
        const val ALIGN_MODE_VIDEO = "align_video"
        const val ALIGN_MODE_CANVAS = "align_canvas"
    }

    fun getUri(): Uri {
        val uri: Uri
        return if (URLUtil.isValidUrl(mediaSrcPath)) {
            uri = Uri.parse(mediaSrcPath)
            Intrinsics.checkNotNullExpressionValue(uri, "parse(mediaSrcPath)")
            uri
        } else {
            val file = File(mediaSrcPath)
            if (file.exists() && file.isFile) {
                uri = Uri.fromFile(file)
                Intrinsics.checkNotNullExpressionValue(uri, "fromFile(file)")
                uri
            } else {
                uri = Uri.EMPTY
                Intrinsics.checkNotNullExpressionValue(uri, "EMPTY")
                uri
            }
        }
    }
}

@Parcelize
data class ItemCrop(
    var lowerRightX: Float,
    var lowerRightY: Float,
    var upperLeftX: Float,
    var upperLeftY: Float
) : Parcelable