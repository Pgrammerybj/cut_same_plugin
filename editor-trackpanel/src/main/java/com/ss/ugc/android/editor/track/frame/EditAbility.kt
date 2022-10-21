package com.ss.ugc.android.editor.track.frame

import com.ss.ugc.android.editor.base.logger.ILog


/**
 *  author : wenlongkai
 *  date : 2020/5/22 10:36 AM
 *  description : 提供的编辑能力
 */
class EditAbility : IEditAbility {

    private val getFrameAbility by lazy {
        GetFrameAbility()
    }

    private val optimizeVideoSizeAbility by lazy {
        OptimizeVideoSizeAbility()
    }

    override fun getVideoBitmap(
        path: String,
        ptsMs: IntArray,
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
        isRough: Boolean,
        bitmapAvailable: VEBitmapAvailableListener
    ): Int {
        ILog.d("bitmap-track", "getVideoBitmap $path  $ptsMs.")
        return getFrameAbility.getVideoBitmap(
            path,
            ptsMs,
            sourceWidth,
            sourceHeight,
            targetWidth,
            targetHeight,
            isRough,
            bitmapAvailable
        )
    }

    override fun optimizeVideoSize(
        inputPath: String,
        outputPath: String,
        width: Int,
        height: Int,
        workSpacePath: String,
        outBps: Int?,
        onOptimizeListener: OnOptimizeListener?
    ) {
        optimizeVideoSizeAbility.optimizeVideoSize(
            inputPath,
            outputPath,
            width,
            height,
            workSpacePath,
            outBps,
            onOptimizeListener
        )
    }

    override fun getReverseVideo(
        reversePath: String,
        path: String,
        startTime: Int,
        endTime: Int,
        workSpacePath: String,
        onProgress: (Float) -> Unit,
        onResult: (Int) -> Unit
    ) {
        return
    }

    override fun cancelResize() {
        optimizeVideoSizeAbility.cancelResize()
    }
}
