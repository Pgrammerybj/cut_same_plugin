package com.ola.chat.picker.album.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CheckBoxMediaData(
    val mediaData: MediaData,
    var isSelected: Boolean
) : Parcelable
