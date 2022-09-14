package com.cutsame.ui.cut.videoedit.customview

import com.cutsame.ui.utils.SizeUtil


object TrackConfig {
    // 每一帧对应的时间
    var FRAME_DURATION = 1000 // ms

    var THUMB_WIDTH = SizeUtil.dp2px(30f)

    var THUMB_HEIGHT = SizeUtil.dp2px(56f)

    // 每个像素对应的时间
    val PX_MS: Float
        get() = THUMB_WIDTH / FRAME_DURATION.toFloat()
}
