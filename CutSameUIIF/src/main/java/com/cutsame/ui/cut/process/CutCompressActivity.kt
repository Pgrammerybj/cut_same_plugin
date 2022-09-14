package com.cutsame.ui.cut.process

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cutsame.solution.CutSameSolution
import com.cutsame.solution.source.ComposeSourceListener
import com.cutsame.solution.source.CutSameSource
import com.cutsame.ui.CutSameUiIF
import com.cutsame.ui.R
import com.cutsame.ui.customwidget.LoadingProgressDialog
import com.cutsame.ui.utils.postOnUiThread
import com.cutsame.ui.utils.removeOnUiThread
import com.cutsame.ui.exten.FastMain
import com.ss.android.ugc.cut_log.LogUtil
import com.ss.android.ugc.cut_ui.MediaItem
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import java.lang.Runnable

private const val TAG = "cutui.Compress"

class CutCompressActivity : AppCompatActivity(), CoroutineScope {
    override val coroutineContext: CoroutineContext = FastMain + Job()
    private lateinit var progressDialog: LoadingProgressDialog //压缩素材(包括素材到放过程)
    private var currentProgress: Int = 0
    private var cutSameSource: CutSameSource? = null
    private var compressSuccess: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        overridePendingTransition(0, 0)

        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        supportActionBar?.hide()

        initProgressDialog()

        val mediaItems = CutSameUiIF.getCompressDataByIntent(intent)
        val templateUrl = intent.getStringExtra(CutSameUiIF.ARG_CUT_TEMPLATE_URL)
        if (templateUrl.isNullOrEmpty()) {
            LogUtil.e(TAG, "templateUrl isEmpty, finish")
            finish()
            return
        }

        cutSameSource = CutSameSolution.getCutSameSource(templateUrl)
        LogUtil.d(TAG, "onCreate mediaItems=$mediaItems cutSameSource=$cutSameSource")
        launch {
            if(mediaItems!=null) {
                cutSameSource?.composeSource(mediaItems, object : ComposeSourceListener {
                    override fun onError(errorCode: Int, message: String?) {
                        LogUtil.e(TAG,"compressSource Error: $message")
                        setResult(Activity.RESULT_CANCELED, Intent())
                        finish()
                    }

                    override fun onProgress(progress: Int) {
                        updateProgress(progress)
                    }

                    override fun onSuccess(mediaItems: ArrayList<MediaItem>?) {
                        LogUtil.d(TAG,"compressSource onSuccess")
                        compressSuccess = true
                        val dataIntent = Intent().apply {
                            CutSameUiIF.setCompressResultData(this, mediaItems!!)
                        }
                        setResult(Activity.RESULT_OK, dataIntent)
                        finish()
                    }
                })
            }
        }

        nextFakeProgress()
    }

    override fun onResume() {
        super.onResume()
        progressDialog.show()
    }

    private fun initProgressDialog() {
        progressDialog = LoadingProgressDialog(this).apply {
            setCancelable(true)
            setMessage(getString(R.string.cutsame_compose_video_compose_loading))
            setProgress(0)
            setOnDismissListener {
                cancelCompress()
                onBackPressed()
            }
        }
    }


    private fun cancelCompress() {
        LogUtil.d(TAG,"cancelCompress compressSuccess: $compressSuccess")
        if(!compressSuccess) {
            cutSameSource?.cancelCompose()
            progressDialog.dismiss()
        }
    }

    private fun updateProgress(progress: Int) {
        if (currentProgress >= progress || progress > 100) {
            return
        }
        launch(FastMain) {
            progressDialog.setProgress(progress)
        }
        currentProgress = progress

        nextFakeProgress()
    }

    private val nextFakeProgressRun = Runnable {
        if (currentProgress >= 99) { // 假进度不能超过99
            return@Runnable
        }

        updateProgress(currentProgress + 1)
    }

    private fun nextFakeProgress() {
        removeOnUiThread(nextFakeProgressRun)
        postOnUiThread(1000, nextFakeProgressRun) // 1s至少跑1%
    }

    override fun onDestroy() {
        removeOnUiThread(nextFakeProgressRun)
        coroutineContext.cancel()
        progressDialog.dismiss()
        super.onDestroy()
    }

}