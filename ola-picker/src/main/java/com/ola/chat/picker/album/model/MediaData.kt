package com.ola.chat.picker.album.model

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.Size
import com.ola.chat.picker.R
import kotlinx.android.parcel.Parcelize

enum class MediaType(val stringId: Int) {
    TYPE_IMAGE(R.string.picker_common_title_image), TYPE_VIDEO(
        R.string.picker_common_title_video
    )
}

enum class MaterialEditType(val stringId: Int) {
    TYPE_VIDEO(R.string.ola_material_edit_video), TYPE_TEXT(R.string.ola_material_edit_text), TYPE_LYRICS(
        R.string.ola_material_edit_lyrics
    )
}

enum class TitleMediaType(val stringId: Int) {
    TYPE_ALL(R.string.picker_common_title_all), TYPE_VIDEO(R.string.picker_common_title_video), TYPE_IMAGE(
        R.string.picker_common_title_image
    )
}

@Parcelize
data class MediaData(

    val mimeType: String,

    val path: String,

    val uri: Uri,

    val displayName: String,

    val dateAdd: Long,

    val dateModify: Long,

    @Size(min = 0)
    val size: Long,

    @Size(min = 0)
    val width: Int,

    @Size(min = 0)
    val height: Int,

    val duration: Long = 0,

    val orientation: Int

) : Parcelable {
    fun isVideo(): Boolean = mimeType.startsWith("video/")
    fun isImage(): Boolean = mimeType.startsWith("image/")
}