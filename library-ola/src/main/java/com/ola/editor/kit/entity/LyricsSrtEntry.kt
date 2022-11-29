package com.ola.editor.kit.entity;

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * @Author：yangbaojiang
 * @Date: 2022/11/16 15:34
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */

@Parcelize
data class LyricsSrtEntry(
    val header: SongHeader,
    val content: List<SongContent>,
) : Parcelable

@Parcelize
data class SongHeader(
    val title: String,
    val artist: String,
    val musicBy: String,
    val writtenBy: String,
) : Parcelable

@Parcelize
data class SongContent(
    val text: String,
    val timeId: String
) : Parcelable
