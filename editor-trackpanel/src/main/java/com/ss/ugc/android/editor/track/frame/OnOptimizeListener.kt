package com.ss.ugc.android.editor.track.frame

interface OnOptimizeListener {
    /**
     * 开始转码
     */
    fun onStart()

    /**
     * 正在转码
     * @param progress 进度（0~1.0）
     */
    fun onProgress(progress: Float)

    /**
     * 转码完成
     * @param inputPath 输入的path
     * @param outputPath 输出的path
     */
    fun onSuccess(inputPath: String, outputPath: String)

    /**
     * 转码失败
     * @param inputPath 输入的path
     * @param outputPath 输出的path
     * @param errorInfo 错误信息
     */
    fun onError(inputPath: String, outputPath: String, errorInfo: String)

    /**
     * 转码cancel
     * @param inputPath 输入的path
     * @param outputPath 输出的path
     */
    fun onCancel(inputPath: String, outputPath: String)
}
