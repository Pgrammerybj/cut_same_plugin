package com.cutsame.ui.gallery.customview.utils

fun isVideoResizeToFitHeight(
    viewWidth: Int,
    viewHeight: Int,
    videoWidth: Int,
    videoHeight: Int
): Boolean {
    if (videoWidth == 0 || videoHeight == 0 || videoWidth == 0 || videoHeight == 0) {
        return true
    }
    //1.05代表视频比例与屏幕比例的一个宽容度，代表视频可拉伸的一个范围，在视觉效果上对用户是可接受的
    val videoRatio = videoHeight.toFloat() / videoWidth //视频高宽比
    val screenRatio = viewHeight.toFloat() / viewWidth //屏幕高宽比

    return videoRatio >= screenRatio
}

fun isVdeoFullTexture(viewWidth: Int, viewHeight: Int, videoWidth: Int, videoHeight: Int): Boolean {
    if (videoWidth == 0 || videoHeight == 0 || videoWidth == 0 || videoHeight == 0) {
        return true
    }
    val videoRatio = videoHeight.toFloat() / videoWidth //视频高宽比
    val screenRatio = viewHeight.toFloat() / viewWidth //屏幕高宽比
    return videoRatio < screenRatio
}
