package com.cutsame.ui.gallery.camera

import android.view.SurfaceView
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ss.android.vesdk.VERecorder
import com.vesdk.verecorder.record.demo.fragment.PreviewFragment
import com.vesdk.verecorder.record.preview.model.PreviewConfig
import com.vesdk.verecorder.record.preview.model.PreviewLifecycle

/**
 * 预览相机适配器，用于兼容IRecordCamera
 * @see IRecordCamera
 */
class RecordCameraAdapter : IRecordCamera, PreviewLifecycle {
    private var cameraFragment: PreviewFragment? = null
    private var cameraCallBack: IRecordCamera.CameraCallback? = null
    fun getCameraInstance(): Fragment {
        //ve预览配置
        val config = PreviewConfig.Builder()
            .uiStyle(PreviewConfig.Builder.UI_STYLE_CUT_SAME)
            .enableGl3(true)
            .enableRefactorRecorder(true)
            .enableEffectAmazing(true)
            .recordType(PreviewConfig.Builder.RECORD_TYPE_EFFECT)
            .enable3buffer(true) //开启3buffer
            .enableEffectRT(true)
            .stopPrePlay(true)
            .enableFollowShot(true)
            .saveAlbum(false)//pic don`t save to DCIM album
            .autoChangeDisplay(false)//不用自动切换尺寸，由外界控制
            .build()
        cameraFragment = PreviewFragment.getInstance(config)
        return cameraFragment!!.also {
            it.setPreviewLifecycle(this)
        }
    }

    fun onBackPressed(): Boolean {
        cameraFragment?.onBackPressed()
        return true
    }

    fun setCameraCallBack(callback: IRecordCamera.CameraCallback) {
        cameraCallBack = callback
    }

    /**
     * 更新素材约束条件
     */
    override fun updateConstraints(
        width: Int,
        height: Int,
        duration: Long,
        changeRenderSize: Boolean
    ) {
        cameraFragment?.updateCutSameConstraints(width, height, duration, changeRenderSize)
    }

    /**
     * 拍摄的视频/图片，确认保存时回调
     *
     * @param isVideo 是否视频
     */
    override fun onConfirmPreview(isVideo: Boolean) {
        cameraFragment?.onConfirmPreview(isVideo)
    }

    /**
     * 刷新槽位是否已满
     */
    override fun updatePickFull(isFull: Boolean) {
        cameraFragment?.updatePickFull(isFull)
    }

    /**-------相机生命周期回调代理开始--------**/
    /**
     * 初始化相机时回调
     */
    override fun onCameraInit(
        recorder: VERecorder,
        surfaceView: SurfaceView?,
        smallWindowContainer: ViewGroup
    ) {
        cameraCallBack?.onCameraInit(recorder, surfaceView, smallWindowContainer)
    }

    /**
     * 录制接口底层初始化时回调
     */
    override fun onRecorderNativeInit(ret: Int, msg: String) {
        cameraCallBack?.onRecorderNativeInit(ret, msg)
    }

    /**
     * 相机底栏面板展示变化回调
     */
    override fun onBottomPanelVisible(visible: Boolean, hasRecord: Boolean) {
        cameraCallBack?.onBottomPanelVisible(visible, hasRecord)
    }

    /**
     * 拍照或者录制回调
     * @return true-外界已处理结果；false-外界不处理拍照录制结果
     */
    override fun onShotOrRecord(path: String, isVideo: Boolean, duration: Long): Boolean? {
        return cameraCallBack?.onShotOrRecord(path, isVideo, duration) ?: false
    }

    /**
     * 录制状态变化时回调
     * @see RECORD_START
     * @see RECORD_PAUSE
     * @see RECORD_COMPLETE
     */
    override fun onRecordState(state: Int) {
        cameraCallBack?.onRecordState(state)
    }

    /**
     * 点击返回时回调
     * @return true-外界已处理结果；false-外界不处理该动作
     */
    override fun onHandleBack(): Boolean {
        return cameraCallBack?.onHandleBack() ?: false
    }

    /**
     * 视频视频时回调
     * @param complete false-开始合并，true-合并完成
     */
    override fun onContactVideo(complete: Boolean) {
        cameraCallBack?.onContactVideo(complete)
    }

    /**
     * 相机可见状态变化时回调
     */
    override fun onCameraHiddenChanged(hidden: Boolean) {
        cameraCallBack?.onCameraHiddenChanged(hidden)
    }

    /**-------相机生命周期回调代理结束--------**/


}