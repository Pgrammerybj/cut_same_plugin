package com.angelstar.ola.player

import android.os.Bundle
import android.view.SurfaceView
import androidx.fragment.app.FragmentActivity
import com.bytedance.ies.nle.editor_jni.NLEEditorListener
import com.bytedance.ies.nle.editor_jni.NLEModel
import com.bytedance.ies.nle.editor_jni.TemplateInfo
import com.ss.ugc.android.editor.base.EditorSDK
import com.ss.ugc.android.editor.base.utils.runOnUiThread
import com.ss.ugc.android.editor.core.NLEEditorContext
import com.ss.ugc.android.editor.core.api.video.EditMedia
import com.ss.ugc.android.editor.core.utils.DLog
import com.ss.ugc.android.editor.core.vm.EditViewModelFactory
import com.ss.ugc.android.editor.main.EditorHelper
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/23 17:52
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
class TemplateActivityDelegate (
    private val activity: FragmentActivity,
    private val surfaceView: SurfaceView?
) : IPlayerActivityDelegate {

    companion object {
        const val TAG = "JackYang-EditorPageDelegate"
        const val CHOSE_VIDEO = 0
        const val SELECT_VIDEO_AUDIO = 1
        const val DRAFT_RESTORE = 2
        const val FILE_DRAFT = 100
    }


    private var type: Int = 0 // 0:默认 1:从拍摄进来 2:草稿
    private var mediaType: Int = 0 //代表从拍摄进来的资源类型 图片1 视频3
    private var videoPaths: List<String> = arrayListOf()
    private var hasLoaded = false
    private var nleModel: NLEModel? = null

    override var nleEditorContext: NLEEditorContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        nleEditorContext = EditViewModelFactory.viewModelProvider(activity).get(NLEEditorContext::class.java)
        nleEditorContext!!.nleEditor.addConsumer(nleEditorListener)  //往nleEditor中加入 监听器

        type = activity.intent.getIntExtra(EditorHelper.EXTRA_KEY_FROM_TYPE, 0)
        mediaType = activity.intent.getIntExtra(EditorHelper.EXTRA_KEY_MEDIA_TYPE, 0)
        videoPaths = activity.intent.getStringArrayListExtra(EditorHelper.EXTRA_KEY_VIDEO_PATHS)
            ?: arrayListOf()
        val costMill = measureTimeMillis {
            initImport()//1️⃣
        }
        DLog.d(TAG, "EditorActivityDelegate initImport costTime:$costMill")
    }

    //2️⃣
    private fun initImport() { // 判断是否从拍摄进来
        DLog.d(TAG, "是否从拍摄进来 jump type $type")
        if (type == SELECT_VIDEO_AUDIO) { //从拍摄进来
            nleEditorContext?.templateInfo = TemplateInfo()
            val select: MutableList<EditMedia> = ArrayList()
            videoPaths.forEach {
                select.add(EditMedia(it, mediaType == 3))
            }

            initEditor(select)
        }
    }

    //3️⃣
    private fun initEditor(select: MutableList<EditMedia>) {
        val rootPath = activity.filesDir.absolutePath
        if (surfaceView == null) {
            //日志上报
            return;
        }
        nleEditorContext!!.init(rootPath, surfaceView)
        nleEditorContext!!.videoEditor.importMedia(
            select,
            EditorSDK.instance.config.pictureTime,
            EditorSDK.instance.config.isFixedRatio
        )
        nleEditorContext!!.videoPlayer.seek(0)
        if (type == EditorHelper.EXTRA_FROM_MULTI_SELECT || type == CHOSE_VIDEO ||
            type == SELECT_VIDEO_AUDIO
        ) {
            // 如果是模板生产工具 默认静音所有视频
            val isAllMute = EditorSDK.instance.config.enableTemplateFunction
            nleEditorContext!!.oriAudioMuteEvent.postValue(isAllMute)
        }

        activity.lifecycle.addObserver(nleEditorContext!!)
    }

    //4️⃣
    private val nleEditorListener: NLEEditorListener = object : NLEEditorListener() {
        override fun onChanged() {
            val model = nleEditorContext!!.nleModel  //获取nle model
            if (model.tracks?.size == 0) {
                return
            }
            //日志耗时约50ms
            if (model?.stage != null) {
                nleModel = model
                runOnUiThread {
                    handleVEEditor()
                }
            }
        }
    }

    /**
     * 5️⃣ NLE数据驱动触发VEEditor
     */
    private fun handleVEEditor() {
        if (nleEditorContext != null) {
            nleEditorContext!!.videoPlayer.player!!.dataSource = nleEditorContext!!.nleModel
            if ((type == DRAFT_RESTORE || type == EditorHelper.EXTRA_FROM_TEMPLATE || type == FILE_DRAFT) && !hasLoaded) {
                val mainTrack = nleModel!!.getMainTrack()
                nleEditorContext!!.nleMainTrack = mainTrack!!
            }
            hasLoaded = true
        }
    }

    override fun onResume() {
        nleEditorContext?.apply {
            if (nleEditorContext?.nleModel?.stage != null) {
                videoPlayer.resume()
            }
        }
    }

    override fun onPause() {
        nleEditorContext!!.videoPlayer.posWhenEditorSwitch =
            nleEditorContext!!.videoPlayer.curPosition()
    }

    override fun play() {
        nleEditorContext!!.videoPlayer.play()
    }

    override fun pause() {
        nleEditorContext!!.videoPlayer.pause()
    }

    private fun getCurrentPosition(): Int {
        return nleEditorContext!!.videoPlayer.curPosition()
    }

    private fun getTotalDuration(): Int {
        return nleEditorContext!!.videoPlayer.totalDuration()
    }
}