package com.cutsame.ui.gallery.camera

import com.bytedance.ies.nle.editor_jni.NLEModel
import java.io.Serializable

/**
 * 视频模板预览帮助类
 */
object VideoPreviewHelper {
    private var nleModel: NLEModel? = null
    private var data: PreMediaData? = null
    fun getCacheNelModel(): NLEModel? {
        return nleModel
    }

    fun getPreviewData(): PreMediaData? {
        return data
    }


    fun init(nleModel: NLEModel?, data: PreMediaData) {
        VideoPreviewHelper.nleModel = nleModel
        VideoPreviewHelper.data = data
    }

    fun release() {
        nleModel = null
        data = null
    }

    class PreMediaData(
        var path: String,
        val width: Int,
        val height: Int,
        val isVideo: Boolean = false,
        val duration: Long = 0,
    ) : Serializable

}