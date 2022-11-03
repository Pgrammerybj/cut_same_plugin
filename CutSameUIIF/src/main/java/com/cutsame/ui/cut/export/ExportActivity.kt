package com.cutsame.ui.cut.export

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bytedance.ies.cutsame.veadapter.CompileListener
import com.cutsame.solution.CutSameSolution
import com.cutsame.solution.compile.CompileParam
import com.cutsame.solution.compile.ExportResolution
import com.cutsame.solution.player.CutSamePlayer
import com.cutsame.solution.source.CutSameSource
import com.cutsame.ui.CutSameUiIF
import com.cutsame.ui.R
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.exten.*
import com.cutsame.ui.utils.FileUtil
import com.cutsame.ui.utils.MediaUtil
import com.cutsame.ui.utils.SizeUtil
import com.cutsame.ui.utils.showSuccessTipToast
import com.ss.android.ugc.cut_log.LogUtil
import kotlinx.android.synthetic.main.activity_export.*
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class ExportActivity : AppCompatActivity(), CoroutineScope {
    private val TAG = "OLA_ExportActivity"
    override val coroutineContext: CoroutineContext = FastMain + Job()
    private var cutSameSource: CutSameSource? = null
    private var cutSamePlayer: CutSamePlayer? = null
    private var exportFinish: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        val templateUrl = intent.getStringExtra(CutSameUiIF.ARG_CUT_TEMPLATE_URL)
        LogUtil.d(TAG, "onCreate, templateUrl=$templateUrl")
        if (templateUrl.isNullOrEmpty()) {
            LogUtil.e(TAG, "templateUrl isEmpty, finish")
            finish()
            return
        }

        // cutSameSource == null 则return
        cutSameSource = CutSameSolution.getCutSameSource(templateUrl)
        cutSamePlayer = CutSameSolution.getCutSamePlayer(templateUrl)
        LogUtil.d(TAG, "onCreate, cutSameSource=$cutSameSource")
        LogUtil.d(TAG, "onCreate, cutSamePlayer=$cutSamePlayer")
        setContentView(R.layout.activity_export)
        initView()
        initListener()
    }

    private fun initView() {
        val videoWidth = cutSamePlayer?.getConfigCanvasSize()?.width ?: 0
        val videoHeight = cutSamePlayer?.getConfigCanvasSize()?.height ?: 0
        LogUtil.d(TAG, "initView, videoWidth=$videoWidth,videoHeight=$videoHeight")
        if (videoWidth == 0 || videoHeight == 0) {
            finish()
            return
        }

        val previewWidth: Int
        val previewHeight: Int

        if (videoWidth >= videoHeight) {
            previewWidth = SizeUtil.dp2px(250f)
            previewHeight = videoHeight * previewWidth / videoWidth
        } else {
            previewWidth = SizeUtil.dp2px(260f)
            previewHeight = previewWidth * 16 / 9
        }
        val imageViewLayoutParams = exportPreview.layoutParams as ConstraintLayout.LayoutParams
        imageViewLayoutParams.width = previewWidth
        imageViewLayoutParams.height = previewHeight
        exportPreview.layoutParams = imageViewLayoutParams

        cutSamePlayer?.getSpecificImage(
            100,
            videoWidth,
            videoHeight
        ) { bitmap ->
            runOnUiThread {
                exportPreview.setImageBitmap(bitmap)
                exportPreview.showProgress(true)
                exportPreview.isRoundedCorners = true
                exportPreview.width = 8
                export()
            }
        }
    }

    private fun initListener() {
        exportFinishView.setGlobalDebounceOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        LogUtil.d(TAG, "onPause exportFinish: $exportFinish")
        if (!exportFinish) {
            cutSamePlayer?.cancelCompile()
            finish()
        }

    }

    private fun export() {
        LogUtil.d(TAG, "export")
        launch(FastMain) {
            var outputVideoPath: String
            var tryCount = 5
            var available: Boolean
            do {
                tryCount--
                outputVideoPath =
                    FileUtil.getExternalTmpSaveName(getSaveFileName(), this@ExportActivity)
            } while (File(outputVideoPath).exists().also { available = !it } && tryCount > 0)
            LogUtil.d(TAG, "outputVideoPath $outputVideoPath")
            if (available) {
                withContext(Dispatchers.IO) {
                    val param = createCompileParam()
                    cutSamePlayer?.compile(outputVideoPath, param, object : CompileListener {
                        override fun onCompileDone() {
                            //进度更新
                            exportPreview.progress = 100.0
                            LogUtil.d(TAG, "onCompileDone")
                            val srcFile = File(outputVideoPath)
                            var tarFilePath = ""
                            val isMoveSuccess =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    LogUtil.d(TAG, "is android 11")
                                    tarFilePath = "${MediaUtil.getSDPath()}/${
                                        MediaUtil.getMediaDirForAndroid11(this@ExportActivity)
                                    }/${srcFile.name}"
                                    var isRenameSuccess = false
                                    try {
                                        isRenameSuccess = srcFile.renameTo(File(tarFilePath))
                                    } catch (t: Throwable) {
                                        LogUtil.e(
                                            TAG,
                                            "doExport file rename fail! source path: ${srcFile.absolutePath} target path: $tarFilePath"
                                        )
                                    }
                                    val ret = if (!isRenameSuccess) {
                                        srcFile.copyToMediaDir(
                                            this@ExportActivity,
                                            false,
                                            MediaUtil.getMediaDirForAndroid11(this@ExportActivity)
                                        )
                                    } else true
                                    if (ret) {
                                        srcFile.delete()
                                    }
                                    ret
                                } else {
                                    val tarFilePath =
                                        MediaUtil.getNewSavePath(
                                            srcFile.name,
                                            this@ExportActivity
                                        )
                                    srcFile.moveTo(File(tarFilePath), false)
                                }

                            LogUtil.d(
                                TAG,
                                "onCompileDone MoveSuccess?${tarFilePath} $isMoveSuccess"
                            )
                            MediaUtil.notifyAlbum(this@ExportActivity, tarFilePath)
                            showFinishToast()
                            onExportFinish()
                        }

                        override fun onCompileProgress(progress: Float) {
                            Log.i(TAG, "onCompileProgress: $progress")
                            runOnUiThread {
                                exportPreview.progress = (progress * 100).toDouble()
                            }
                        }

                        override fun onCompileError(error: Int, ext: Int, f: Float, msg: String?) {
                            LogUtil.d(TAG, "onCompileError msg=$msg")
                        }
                    })

                }
            } else {
                LogUtil.e(TAG, "can not generate file for compiling")
            }
        }
    }

    private fun getSaveFileName() = "${
        SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
    }.mp4"

    fun onExportFinish() {
        runOnUiThread {
            exportTipView.hide()
            exportFinishView.show()
            exportFinish = true
        }
    }

    fun showFinishToast() {
        val text = this@ExportActivity.resources.getString(R.string.cutsame_export_save_album)
        showSuccessTipToast(this@ExportActivity, text)
    }

    private fun createCompileParam(): CompileParam {
        val supportHwEncoder = false
        val bps = 16 * 1024 * 1024
        val fps = 30
        val gopSize = 35
        return CompileParam(ExportResolution.V_1080P, supportHwEncoder, bps, fps, gopSize)
    }
}