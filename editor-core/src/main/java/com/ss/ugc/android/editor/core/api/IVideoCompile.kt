package com.ss.ugc.android.editor.core.api

/**
 * @desc: 视频合成&导出接口
 */
interface IVideoCompile {

    var exportFilePath: String

    /**
     * 视频合成前，返回视频评估的大小
     */
    fun calculateVideoSize(): Long

    fun compileVideo(exportVideoPath: String?, listener: IExportStateListener?): Boolean

    fun pause(): Int

    /**
     * 是否需要保持视频原始比例导出
     * 当导入的为横屏视频时，如果没有贴纸等超过横屏区域，应导出为横屏，否则导出竖屏
     */
    fun getExportRatio(): Float
}

/**
 * 导出视频状态监听
 */
interface IExportStateListener {
    /**
     * 导出错误
     *
     * 注意!!!  该接口在发生错误时可能会回调多次, 所以要在其中执行只需执行一次的收尾流程的话, 需要自行判断(因为回调消息队列里可能积留有多个错误)
     *
     */
    fun onExportError(error: Int, ext: Int, f: Float, msg: String?)

    /**
     * 导出结束
     */
    fun onExportDone()

    /**
     * 导出进度
     */
    fun onExportProgress(progress: Float)
}