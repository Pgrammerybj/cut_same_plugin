package com.cutsame.ui

import android.content.Context
import android.content.Intent
import com.bytedance.ies.cutsame.util.Size
import com.cutsame.solution.template.model.TemplateItem
import com.ss.android.ugc.cut_ui.MediaItem
import com.ss.ugc.android.editor.core.api.params.AudioParam


object CutSameUiIF {

    private const val INTERFACE_PLAYER = "com.ss.android.ugc.cut_ui.PLAY"
    const val ARG_CUT_TEMPLATE_URL = "arg_cut_template_url"

    // 模板的预览视频
    const val ARG_CUT_TEMPLATE_VIDEO_PATH = "arg_cut_template_video_path"
    const val ARG_CUT_SAME_AUDIO_PARAM = "arg_cut_same_audio_param"
    const val ARG_TEMPLATE_ITEM = "arg_template_item"

    /**
     *  创建剪同款播放页面的Intent；这是一个隐式Intent所以可能会跳转到别到App；
     *  你可以调用 [Intent.setPackage] 或者 [Intent.setComponent] 接口指定明确到页面；
     */
    fun createCutUIIntent(
        context: Context,
        templateItem: TemplateItem,
        audioParam: AudioParam,
        videoCache: String
    ): Intent? {
        val intent = Intent(INTERFACE_PLAYER)
        intent.setPackage(context.packageName)
        intent.putExtra(ARG_TEMPLATE_ITEM, templateItem)
        intent.putExtra(ARG_CUT_TEMPLATE_VIDEO_PATH, videoCache)
        intent.putExtra(ARG_CUT_SAME_AUDIO_PARAM, audioParam)
        return if (checkIntent(context, intent)) {
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

    private const val ARG_DATA_PICK_RESULT_MEDIA_ITEMS = "arg_data_pick_result_media_items"
    const val ARG_DATA_PRE_PICK_RESULT_MEDIA_ITEMS =
        "arg_data_pre_pick_result_media_items" //当前槽位信息，为了在素材选择页面在已选槽位上提醒"已选"。


    fun getTemplateVideoCacheByIntent(intent: Intent): String? {
        return intent.getStringExtra(ARG_CUT_TEMPLATE_VIDEO_PATH)
    }

    fun getGalleryPickResultData(intent: Intent): ArrayList<MediaItem>? {
        return intent.getParcelableArrayListExtra(ARG_DATA_PICK_RESULT_MEDIA_ITEMS)
    }

    /**
     * request compressing media
     */
    private const val ARG_DATA_COMPRESS_MEDIA_ITEMS = "arg_data_compress_media_items"
    private const val ARG_DATA_COMPRESS_RESULT_MEDIA_ITEMS = "arg_data_compress_result_media_items"

    fun getCompressDataByIntent(intent: Intent): ArrayList<MediaItem>? {
        return intent.getParcelableArrayListExtra<MediaItem>(ARG_DATA_COMPRESS_MEDIA_ITEMS)
    }

    fun setCompressResultData(intent: Intent, mediaItems: ArrayList<MediaItem>) {
        intent.putParcelableArrayListExtra(ARG_DATA_COMPRESS_RESULT_MEDIA_ITEMS, mediaItems)
    }

    private const val INTERFACE_EXPORT = "com.ss.android.ugc.cut_ui.EXPORT"
    private const val ARG_DATA_EXPORT_CANVAS_SIZE = "arg_data_export_canvas_size"

    fun createExportUIIntent(
        context: Context,
        templateUrl: String,
        canvasSize: Size
    ): Intent? {
        val intent = Intent(INTERFACE_EXPORT)
        intent.setPackage(context.packageName)
        intent.putExtra(ARG_CUT_TEMPLATE_URL, templateUrl)
        intent.putExtra(ARG_DATA_EXPORT_CANVAS_SIZE, canvasSize)
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