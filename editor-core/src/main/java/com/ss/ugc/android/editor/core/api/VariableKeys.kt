package com.ss.ugc.android.editor.core.api

class VariableKeys {

    companion object {
        const val CHANGE_RESOLUTION_EVENT = "change_resolution_event"                       //修改分辨率事件
        const val CHANGE_RATIO_EVENT = "change_ratio_event"                                 //修改画布比例事件
        const val SELECT_SLOT_EVENT = "select_slot_event"                                   //slot选中事件
        const val SEEK_TO_SLOT_START = "seek_to_slot_start"                                 //seek到slot开头
        const val PANEL_BOUNCE_EVENT = "panel_bounce_event"                           //面板操作事件
        const val ANIMATION_CHANGED_EVENT = "animation_changed_event"                       //动画改变事件
        const val UPDATE_CLIP_RANGE_EVENT = "update_clip_range_event"                       //移动贴纸slot事件
        const val CLIP_STICKER_SLOT_EVENT = "clip_sticker_slot_event"                       //伸缩贴纸slot事件
        const val IS_COMPILING_VIDEO_EVENT = "is_compiling_video_event"                     //是否在合成视频事件
        const val RESET_COVER_EVENT = "reset_cover_event"                                   //重置视频封面事件
        const val DELETE_SLOT_EVENT = "delete_slot_event"                                   //slot删除事件
        const val SELECTED_NLE_TRACK_SLOT = "selected_nle_track_slot"                       //当前选中的视频片段
        //onActivityResult 标志位，用于处理seek逻辑
        const val ACTIVITY_RESULT_FLAG = "activity_result_flag"
        //保存compile发生时的位置
        const val PLAY_POSITION_WHEN_COMPILING = "play_position_when_compile"
        //记录离开编辑页时光标位置
        const val PLAY_POSITION_WHEN_EXIT_EDIT = "play_position_when_exit_edit"
        const val IS_PLAY_WHILE_EXIT_EDIT = "is_play_while_exit_edit"

        const val UPDATE_DRAW_STICK_POINT = "update_draw_stick_point" //更新bgm轨上的卡点标记
        const val SHOW_ADD_FUNCTION_ITEM = "show_add_function_item"
    }
}