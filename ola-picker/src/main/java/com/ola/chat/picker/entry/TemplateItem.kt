package com.ola.chat.picker.entry

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/17 16:37
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
@Parcelize
data class TemplateItem(
    val author: Author,
    val title: String?,
    val md5: String?,
    val template_type: Int? = null,
    val provider_media_id: String?,
    val origin_video_info: OriginVideoInfo? = null,
    val extra: String?,
    val template_url: String,
    val template_tags_list: String?,
    val cover: Cover? = null,
    val fragment_count: Int? = null,
    val id: Long?,
    val video_info: OriginVideoInfo? = null,
    val short_title: String?,
    val template_tags: String?
) : Parcelable

@Parcelize
data class Cover(
    val url: String,
    val width: Int,
    val height: Int,
) : Parcelable

@Parcelize
data class OriginVideoInfo(
    val url: String
) : Parcelable

@Parcelize
data class Author(
    val avatarUrl: String,
    val name: String,
    val uid: Long,
) : Parcelable






