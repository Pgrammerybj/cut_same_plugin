package com.cutsame.ui.gallery.camera

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.bytedance.ies.cutsame.veadapter.CompileListener
import com.bytedance.ies.nle.editor_jni.NLEModel
import com.cutsame.solution.CutSameSolution
import com.cutsame.solution.compile.CompileHelper
import com.cutsame.solution.player.ModelPlayer
import com.cutsame.solution.player.PlayerStateListener
import com.cutsame.ui.R
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.customwidget.LoadingDialog
import com.cutsame.ui.exten.FastMain
import com.cutsame.ui.utils.PrefUtils
import com.ss.android.ugc.cut_log.LogUtil
import com.ss.android.vesdk.VEVideoEncodeSettings
import kotlinx.android.synthetic.main.activity_video_preview.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * 拍摄后 视频、图片的效果预览页
 *
 */
class VideoPreviewActivity : AppCompatActivity() {
    companion object {
        const val TAG = "VideoPreviewActivity"
        private const val KEY_PREVIEW_RESULT = "PREVIEW_RESULT"
        private const val KEY_SP_SAVE_TO_ALBUM = "SP_SAVE_TO_ALBUM"

        /**
         * 开启模板槽位预览，必须通过此接口传递
         */
        fun startPreViewSlotForResult(
            activity: Activity,
            requestCode: Int,
            nleModel: NLEModel,
            data: VideoPreviewHelper.PreMediaData
        ) {
            VideoPreviewHelper.init(nleModel, data)//通过单例存储，避免nle过大的序列化问题
            activity.startActivityForResult(
                Intent(activity, VideoPreviewActivity::class.java),
                requestCode
            )
        }

        fun obtainResult(intent: Intent?): VideoPreviewHelper.PreMediaData? {
            return intent?.getSerializableExtra(KEY_PREVIEW_RESULT) as? VideoPreviewHelper.PreMediaData
        }
    }

    private var modelPlayer: ModelPlayer? = null
    private var loadingDialog: LoadingDialog? = null
    private var hasSetData = false
    private var hasRelease = false

    private val listener: PlayerStateListener = object : PlayerStateListener {
        override fun onChanged(state: Int) {
            when (state) {
                PlayerStateListener.PLAYER_STATE_PREPARED -> {
                    LogUtil.d(TAG, "PLAYER_STATE_PREPARED")
                    modelPlayer?.setLoopPlay(true)
                    modelPlayer?.start()
                }
            }
        }

        override fun onPlayProgress(process: Long) {}
        override fun onPlayEof() {}
        override fun onPlayError(what: Int, extra: String) {}
        override fun onFirstFrameRendered() {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_preview)
        modelPlayer = CutSameSolution.createModelPlayer(videoSurface)
        btn_single_play_back.setGlobalDebounceOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        tv_confirm_replace_material.setGlobalDebounceOnClickListener {
            confirmAndExit()
        }
        cb_confirm_save_material.isChecked = PrefUtils.getBoolean(this, KEY_SP_SAVE_TO_ALBUM, true)
        cb_confirm_save_material.setOnCheckedChangeListener { _, isChecked ->
            PrefUtils.putBoolean(this, KEY_SP_SAVE_TO_ALBUM, isChecked)
        }
    }

    private fun confirmAndExit() {
        if (!cb_confirm_save_material.isChecked) {
            onConfirm()
            return
        }
        CoroutineScope(FastMain).launch {
            modelPlayer?.let { modelPlayer ->
                showLoading(resources.getString(R.string.pick_record_loadding))
                val tempPath =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                        .path + File.separator + System.currentTimeMillis() + "_video.mp4"
                modelPlayer.compile(
                    tempPath,
                    generateCompileParam(),
                    object : CompileListener {
                        /**
                         * 合成错误
                         * 注意!!!  该接口在发生错误时可能会回调多次, 所以要在其中执行只需执行一次的收尾流程的话, 需要自行判断(因为回调消息队列里可能积留有多个错误)
                         */
                        override fun onCompileError(
                            error: Int,
                            ext: Int,
                            f: Float,
                            msg: String?
                        ) {
                            dismissLoading()
                            LogUtil.d(
                                TAG,
                                "onCompileError() error = $error, ext = $ext, f = $f, msg = $msg"
                            )
                        }

                        /**
                         * 合成结束
                         */
                        override fun onCompileDone() {
                            runOnUiThread {
                                dismissLoading()
                                notifyMedia(tempPath)
                                onConfirm()
                            }
                        }

                        /**
                         * 合成进度
                         * @param progress 进度
                         */
                        override fun onCompileProgress(progress: Float) {
                        }

                    })
            }
        }
    }

    private fun onConfirm() {
        setResult(RESULT_OK, Intent().apply {
            putExtra(KEY_PREVIEW_RESULT, VideoPreviewHelper.getPreviewData())
        })
        finish()

    }

    private fun generateCompileParam(): VEVideoEncodeSettings {
        val previewData = VideoPreviewHelper.getPreviewData()
        val width = previewData?.width ?: 100
        val height = previewData?.height ?: 100
        return VEVideoEncodeSettings.Builder(VEVideoEncodeSettings.USAGE_COMPILE)
            .setFps(30)
            .setBps(CompileHelper.calculatorBps(width, height))
            .setGopSize(35)
            .setSupportHwEnc(true)
            .setVideoRes(width, height)
            .build()
    }

    private fun notifyMedia(tempPath: String) {
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intent.data = Uri.fromFile(File(tempPath))
        sendBroadcast(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!hasSetData) {
            val model =
                NLEModel.dynamicCast(VideoPreviewHelper.getCacheNelModel()?.deepClone())
            modelPlayer?.playModel(model, listener)
            hasSetData = true
        } else {
            modelPlayer?.start()
        }
    }

    override fun onPause() {
        super.onPause()
        modelPlayer?.pause()
        if (isFinishing) {
            release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    private fun release() {
        if (hasRelease) {
            return
        }
        modelPlayer?.release()
        VideoPreviewHelper.release()
        hasRelease = true
    }

    private fun showLoading(msg: CharSequence? = null) {
        loadingDialog = LoadingDialog(this).apply {
            msg?.let { setMessage(it) }
            show()
        }
    }

    private fun dismissLoading() {
        runOnUiThread {
            loadingDialog?.dismiss()
        }
    }
}

