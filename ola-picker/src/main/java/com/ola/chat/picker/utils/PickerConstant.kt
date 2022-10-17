package com.ola.chat.picker.utils

import android.content.Context
import android.content.Intent
import com.ola.chat.picker.R
import com.ola.chat.picker.entry.MediaItem
import com.ola.chat.picker.entry.TemplateItem

object PickerConstant {

    enum class TabType {

        Album, Camera;

        fun getTabName(context: Context): String {
            return when (this) {
                Album -> context.getString(R.string.pick_from_album)
                Camera -> context.getString(R.string.pick_from_camera)
            }
        }
    }

    private const val ARG_TEMPLATE_FEED = "com.ss.android.ugc.template_ui.feed.net"
    fun createTemplateUIIntent(context: Context): Intent? {
        val intent = Intent(ARG_TEMPLATE_FEED)
        return if (checkIntent(context, intent)) intent else null
    }

    private const val INTERFACE_PLAYER = "com.ss.android.ugc.cut_ui.PLAY"
    const val ARG_CUT_TEMPLATE_URL = "arg_cut_template_url"

    // 模板的预览视频
    const val ARG_CUT_TEMPLATE_VIDEO_PATH = "arg_cut_template_video_path"
    const val ARG_TEMPLATE_ITEM = "arg_template_item"

    /**
     *  创建剪同款播放页面的Intent；这是一个隐式Intent所以可能会跳转到别到App；
     *  你可以调用 [Intent.setPackage] 或者 [Intent.setComponent] 接口指定明确到页面；
     */
    fun createCutUIIntent(
        context: Context,
        templateItem: TemplateItem,
        videoCache: String
    ): Intent? {
        val intent = Intent(INTERFACE_PLAYER)
        intent.setPackage(context.packageName)
        intent.putExtra(ARG_TEMPLATE_ITEM, templateItem)
        intent.putExtra(ARG_CUT_TEMPLATE_VIDEO_PATH, videoCache)
        return if (checkIntent(context, intent)
        ) {
            intent
        } else {
            null
        }
    }

    //跳转到素材编辑页面的Intent
    private const val INTERFACE_CLIP = "com.ss.android.ugc.cut_ui.CLIP"
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

    private const val INTERFACE_PICKER = "com.ola.chat.picker.process.PICKER"
    private const val ARG_DATA_PICK_MEDIA_ITEMS = "arg_data_pick_media_items"
    private const val ARG_DATA_PICK_RESULT_MEDIA_ITEMS = "arg_data_pick_result_media_items"
    const val ARG_DATA_PRE_PICK_RESULT_MEDIA_ITEMS =
        "arg_data_pre_pick_result_media_items" //当前槽位信息，为了在素材选择页面在已选槽位上提醒"已选"。

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

    fun getTemplateVideoCacheByIntent(intent: Intent): String? {
        return intent.getStringExtra(ARG_CUT_TEMPLATE_VIDEO_PATH)
    }

    fun getGalleryPickDataByIntent(intent: Intent): ArrayList<MediaItem>? {
        return intent.getParcelableArrayListExtra<MediaItem>(ARG_DATA_PICK_MEDIA_ITEMS)
    }

    fun setGalleryPickResultData(intent: Intent, mediaItems: ArrayList<MediaItem>) {
        intent.putParcelableArrayListExtra(ARG_DATA_PICK_RESULT_MEDIA_ITEMS, mediaItems)
    }

    /**
     * request compressing media
     */
    private const val INTERFACE_COMPRESS = "com.ss.android.ugc.cut_ui.COMPRESS"
    private const val ARG_DATA_COMPRESS_MEDIA_ITEMS = "arg_data_compress_media_items"
    private const val ARG_DATA_COMPRESS_RESULT_MEDIA_ITEMS = "arg_data_compress_result_media_items"
    fun createCompressUIIntent(
        context: Context,
        mediaItems: ArrayList<MediaItem>,
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

    fun getCompressResultData(intent: Intent): ArrayList<MediaItem>? {
        return intent.getParcelableArrayListExtra<MediaItem>(ARG_DATA_COMPRESS_RESULT_MEDIA_ITEMS)
    }

    private const val INTERFACE_EXPORT = "com.ss.android.ugc.cut_ui.EXPORT"
    private const val ARG_DATA_EXPORT_CANVAS_SIZE = "arg_data_export_canvas_size"


    private const val INTERFACE_MORE_EDITOR = "record_sdk_action_ve"
    private const val ARG_DATA_MORE_EDITOR_DATA = "data"
    private const val ARG_DATA_MORE_EDITOR_FROM_BUSINESS = "extra_key_from_type"
    private const val ARG_DATA_MORE_EDITOR_FROM_BUSINESS_CUTSAME = 100

    /**
     * 创建更多编辑页面的Intent
     * @param
     * @return
     */
    fun createMoreEditorUIIntent(context: Context): Intent? {
        val intent = Intent(INTERFACE_MORE_EDITOR)
        intent.putExtra(
            ARG_DATA_MORE_EDITOR_FROM_BUSINESS,
            ARG_DATA_MORE_EDITOR_FROM_BUSINESS_CUTSAME
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (checkIntent(context, intent)) {
            intent
        } else {
            null
        }
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

}