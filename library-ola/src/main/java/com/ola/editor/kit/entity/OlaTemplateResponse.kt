package com.ola.editor.kit.entity

import android.os.Parcelable
import com.cutsame.solution.template.model.TemplateItem
import kotlinx.android.parcel.Parcelize

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/24 15:39
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
@Parcelize
data class OlaTemplateResponse(
    val success: Boolean,
    val msg: String,
    val list: List<TemplateItem>,
    val has_next: Boolean,
    val next_rank: Int,
    val next_id: Int
) : Parcelable