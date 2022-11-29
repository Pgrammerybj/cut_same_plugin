package com.ola.chat.picker.utils

import android.content.Context
import android.content.Intent
import com.ola.chat.picker.album.model.MediaData
import com.ola.chat.picker.entry.ImagePickConfig
import com.ola.chat.picker.entry.MediaItem
import com.ola.chat.picker.entry.TemplateItem

object PickerConstant {

    enum class TabType {
        Album
    }

    private const val ARG_TEMPLATE_FEED = "com.ss.android.ugc.template_ui.feed.net"

    fun createTemplateUIIntent(context: Context): Intent? {
        val intent = Intent(ARG_TEMPLATE_FEED)
        return if (checkIntent(context, intent)) intent else null
    }

    const val ARG_CUT_TEMPLATE_URL = "arg_cut_template_url"

    // 模板的预览视频
    const val ARG_CUT_TEMPLATE_VIDEO_PATH = "arg_cut_template_video_path"
    const val ARG_TEMPLATE_ITEM = "arg_ola_template_item"
    //图片选择器配置参数
    const val ARG_DATA_PICK_CONFIG = "arg_data_pick_config" //新增

    //跳转到素材编辑页面的Intent
    private const val INTERFACE_CLIP = "com.ola.chat.picker.cut_ui.CLIP"
    const val ARG_DATA_CLIP_MEDIA_ITEM = "arg_data_clip_media_item"
    fun createClipUIIntent(context: Context, mediaItem: MediaItem): Intent? {
        val intent = Intent(INTERFACE_CLIP)
        intent.setPackage(context.packageName)
        intent.putExtra(ARG_DATA_CLIP_MEDIA_ITEM, mediaItem)
        return if (checkIntent(context, intent)) {
            intent
        } else {
            null
        }
    }

    private const val INTERFACE_IMAGE_CLIP = "com.ola.chat.picker.process.ImageCropActivity"
    const val ARG_CLIP_MEDIA_ITEM = "arg_clip_media_item"
    const val ARG_CLIP_PICKER_CONFIG = "arg_clip_picker_config"
    fun createImageClipIntent(
        context: Context,
        mediaData: MediaData,
        imagePickConfig: ImagePickConfig?
    ): Intent? {
        val intent = Intent(INTERFACE_IMAGE_CLIP)
        intent.setPackage(context.packageName)
        intent.putExtra(ARG_CLIP_MEDIA_ITEM, mediaData)
        intent.putExtra(ARG_CLIP_PICKER_CONFIG, imagePickConfig)
        return if (checkIntent(context, intent)) {
            intent
        } else {
            null
        }
    }


    private const val INTERFACE_PICKER = "com.ola.chat.picker.process.PICKER"
    private const val ARG_DATA_PICK_MEDIA_ITEMS = "arg_data_pick_media_items"
    private const val ARG_DATA_PICK_RESULT_MEDIA_ITEMS = "arg_data_pick_result_media_items"
    const val ARG_DATA_PRE_PICK_RESULT_MEDIA_ITEMS = "arg_data_pre_pick_result_media_items" //当前槽位信息，为了在素材选择页面在已选槽位上提醒"已选"。

    fun createGalleryUIIntent(
        context: Context,
        mediaItems: ArrayList<MediaItem>,
        templateItem: TemplateItem
    ): Intent? {
        val intent = Intent(INTERFACE_PICKER)
        intent.setPackage(context.packageName)
        intent.putParcelableArrayListExtra(ARG_DATA_PICK_MEDIA_ITEMS, mediaItems)
        intent.putExtra(ARG_TEMPLATE_ITEM, templateItem)
        return if (checkIntent(context, intent)) {
            intent
        } else {
            null
        }
    }

    fun createSingleGalleryUIIntent(
        context: Context,
        imagePickConfig: ImagePickConfig,
    ): Intent? {
        val intent = Intent(INTERFACE_PICKER)
        intent.setPackage(context.packageName)
        intent.putExtra(ARG_DATA_PICK_CONFIG, imagePickConfig)
        return if (checkIntent(context, intent)) {
            intent
        } else {
            null
        }
    }

    fun getTemplateVideoCacheByIntent(intent: Intent): String? {
        return intent.getStringExtra(ARG_CUT_TEMPLATE_VIDEO_PATH)
    }

    fun getGalleryPickDataByIntent(intent: Intent): ArrayList<MediaItem>? {
        return intent.getParcelableArrayListExtra(ARG_DATA_PICK_MEDIA_ITEMS)
    }

    fun getGalleryPickResultData(intent: Intent): ArrayList<com.ss.android.ugc.cut_ui.MediaItem>? {
        return intent.getParcelableArrayListExtra(ARG_DATA_PICK_RESULT_MEDIA_ITEMS)
    }

    fun setGalleryPickResultData(intent: Intent, mediaItems: ArrayList<com.ss.android.ugc.cut_ui.MediaItem>) {
        intent.putParcelableArrayListExtra(ARG_DATA_PICK_RESULT_MEDIA_ITEMS, mediaItems)
    }

    /**
     * request compressing media
     */
    private const val INTERFACE_COMPRESS = "com.ola.chat.picker.COMPRESS"
    private const val ARG_DATA_COMPRESS_MEDIA_ITEMS = "arg_data_compress_media_items"
    private const val ARG_DATA_COMPRESS_RESULT_MEDIA_ITEMS = "arg_data_compress_result_media_items"
    fun createCompressUIIntent(
        context: Context,
        mediaItems: ArrayList<com.ss.android.ugc.cut_ui.MediaItem>,
        templateUrl: String
    ): Intent? {
        val intent = Intent(INTERFACE_COMPRESS)
        intent.setPackage(context.packageName)
        intent.putExtra(ARG_CUT_TEMPLATE_URL, templateUrl)
        intent.putParcelableArrayListExtra(ARG_DATA_COMPRESS_MEDIA_ITEMS, mediaItems)
        return if (checkIntent(context, intent)) {
            intent
        } else {
            null
        }
    }


    fun setCompressResultData(intent: Intent, mediaItems: ArrayList<com.ss.android.ugc.cut_ui.MediaItem>) {
        intent.putParcelableArrayListExtra(ARG_DATA_COMPRESS_RESULT_MEDIA_ITEMS, mediaItems)
    }

    fun getCompressResultData(intent: Intent): ArrayList<com.ss.android.ugc.cut_ui.MediaItem>? {
        return intent.getParcelableArrayListExtra(ARG_DATA_COMPRESS_RESULT_MEDIA_ITEMS)
    }

    private fun checkIntent(context: Context, intent: Intent): Boolean {
        val queryIntentActivities = context.packageManager.queryIntentActivities(intent, 0)
        var size = 0
        queryIntentActivities.forEach {
            if ("user" == it.activityInfo.nonLocalizedLabel) {
                intent.setClassName(it.activityInfo.packageName, it.activityInfo.name)
            }
            if (it.activityInfo.packageName.equals(context.packageName)) {
                size++
            }
        }
        return size > 0
    }

    fun getCompressDataByIntent(intent: Intent): ArrayList<com.ss.android.ugc.cut_ui.MediaItem>? {
        return intent.getParcelableArrayListExtra(ARG_DATA_COMPRESS_MEDIA_ITEMS)
    }

}