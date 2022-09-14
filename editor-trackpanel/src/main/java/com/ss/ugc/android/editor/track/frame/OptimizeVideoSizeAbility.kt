package com.ss.ugc.android.editor.track.frame

import com.ss.ugc.android.editor.base.logger.ILog


/**
 *  author : wenlongkai
 *  date : 2020/5/24 4:07 PM
 *  description :
 */
class OptimizeVideoSizeAbility {

    companion object {
        const val TAG = "OptimizeVideoSizeAbility"
    }

    fun optimizeVideoSize(
        inputPath: String,
        outputPath: String,
        width: Int,
        height: Int,
        workSpacePath: String,
        outBps: Int?,
        onOptimizeListener: OnOptimizeListener?
    ) {
        ILog.i(TAG, " start optimize video size ")
//        VideoEditorUtils.optimizeVideoSize(
//            inputPath,
//            outputPath,
//            width,
//            height,
//            workSpacePath,
//            outBps,
//            onOptimizeListener
//        )
    }

    fun cancelResize() {
        ILog.i(TAG, " start cancel optimize video size ")
//        VideoEditorUtils.cancelResize()
    }
}
