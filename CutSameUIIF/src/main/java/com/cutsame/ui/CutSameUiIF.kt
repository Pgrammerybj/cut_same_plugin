package com.cutsame.ui

import android.content.Context
import android.content.Intent
import com.cutsame.solution.template.model.TemplateItem
import com.ss.android.ugc.cut_ui.MediaItem
import com.ss.ugc.android.editor.core.api.params.AudioParam


object CutSameUiIF {

    private const val INTERFACE_PLAYER = "com.ss.android.ugc.cut_ui.PLAY"
    const val ARG_CUT_COVER_URL = "arg_cut_cover_url"

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

    private const val OLA_TEMPLATE_LIST = "com.angelstar.ola.process.OlaTemplateFeedActivity"
    const val ARG_DATA_NLE_MODEL_FILE_PATH = "arg_data_nle_model_file_path"
    const val ARG_DATA_COVER_FILE_PATH = "arg_data_cover_file_path"
    const val ARG_DATA_MORE_EDITOR_FROM_BUSINESS = "extra_key_from_type"
    const val ARG_DATA_MORE_EDITOR_FROM_BUSINESS_CUTSAME = 100

    /**
     * 跳转到模版列表页面的Intent
     * @param writeFilePath 写入到本地的nleModel文件路径，使用后该文件将会删除
     */
    fun jumpToOlaTemplateListIntent(
        context: Context,
        writeFilePath: String,
        coverPath: String
    ): Intent? {
        val intent = Intent(OLA_TEMPLATE_LIST)
        intent.putExtra(ARG_DATA_NLE_MODEL_FILE_PATH, writeFilePath)
        intent.putExtra(ARG_DATA_COVER_FILE_PATH, coverPath)
        intent.putExtra(
            ARG_DATA_MORE_EDITOR_FROM_BUSINESS,
            ARG_DATA_MORE_EDITOR_FROM_BUSINESS_CUTSAME
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
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

    fun createExportUIIntent(
        context: Context,
        coverPath: String
    ): Intent? {
        val intent = Intent(INTERFACE_EXPORT)
        intent.setPackage(context.packageName)
        intent.putExtra(ARG_CUT_COVER_URL, coverPath)
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