package com.cutsame.ui.utils

import com.cutsame.solution.template.model.TemplateItem
import com.ola.chat.picker.entry.Author
import com.ola.chat.picker.entry.Cover
import com.ola.chat.picker.entry.OriginVideoInfo
import com.ola.chat.picker.entry.TemplateItem as PickerEntryTemplateItem

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/31 14:53
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
object CutSameTemplateUtils {

     fun parseTemplateItem(templateItem: TemplateItem): PickerEntryTemplateItem {
        val authorEntry= templateItem.author
        val author = Author(authorEntry?.avatarUrl!!,authorEntry.name,authorEntry.uid)
        val coverEntry = templateItem.cover
        val cover = Cover(
            coverEntry!!.url,
            coverEntry.width,
            coverEntry.height
        )
        val originVideoInfo = OriginVideoInfo(templateItem.videoInfo!!.url)
        return PickerEntryTemplateItem(
            author,
            templateItem.title,
            templateItem.md5,
            templateItem.template_type,
            templateItem.provider_media_id,
            originVideoInfo,
            templateItem.extra,
            templateItem.templateUrl,
            templateItem.templateTags,
            cover,
            templateItem.fragmentCount,
            templateItem.id,
            originVideoInfo,
            templateItem.shortTitle,
            templateItem.templateTags
        )
    }
}