package com.ss.ugc.android.editor.track.utils

/**
 * time : 2020/12/10
 * author : tanxiao
 * description :
 *
 */
class Constants {
    companion object {
        const val TRACK_INDEX = "track_index"

        const val TRACK_VIDEO = "video"
        const val TRACK_AUDIO = "audio"
        const val TRACK_STICKER = "sticker"


        const val CLIP_INDEX = "clip_index"
        const val CLIP_REVERSE_VIDEO_PATH = "clip_reverse_video_path"

        const val FILTER_POSITION = "filter_position" //保存所添加滤镜的position
        const val FILTER_INTENSITY = "filter_intensity" //保存所添加滤镜的position
        const val ADJUST_POSITION = "adjust_position" //保存所添加调节选项的position
        const val Transition_POSITION = "transition_position" //保存所添加转场选项的position
        const val Transition_MAX_DURATION = "Transition_Max_Duration" //保存所添加转场选项的position

        const val KEY_FUNCTION = "key_function"
        const val KEY_MAIN = "key_mainViewModel"
        const val KEY_FUNCTION_BACK = "key_function_back"
        const val KEY_ADD_AUDIO = "key_add_audio"

        const val MODEL_TRACK_SIZE = "model_track_size"  //记录model中track的数量
    }
}