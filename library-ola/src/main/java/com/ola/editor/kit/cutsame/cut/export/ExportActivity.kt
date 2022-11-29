package com.ola.editor.kit.cutsame.cut.export

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bytedance.ies.nle.editor_jni.INLEListenerCompile
import com.bytedance.ies.nle.editor_jni.NLEVideoEncodeSettings
import com.bytedance.ies.nle.editor_jni.PairIntInt
import com.ola.editor.kit.R
import com.ola.editor.kit.cutsame.CutSameUiIF
import com.ola.editor.kit.cutsame.utils.FileUtil
import com.ola.editor.kit.cutsame.utils.SizeUtil
import com.ss.android.ugc.cut_log.LogUtil
import com.ss.ugc.android.editor.core.NLEEditorContext
import com.ss.ugc.android.editor.core.NLEEditorInstance
import com.ss.ugc.android.editor.core.utils.DLog
import kotlinx.android.synthetic.main.activity_export.*
import java.text.SimpleDateFormat
import java.util.*

class ExportActivity : AppCompatActivity() {

    private val TAG = "OLA_ExportActivity"
    var nleEditorContext: NLEEditorContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        val value = NLEEditorInstance.mInstance().get(NLEEditorInstance.KEY_NLE_EDITOR_CONTEXT)
        if (value is NLEEditorContext) {
            nleEditorContext = value
        } else {
            LogUtil.e(TAG, "nleEditorContext isEmpty, finish")
            finish()
            return
        }
        setContentView(R.layout.activity_export)
        initView()
    }

    private fun initView() {
        val previewWidth = SizeUtil.dp2px(260f)
        val previewHeight = previewWidth * 16 / 9
        val imageViewLayoutParams = exportPreview.layoutParams as ConstraintLayout.LayoutParams
        imageViewLayoutParams.width = previewWidth
        imageViewLayoutParams.height = previewHeight
        exportPreview.layoutParams = imageViewLayoutParams

        getFirstFrameForThumbnail()
    }

    /**
     * 获取首帧缩略图
     */
    private fun getFirstFrameForThumbnail() {
        DLog.d(TAG, "Getting first frame")
        val coverPath = intent.getStringExtra(CutSameUiIF.ARG_CUT_COVER_URL)
        if (null != exportPreview && coverPath != null) {
            exportPreview.setImageFile(coverPath)
            exportPreview.showProgress(true)
            exportPreview.isRoundedCorners = true
            exportPreview.width = 8
            compileVideo()
        }
    }

    /**
     * Wiki:https://bytedance.feishu.cn/docx/RXaYd33pzoXy8rx7joEcHD1indg
     */
    private fun compileVideo() {
        DLog.d(TAG, "compile video ")
        nleEditorContext!!.player.pause()


        // 构建导出配置
        val compileSetting = NLEVideoEncodeSettings()
        compileSetting.outputSize = PairIntInt(1080, 720) // 导出视频分辨率，1080p
        val videoFile = FileUtil.getExternalExportVideoSavePath(getSaveFileName(), applicationContext)
        // 导出, videoFile为导出视频路径
        nleEditorContext!!.nleSession.exporterApi?.compile(
            videoFile,
            compileSetting,
            object : INLEListenerCompile {
                override fun onCompileError(
                    error: Int,
                    ext: Int,
                    f: Float,
                    msg: String?
                ) {
                    DLog.d(TAG, "onCompileError: $error, $ext")
                }

                override fun onCompileDone() {
                    DLog.d(TAG, "onCompileDone$videoFile")
                    Toast.makeText(baseContext, "Path:$videoFile", Toast.LENGTH_SHORT).show()
                    exportPreview.progress = 100.0
                }

                override fun onCompileProgress(progress: Float) {
                    DLog.d(TAG, "onCompileProgress $progress")
                    exportPreview.progress = progress * 100.0
                }

            })
    }


    private fun getSaveFileName() =
        "export_${SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())}.mp4"
}