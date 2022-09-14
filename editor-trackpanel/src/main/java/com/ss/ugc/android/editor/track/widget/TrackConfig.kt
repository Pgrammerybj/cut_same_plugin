package com.ss.ugc.android.editor.track.widget

import androidx.annotation.Px
import com.ss.ugc.android.editor.base.theme.ThemeStore
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.utils.PadUtil
import com.ss.ugc.android.editor.track.utils.SizeUtil

/**
 *  author : wenlongkai
 *  date : 2018/11/20 下午3:03
 *  description :
 */

object TrackConfig {
    const val DEFAULT_FRAME_DURATION = 1000
    val defaultItemFrameWidth = SizeUtil.dp2px(50F)
    // 每一帧对应的时间
    var FRAME_DURATION = DEFAULT_FRAME_DURATION // ms

    @Px
    var KEYFRAME_ICON_WIDTH = SizeUtil.dp2px(18F)

    val THUMB_WIDTH = if (ThemeStore.getCustomItemFrameWidth() != null)
        ThemeStore.getCustomItemFrameWidth()!!
    else
        defaultItemFrameWidth

    var THUMB_HEIGHT = if (ThemeStore.getCustomItemFrameHeight() != null)
        ThemeStore.getCustomItemFrameHeight()!!
    else
        THUMB_WIDTH

    val PX_MS: Float
        get() = THUMB_WIDTH / FRAME_DURATION.toFloat()

    var BORDER_WIDTH = SizeUtil.dp2px(20F)

//    var MUTE_WIDTH = ModuleCommon.application
//        .resources.getDimensionPixelSize(R.dimen.mute_video_voice_width)

    var ICON_WIDTH = SizeUtil.dp2px(20F)

    var LINE_WIDTH = SizeUtil.dp2px(1f)

    var TRANSITION_WIDTH = SizeUtil.dp2px(26f)
    var MUTE_WIDTH = SizeUtil.dp2px(50f)

    var DIVIDER_WIDTH = SizeUtil.dp2px(2f)

    const val MIN_AUDIO_DURATION = 100L

    val SUB_TRACK_HEIGHT = if (ThemeStore.getCustomViceTrackHeight() != null)
        ThemeStore.getCustomViceTrackHeight()!!
    else
        SizeUtil.dp2px(35F)


    // 主轨与副轨, 副轨与副轨之间的间距z
    val TRACK_MARGIN = if (PadUtil.isPad) SizeUtil.dp2px(8F) else SizeUtil.dp2px(6F)

    // 可视线与可视线之间的间距
    val LINE_MARGIN = if (PadUtil.isPad) SizeUtil.dp2px(4F) else SizeUtil.dp2px(3F)

    @Px
    val AUTO_SCROLL_SIZE = SizeUtil.dp2px(5F)

    @Px
    val AUTO_SCROLL_START_POSITION = (THUMB_WIDTH * 1.5).toInt()
    private const val AUTO_SCROLL_ACCELERATE_BASE = 4

    /**
     * 轨道裁剪在自动滚动时，计算自动滚动速率
     * 基本原理：手指距离屏幕边缘越近，滚动越快
     */
    fun calAutoScrollSpeedRate(touchRawX: Float, screenWidth: Int): Float {
        // 计算手指点击位置的x轴离屏幕左右边缘的最短距离
        val minDisX = if (touchRawX < screenWidth / 2) touchRawX else screenWidth - touchRawX

        return if (minDisX >= AUTO_SCROLL_START_POSITION)
            1.0f
        else
            1.0f + (AUTO_SCROLL_START_POSITION - minDisX) / AUTO_SCROLL_START_POSITION * AUTO_SCROLL_ACCELERATE_BASE
    }


    val PLAY_HEAD_POSITION = SizeUtil.getScreenWidth(TrackSdk.application)


}
