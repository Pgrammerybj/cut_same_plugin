package com.ss.ugc.android.editor.track.frame

private const val KEY_SHOW_CARTON_DIALOG_COUNT = "carton_dialog_count"
private const val KEY_SHOW_PROJECT_CARTON_DIALOG = "has_show_carton_dialog"

private const val SHOW_MAX_COUNT = 3
private const val CARTON_LOSS_FRAME_RATE = 0.2F

/**
 * detect the fluency of playing video, popup a dialog when the fluency is vary bad
 */
internal class VideoFluencyHelper {


    /** record latest lossFrameRate,show carton dialog when average of lossFrameRate is large then [CARTON_LOSS_FRAME_RATE] */
    private var preFrameRates = 0F
    private var latestFrameRates = 0F

    @Volatile
    private var stopped = false

    private var veFrameTaskExecuting: Boolean = false
    private var veFrameFetchingCountListener: ((executing: Boolean) -> Unit)? = null
    private val veFrameFetchingCountLock = Any()



    fun setVEFrameTaskExecuting(executing: Boolean) {
        synchronized(veFrameFetchingCountLock) {
            veFrameTaskExecuting = executing
            veFrameFetchingCountListener?.invoke(executing)
        }
    }

    fun getVEFrameTaskExecution(): Boolean = veFrameTaskExecuting

    private fun onCompressStart(preparedCallback: () -> Unit) {
        synchronized(veFrameFetchingCountLock) {
            if (!veFrameTaskExecuting) {
                preparedCallback.invoke()
            } else {
                veFrameFetchingCountListener = {
                    synchronized(veFrameFetchingCountLock) {
                        if (!veFrameTaskExecuting) {
                            preparedCallback.invoke()
                            veFrameFetchingCountListener = null
                        }
                    }
                }
            }
        }
    }




    interface CompressCallback {
        fun onSingleProgress(progress: Float)
        fun onSingleFinished()
        fun onFinished(result: Boolean)
    }
}
