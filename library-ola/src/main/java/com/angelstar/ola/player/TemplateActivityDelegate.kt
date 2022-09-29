package com.angelstar.ola.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import androidx.fragment.app.FragmentActivity
import com.angelstar.ola.ITemplateVideoStateListener
import com.bytedance.ies.nle.editor_jni.NLEEditorListener
import com.bytedance.ies.nle.editor_jni.NLEModel
import com.bytedance.ies.nle.editor_jni.TemplateInfo
import com.ss.ugc.android.editor.base.EditorSDK
import com.ss.ugc.android.editor.base.utils.FileUtil
import com.ss.ugc.android.editor.base.utils.runOnUiThread
import com.ss.ugc.android.editor.core.Constants
import com.ss.ugc.android.editor.core.NLEEditorContext
import com.ss.ugc.android.editor.core.api.video.EditMedia
import com.ss.ugc.android.editor.core.utils.DLog
import com.ss.ugc.android.editor.core.utils.LiveDataBus
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

    override var viewStateListener: ITemplateVideoStateListener? = null

    private val mHandler = Handler(Looper.getMainLooper())

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
        registerEvent()
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
            return
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
            if (model.stage != null) {
                nleModel = model
                runOnUiThread {
                    handleVEEditor()
                }
            }
        }
    }

    private var runnable: Runnable = object : Runnable {
        override fun run() {
            viewStateListener?.onPlayTimeChanged(
                FileUtil.stringForTime(getCurrentPosition()),
                FileUtil.stringForTime(getTotalDuration()),
                false
            )
            mHandler.postDelayed(this, 50)
        }
    }

    private fun registerEvent() {
        LiveDataBus.getInstance().with(Constants.KEY_MAIN, Int::class.java)
            .observe(activity, { position ->
                when (position) {
                    NLEEditorContext.STATE_PLAY -> { //代表播放
                        mHandler.removeCallbacks(runnable) // 防止多次调play的时候 有重复
                        nleEditorContext!!.stopTrack()
                        mHandler.post(runnable)
                        viewStateListener?.onPlayViewActivate(true)
                    }
                    NLEEditorContext.STATE_PAUSE -> { //代表暂停 统一处理ui状态
                        DLog.d("editorModel.STATE_PAUSE....")
                        mHandler.removeCallbacks(runnable)
                        viewStateListener?.onPlayViewActivate(false)
                        nleEditorContext!!.stopTrack()
                        viewStateListener?.onPlayTimeChanged(
                            FileUtil.stringForTime(getCurrentPosition()),
                            FileUtil.stringForTime(getTotalDuration()),
                            true
                        )
                        if (nleEditorContext!!.videoPlayer.isPlayingInFullScreen) {
                            nleEditorContext!!.videoPlayer.isPlayingInFullScreen = false
                        }
                        DLog.d("暂停了,当前播放位置：" + getCurrentPosition())
                    }
                    NLEEditorContext.STATE_BACK -> { // 点击关闭转场面板按钮
                        DLog.d("取消转场按钮的选中态和所选slot....")
                    }
                    NLEEditorContext.STATE_SEEK -> { // seek了
                        DLog.d("editorModel.STATE_SEEK....")
                        mHandler.removeCallbacks(runnable)
                        viewStateListener?.onPlayViewActivate(false)
                        nleEditorContext!!.stopTrack()
                        DLog.d("seek了: " + getCurrentPosition())
                        viewStateListener?.onPlayTimeChanged(
                            FileUtil.stringForTime(getCurrentPosition()),
                            FileUtil.stringForTime(getTotalDuration()),
                            false
                        )
                    }
                }
            })
    }

    /**
     * 5️⃣ NLE数据驱动触发VEEditor
     */
    private fun handleVEEditor() {
        if (nleEditorContext != null) {
            nleEditorContext!!.videoPlayer.player!!.dataSource = nleEditorContext!!.nleModel
            if ((type == DRAFT_RESTORE || type == EditorHelper.EXTRA_FROM_TEMPLATE || type == FILE_DRAFT) && !hasLoaded) {
                val mainTrack = nleModel!!.mainTrack
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

    override fun onDestroy() {
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun getCurrentPosition(): Int {
        return nleEditorContext!!.videoPlayer.curPosition()
    }

    private fun getTotalDuration(): Int {
        return nleEditorContext!!.videoPlayer.totalDuration()
    }
}