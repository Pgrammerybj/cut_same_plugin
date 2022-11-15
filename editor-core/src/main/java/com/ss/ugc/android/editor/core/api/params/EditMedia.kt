package com.ss.ugc.android.editor.core.api.params

import android.content.ContentResolver
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.Keep
import com.bytedance.ies.nle.editor_jni.NLEResType
import kotlinx.android.parcel.Parcelize

@Keep
@Parcelize
data class EditMedia(val path: String, val isVideo: Boolean, val absolutePath: String? = null) : Parcelable

fun EditMedia.getMediaType(): Int {
    return if (isVideo) NLEResType.VIDEO.swigValue() else NLEResType.IMAGE.swigValue()
}

fun EditMedia.convertPath(): String {
    return if (!isVideo && Uri.parse(path).scheme == ContentResolver.SCHEME_CONTENT) {
        absolutePath ?: path
    } else {
        path
    }
}