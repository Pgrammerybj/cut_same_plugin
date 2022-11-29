package com.ola.editor.kit.player

import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import androidx.fragment.app.FragmentActivity
import com.bytedance.ies.nle.editor_jni.NLEEditorListener
import com.bytedance.ies.nle.editor_jni.NLEModel
import com.bytedance.ies.nle.editor_jni.NLESegmentVideo
import com.ola.editor.kit.cutsame.CutSameUiIF
import com.ola.editor.kit.cutsame.utils.FileUtil
import com.ola.editor.kit.interfaces.ITemplateVideoStateListener
import com.ss.ugc.android.editor.core.Constants
import com.ss.ugc.android.editor.core.Constants.Companion.STATE_PAUSE
import com.ss.ugc.android.editor.core.Constants.Companion.STATE_PLAY
import com.ss.ugc.android.editor.core.Constants.Companion.STATE_SEEK
import com.ss.ugc.android.editor.core.NLEEditorContext
import com.ss.ugc.android.editor.core.api.params.AudioParam
import com.ss.ugc.android.editor.core.api.params.EditMedia
import com.ss.ugc.android.editor.core.getNLEEditor
import com.ss.ugc.android.editor.core.publicUtils.MediaUtil
import com.ss.ugc.android.editor.core.utils.DLog
import com.ss.ugc.android.editor.core.utils.LiveDataBus
import com.ss.ugc.android.editor.core.utils.runOnUiThread
import com.ss.ugc.android.editor.core.vm.EditViewModelFactory
import java.io.File
import java.util.*

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/23 17:52
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
class TemplateActivityDelegate(
    private val activity: FragmentActivity,
    private val surfaceView: SurfaceView?,
    private val audioParam: AudioParam
) : IPlayerActivityDelegate {

    private var nleModel: NLEModel? = null

    override var nleEditorContext: NLEEditorContext? = null

    override var viewStateListener: ITemplateVideoStateListener? = null

    private val mHandler = Handler(Looper.getMainLooper())

    private var type: Int = 0 // 0:默认 1:从拍摄进来 2:草稿

    override fun onCreate() {
        type = activity.intent.getIntExtra(CutSameUiIF.ARG_DATA_MORE_EDITOR_FROM_BUSINESS, 0)
        nleEditorContext = EditViewModelFactory.viewModelProvider(activity).get(NLEEditorContext::class.java)
        nleEditorContext?.getNLEEditor()?.addConsumer(nleEditorListener)//往nleEditor中加入 监听器
        initNLEPlayer()//1️⃣
        registerEvent()
    }

//    private fun InitNleEditorContext(){
//
//    }

    /**
     * 2️⃣初始化播放器
     *
     */
    private fun initNLEPlayer() {
        val rootPath = activity.filesDir.absolutePath
        if (surfaceView != null) {
            nleEditorContext?.init(rootPath, surfaceView)
            activity.lifecycle.addObserver(nleEditorContext!!)
        }
    }

    override fun restoreDraftContent(draftModelPath: String) {
        if (draftModelPath.isNotBlank()) {
            val draftsFile = File(draftModelPath)
            if (draftsFile.exists()) {
                val draftString = draftsFile.readText()
                //恢复草稿
                nleEditorContext!!.restoreDraft(draftString)
                //NLEModel数据转换赋值
//                convertModel()
                //删除临时NLEModel草稿文件
                draftsFile.delete()
            }
        }
    }


    //3️⃣
    override fun importVideoMedia(filePathList: List<String>) {
        initNLEPlayer()
        val select: MutableList<EditMedia> = ArrayList()
        filePathList.forEach { select.add(EditMedia(it, true)) }
        //先清空主轨道
        nleEditorContext?.editor?.initMainTrack(select)
        //导入歌曲音频和歌词贴纸
//        nleEditorContext?.editor?.addAudioTrack(audioParam)
        nleEditorContext?.editor?.addLyricsStickerTrack(audioParam)
        //原预览视频静音
        nleEditorContext?.editor?.closeOriVolume()
        nleEditorContext?.player?.prepare()
    }

    //4️⃣
    private val nleEditorListener: NLEEditorListener = object : NLEEditorListener() {
        override fun onChanged() {
            val model = nleEditorContext?.nleModel  //获取nle model
            if (model?.tracks?.size == 0) {
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
     * NLEModel数据转换
     */
    private fun convertModel() {
        nleEditorContext?.nleModel?.let { nleModel ->
            nleModel.tracks.forEach { track ->
                track.slots.forEach { slot ->
                    //关键帧数据迁移到slot上
                    slot.keyframesUUIDList.forEach { keyFrameUUID ->
                        track.keyframeSlots.find { keyFrameUUID == it.uuid }?.let { keyframe ->
                            keyframe.startTime = keyframe.startTime - slot.startTime//时间坐标基于slot
                            slot.addKeyframe(keyframe)
                        }
                    }
                    slot.keyframesUUIDList.clear()//清空旧关键帧数据
                    NLESegmentVideo.dynamicCast(slot.mainSegment)?.let { segment ->
                        if (segment.avFile != null) {
                            val videoInfo = MediaUtil.getRealVideoMetaDataInfo(segment.avFile.resourceFile)
                            var width = videoInfo.width.toLong()
                            var height = videoInfo.height.toLong()
                            if (videoInfo.rotation == 90 || videoInfo.rotation == 270) {
                                width = videoInfo.height.toLong()
                                height = videoInfo.width.toLong()
                            }
                            segment.avFile.width = width
                            segment.avFile.height = height
                        }
                    }
                }
                track.clearKeyframeSlot()//清空旧关键帧数据
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
            mHandler.postDelayed(this, 100)
        }
    }

    private fun registerEvent() {
        LiveDataBus.getInstance().with(Constants.KEY_MAIN, Int::class.java)
            .observe(activity, { position ->
                when (position) {
                    STATE_PLAY -> { //代表播放
                        mHandler.removeCallbacks(runnable) // 防止多次调play的时候 有重复
                        nleEditorContext?.player?.isPlaying = true
                        mHandler.post(runnable)
                        viewStateListener?.onPlayViewActivate(true)
                    }
                    STATE_PAUSE -> { //代表暂停 统一处理ui状态
                        DLog.d("editorModel.STATE_PAUSE....")
                        mHandler.removeCallbacks(runnable)
                        viewStateListener?.onPlayViewActivate(false)
                        nleEditorContext?.player?.isPlaying = false
                        viewStateListener?.onPlayTimeChanged(
                            FileUtil.stringForTime(getCurrentPosition()),
                            FileUtil.stringForTime(getTotalDuration()),
                            true
                        )
                        DLog.d("暂停了,当前播放位置：" + getCurrentPosition())
                    }
                    STATE_SEEK -> { // seek了
                        DLog.d("editorModel.STATE_SEEK....")
                        mHandler.removeCallbacks(runnable)
                        viewStateListener?.onPlayViewActivate(false)
                        nleEditorContext?.player?.isPlaying = false
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
        nleEditorContext?.nleSession?.dataSource = nleEditorContext?.nleModel
    }

    override fun onResume() {
        nleEditorContext?.apply {
            if (nleEditorContext?.nleModel?.stage != null) {
                player.resume()
            }
        }
    }

    override fun onPause() {
        nleEditorContext?.player?.curPosition()
    }

    override fun play() {
        nleEditorContext?.player?.play()
    }

    override fun pause() {
        nleEditorContext?.player?.pause()
    }

    override fun onDestroy() {
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun getCurrentPosition(): Long {
        return nleEditorContext?.player?.curPosition()!!
    }

    private fun getTotalDuration(): Long {
        return nleEditorContext?.player?.totalDuration()!!
    }
}