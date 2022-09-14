package com.ss.ugc.android.editor.track.frame

import android.graphics.Bitmap

/**
 *  author : wenlongkai
 *  date : 2020/5/22 10:40 AM
 *  description : 需要跨进程提供的编辑能力
 */
interface IEditAbility {

    fun getVideoBitmap(
        path: String,
        ptsMs: IntArray,
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
        isRough: Boolean,
        bitmapAvailable: VEBitmapAvailableListener
    ): Int

    fun optimizeVideoSize(
        inputPath: String,
        outputPath: String,
        width: Int,
        height: Int,
        workSpacePath: String,
        outBps: Int?,
        onOptimizeListener: OnOptimizeListener?
    )

    fun getReverseVideo(
        reversePath: String,
        path: String,
        startTime: Int,
        endTime: Int,
        workSpacePath: String,
        onProgress: (Float) -> Unit,
        onResult: (Int) -> Unit
    )

    fun cancelResize()
}

interface VEBitmapAvailableListener {
    fun processBitmap(
        bitmap: Bitmap?,
        timestamp: Int
    ): Boolean
}
