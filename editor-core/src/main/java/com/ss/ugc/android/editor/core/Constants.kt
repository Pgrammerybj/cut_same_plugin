package com.ss.ugc.android.editor.core

class Constants {
    companion object {
        const val TRACK_TYPE = "track_type"
        const val PIP_TRACK_INDEX = "pip_track_index"
        const val MUSIC_NAME = "music_name" //保存所添加转场选项的position

        const val TRACK_VIDEO = "video"
        const val TRACK_AUDIO = "audio"
        const val TRACK_STICKER = "sticker"

        const val MAIN_TRACK_MUTE_KEY = "is_maintrack_mute"
        const val MAIN_TRACK_MUTE_ENABLE = "is_maintrack_mute_enable"
        const val SLOT_MUTE_VOLUME_KEY = "slot_mute_volume"
        const val SELECTED_NLE_TRACK = "selected_nle_track"                                 //当前选中的视频轨道

        const val TRACK_FILTER_ADJUST = "type_filter_adjust"
        const val TRACK_FILTER_FILTER = "type_filter_filter"
        const val TRACK_VIDEO_EFFECT = "video_effect" //视频特效
        const val TRACK_LAYER = "track_layer"  //记录model中track的数量
        const val KEY_MAIN = "key_mainViewModel"

        const val CLIP_REVERSE_VIDEO_PATH = "clip_reverse_video_path"
        const val KEY_ADD_FILTER = "key_add_filter"


        ///////////////////////////////////////////////////////////////////////////
        // ExtraKeys
        ///////////////////////////////////////////////////////////////////////////
        // slot素材来源，共4种value：来自：拍摄页、Editor Pro；方式：拍摄、上传
        const val SLOT_EXTRA_SOURCE_TYPE = "slot_extra_source_type"

        /**
         * 文字模板，依赖资源的urs_id
         */
        const val DEP_RES_URS_ID = "dependency_res_id"
        /**
         * 是否支持外网访问
         */
        var EXTRANET_ENVIRONMENT = true



        const val STATE_PAUSE = 0
        const val STATE_PLAY = 1
        const val STATE_SEEK = 3

    }
}