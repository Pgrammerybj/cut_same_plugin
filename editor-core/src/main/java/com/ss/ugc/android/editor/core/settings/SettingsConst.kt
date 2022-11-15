package com.ss.ugc.android.editor.core.settings

import com.ss.ugc.android.editor.core.api.params.*

/**
 * @author: Jeff Beyond  <ouyanghaibing@bytedance.com>
 * @date: 2021/8/18
 */

enum class SettingsKey {
    ENABLE_LOG,  //日志开关
    ENABLE_DRAFT_BOX, //是否开启草稿箱
    IS_DEFAULT_SAVE_IN_ALBUM,  //默认保存在app内部存储空间，不保存在相册
    IS_LOOP_PLAY_VIDEO, //默认不循环播放视频
    FREEZE_FRAME_TIME, //定格时长，默认3s
    PICTURE_TRACK_TIME, //图片素材轨道默认4s
    ENABLE_EFFECT_AMAZING,//是否开启amazing引擎
    EFFECT_APPLY_GLOBAL, //特效默认作用对象 是否是全局
    FILE_PROVIDER_AUTHORITY,
    CUSTOM_CANVAS_RATIO_LIST, //自定义画布比例列表
    FIXED_CANVAS_RATIO,      //固定的画布比例
    WATER_MARK_PATH,  //自定义水印路径
    ENABLE_LOCAL_STICKER, //是否启用本地贴纸
    ENABLE_RECORDING_CHANGE_VOICE, //是否添加变声-录音功能
    DEFAULT_EDIT_MODE, //编辑模式
    LARK_EDIT_MODE, // 飞书编辑模式
    ENABLE_CLICK_WHEN_DISABLE_FUNCTION_ITEM,//当FunctionItem处于disable状态时，开启点击功能
    MAX_EDIT_VIDEO_LIMIT_TIME_MILLIS, //最长编辑视频的限制时间（毫秒）
    VIDEO_CLIP_MIN_DURATION, //视频轨允许裁剪的最小时长，小于会toast
    MAX_PIP_TRACK_COUNT, //画中画轨道最大数量
    ENABLE_TRACK_LABEL_SCROLLABLE, //轨道标签是否可滑动
    PLUGIN_MODE, // 插件化模式
    ENABLE_TEMPLATE_MODE, // 模板模式
    ENABLE_TOAST_WHEN_UNDO_REDO, // 撤销重做时是否弹Toast提示
    DEFAULT_FONT_PATH, // 默认字体路劲
}