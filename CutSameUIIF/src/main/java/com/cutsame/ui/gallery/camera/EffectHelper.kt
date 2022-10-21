package com.cutsame.ui.gallery.camera

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import androidx.lifecycle.Lifecycle
import com.bef.effectsdk.FileResourceFinder
import com.bytedance.ies.cutsame.util.Md5Utils
import com.bytedance.ies.nle.editor_jni.*
import com.bytedance.ugc.nlerecorder.videoPath
import com.bytedance.ugc.recorder.camera.CameraSetting
import com.bytedance.ugc.recorder.camera.ICameraSettting
import com.bytedance.ugc.recorder.template.BgAudioChain
import com.bytedance.ugc.recorder.template.ModelConvertor
import com.bytedance.ugc.recorder.template.VideoCoverChain
import com.bytedance.ugc.recorder.template.mutableId
import com.cutsame.solution.CutSameSolution
import com.cutsame.solution.source.PrepareSourceListener
import com.cutsame.solution.source.SourceInfo
import com.ss.android.ugc.cut_log.LogUtil
import com.ss.android.ugc.cut_ui.MediaItem
import com.ss.android.ugc.cut_ui.TextItem
import com.ss.android.vesdk.VERecorder
import com.ss.android.vesdk.VESDK
import com.vesdk.RecordInitHelper
import com.vesdk.vebase.resource.ResourceHelper
import com.vesdk.vebase.task.UnzipTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 效果帮助类
 * 用于解压ve内置素材、剪同款效果逻辑处理等
 */
class EffectHelper {
    companion object {
        const val TAG = "EffectHelper"
    }

    private var mCameraString: ICameraSettting? = null

    private lateinit var context: Context
    private var mTemplateModel: NLEModel? = null
    private var mSelectedMaterialId: String? = null
    var cutSameWidth = 0
    var cutSameHeight = 0
    var cutSameDuration = 3000L
    var mSelectedModel: NLEModel? = null

    @Volatile
    private var hasLoadEffect = false

    @Volatile
    private var hasLoadTemplate = false
    private var videoCache: String? = null


    /**
     * 初始化上下文
     */
    fun init(app: Context, videoCache: String) {
        RecordInitHelper.setApplicationContext(app)
        context = app
        this.videoCache = videoCache
    }

    /**
     * 相机初始化时，初始化NLERecorder
     */
    fun onCameraInit(recorder: VERecorder, lifecycle: Lifecycle, sfView: SurfaceView?) {
        mCameraString = CameraSetting(recorder)
        sfView?.let {
            mCameraString?.attachSurface(sfView, lifecycle)
        }
        //设置算法（人脸、手势、物体识别...）模型目录（素材提前解压）
        VESDK.setEffectResourceFinder(FileResourceFinder(ResourceHelper.getInstance().modelPath))
    }

    /**
     * 拉取模板的模板对应的资源
     *
     */
    fun loadTemplateRes(sourceInfo: SourceInfo, action: (isSuccess: Boolean) -> Unit) {
        Log.d(TAG, "loadTemplateRes templateUrl = ${sourceInfo.url} ")
        if (hasLoadEffect && hasLoadTemplate) {
            // 已经加载成功了
            action.invoke(true)
            checkSuccess(action)
        } else {
            GlobalScope.launch(Dispatchers.Main) {
                // 检测内置相机道具等解压状态
                if (!hasLoadEffect) {
                    loadInnerEffect(action)
                }
                // 拉取模板资源
                if (!hasLoadTemplate) {
                    loadTemplate(sourceInfo, action)

                }
            } // end for GlobalScope.
        }

    }

    private suspend fun preHanlder(temModel: NLEModel, md5Id: String): NLEModel =
        suspendCancellableCoroutine {
            val newModel = ModelConvertor.Builder(temModel)
                .addPrepareChain(
                    BgAudioChain(context = context, context.cacheDir.absolutePath, md5Id)
                )
                .addPrepareChain(VideoCoverChain(context))
                .execute()
            it.resume(newModel)
        }

    private fun loadTemplate(sourceInfo: SourceInfo, action: (isSuccess: Boolean) -> Unit) {
        sourceInfo.url?.let {
            //first get cache
            //don't need operation data， so get getOriModel.
            CutSameSolution.getCutSamePlayer(it)?.getOriModel()?.let { model->
                GlobalScope.launch {
                    onLoadModelSuccess(model, it, action)
                }
                return
            }
        }
        //prepare model
        val cutSameSource = CutSameSolution.createCutSameSource(sourceInfo)
        cutSameSource.prepareSource(object : PrepareSourceListener {
            override fun onProgress(progress: Float) {
                LogUtil.d(TAG, "loadTemplateRes onProgress $progress ")
            }

            override fun onSuccess(
                mediaItemList: ArrayList<MediaItem>?,
                textItemList: ArrayList<TextItem>?, model: NLEModel
            ) {
                LogUtil.d(TAG, "loadTemplateRes onSuccess ")
                GlobalScope.launch {
                    onLoadModelSuccess(model, sourceInfo.url, action)
                }
            }

            override fun onError(code: Int, message: String?) {
                LogUtil.d(TAG, "loadTemplateRes onError  code $code ,message  = $message")
            }
        })
    }

    private suspend fun onLoadModelSuccess(
        model: NLEModel,
        templateUrl: String,
        action: (isSuccess: Boolean) -> Unit
    ) {
        model.mutableId = Md5Utils.getMD5String(templateUrl)
        mTemplateModel = preHanlder(model, model.mutableId)
        LogUtil.d(TAG, "loadTemplateRes mSelectedMaterialId = $mSelectedMaterialId ")
        hasLoadTemplate = true
        checkSuccess(action)
    }

    private fun loadInnerEffect(action: (isSuccess: Boolean) -> Unit) {
        RecordInitHelper.initResource(object : UnzipTask.IUnzipViewCallback {
            override fun getContext(): Context {
                return this@EffectHelper.context
            }

            override fun onStartTask() {

            }

            override fun onEndTask(result: Boolean) {
                hasLoadEffect = true
                if (result) {
                    checkSuccess(action)
                } else {
                    action.invoke(false)
                }
            }
        })
    }

    private fun checkSuccess(action: (isSuccess: Boolean) -> Unit) {
        if (hasLoadEffect && hasLoadTemplate) {
            mSelectedMaterialId?.let { id ->
                val nleModel = mTemplateModel!!
                if (!videoCache.isNullOrBlank()) {
                    nleModel.videoPath = videoCache!!

                }
                findSelectSlot(id, nleModel)?.let { it ->
                    mSelectedModel = mCameraString?.switchSlot(context, it, nleModel, false)
                }
                action.invoke(true)
            }

        }
    }

    private fun findSelectSlot(metarialId: String, nleModel: NLEModel): NLETrackSlot? {
        var fisrSlot: NLETrackSlot? = null
        nleModel.tracks.filter { it.slots.size > 0 && it.trackType == NLETrackType.VIDEO }
            .forEach {
                it.sortedSlots.forEach { slot ->
                    NLESegmentVideo.dynamicCast(slot.mainSegment).let { segment ->
                        if (segment.mutableId == metarialId && segment.getExtra("is_mutable") == "true") {
                            return slot;
                        }
                    }
                }
            }
        return fisrSlot
    }

    fun switchMaterial(
        material: MediaItem,
        selectList: ArrayList<MediaItem>,
        changeModel: Boolean = true
    ) {
        val materialId = material.materialId
        Log.d(TAG, "switchMaterial  materialId= $materialId")
        cutSameWidth = material.width
        cutSameHeight = material.height
        cutSameDuration = material.duration
        if (mSelectedMaterialId != materialId) {
            mTemplateModel?.let { model ->
                findSelectSlot(materialId, model)?.let { slot ->
                    mSelectedModel =
                        mCameraString?.switchSlot(context, slot, model, false, needCommit = false)
                    fillMediaPath(mSelectedModel, selectList)?.let {//fill select path and render
                        if (changeModel) {
//                            GlobalScope.launch(Dispatchers.IO) {
                            mCameraString?.changeModel(it)
//                            }
                        }
                    }

                }

            }
            mSelectedMaterialId = materialId
        }

    }


    fun fillMatch(path: String): NLEModel? {
        if (!mSelectedMaterialId.isNullOrBlank() && !path.isNullOrBlank() && mSelectedModel != null) {
            return findSelectSlot(mSelectedMaterialId!!, mSelectedModel!!)?.run {
                NLESegmentVideo.dynamicCast(this.mainSegment)?.let {
                    it.avFile.resourceFile = path
                }
                mSelectedModel
            } ?: null
        }

        return null
    }

    /**
     * fill select path to nleModel
     */
    private fun fillMediaPath(nleModel: NLEModel?, selectList: ArrayList<MediaItem>): NLEModel? {
        nleModel?.let {
            selectList.forEach { mediaItem ->
                mediaItem.takeIf { it.source.isNotEmpty() }?.let {
                    //fill path to the corresponding slot
                    findSelectSlot(mediaItem.materialId, nleModel)?.let { slot ->
                        slot.mainSegment.resource.resourceFile = mediaItem.mediaSrcPath
                    }
                }
            }
        }
        return nleModel
    }


}