package com.cutsame.ui.gallery.camera

import android.view.SurfaceView
import android.view.ViewGroup
import com.ss.android.vesdk.VERecorder

/**
 * 剪同款所需要的相机接口
 */
interface IRecordCamera {

    /**
     * 更新素材约束条件
     */
    fun updateConstraints(width: Int, height: Int, duration: Long, init: Boolean)

    /**
     * 拍摄的视频/图片，确认保存时回调
     *
     * @param isVideo 是否视频
     */
    fun onConfirmPreview(isVideo: Boolean)

    /**
     * 刷新槽位是否已满
     */
    fun updatePickFull(isFull: Boolean)


    interface CameraCallback {
        /**
         * 初始化相机时回调
         */
        fun onCameraInit(
            recorder: VERecorder,
            surfaceView: SurfaceView?,
            smallWindowContainer: ViewGroup
        )

        /**
         * 录制接口底层初始化时回调
         */
        fun onRecorderNativeInit(ret: Int, msg: String)

        /**
         * 相机底栏面板展示变化回调
         */
        fun onBottomPanelVisible(visible: Boolean, hasRecord: Boolean)

        /**
         * 拍照或者录制回调
         * @return true-外界已处理结果；false-外界不处理拍照录制结果
         */
        fun onShotOrRecord(path: String, isVideo: Boolean, duration: Long): Boolean

        /**
         * 录制状态变化时回调
         * @see RECORD_START
         * @see RECORD_PAUSE
         * @see RECORD_COMPLETE
         */
        fun onRecordState(state: Int)

        /**
         * 点击返回时回调
         * @return true-外界已处理结果；false-外界不处理该动作
         */
        fun onHandleBack(): Boolean

        /**
         * 视频视频时回调
         * @param complete false-开始合并，true-合并完成
         */
        fun onContactVideo(complete: Boolean)

        /**
         * 相机可见状态变化时回调
         */
        fun onCameraHiddenChanged(hidden: Boolean)

        companion object {
            const val RECORD_START = 1
            const val RECORD_PAUSE = 2
            const val RECORD_COMPLETE = 3
            const val RECORD_CANCEL = 4
        }
    }
}