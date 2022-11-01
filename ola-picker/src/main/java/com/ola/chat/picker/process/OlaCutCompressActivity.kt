package com.ola.chat.picker.process

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cutsame.solution.CutSameSolution
import com.cutsame.solution.source.ComposeSourceListener
import com.cutsame.solution.source.CutSameSource
import com.ola.chat.picker.R
import com.ola.chat.picker.customview.LoadingProgressDialog
import com.ola.chat.picker.utils.PickerConstant
import com.ola.chat.picker.utils.postOnUiThread
import com.ola.chat.picker.utils.removeOnUiThread
import com.ss.android.ugc.cut_ui.MediaItem
import kotlinx.coroutines.launch
import java.lang.Runnable

private const val TAG = "cutui.Compress"

class OlaCutCompressActivity : AppCompatActivity() {
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

        val mediaItems = PickerConstant.getCompressDataByIntent(intent)
        val templateUrl = intent.getStringExtra(PickerConstant.ARG_CUT_TEMPLATE_URL)
        if (templateUrl.isNullOrEmpty()) {
            Log.e(TAG, "templateUrl isEmpty, finish")
            finish()
            return
        }

        cutSameSource = CutSameSolution.getCutSameSource(templateUrl)
        Log.d(TAG, "onCreate mediaItems=$mediaItems cutSameSource=$cutSameSource")
        lifecycleScope.launch {
            if(mediaItems!=null) {
                cutSameSource?.composeSource(mediaItems, object : ComposeSourceListener {
                    override fun onError(errorCode: Int, message: String?) {
                        Log.e(TAG,"compressSource Error: $message")
                        setResult(Activity.RESULT_CANCELED, Intent())
                        finish()
                    }

                    override fun onProgress(progress: Int) {
                        updateProgress(progress)
                    }

                    override fun onSuccess(mediaItems: ArrayList<MediaItem>?) {
                        Log.d(TAG,"compressSource onSuccess")
                        compressSuccess = true
                        val dataIntent = Intent().apply {
                            PickerConstant.setCompressResultData(this, mediaItems!!)
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
        Log.d(TAG,"cancelCompress compressSuccess: $compressSuccess")
        if(!compressSuccess) {
            cutSameSource?.cancelCompose()
            progressDialog.dismiss()
        }
    }

    private fun updateProgress(progress: Int) {
        if (currentProgress >= progress || progress > 100) {
            return
        }
        lifecycleScope.launch {
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
        progressDialog.dismiss()
        super.onDestroy()
    }

}