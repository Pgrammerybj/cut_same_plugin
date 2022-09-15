package com.angelstar.ola

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cutsame.editor.EditorManager
import com.cutsame.ui.CutSameUiIF
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.ss.ugc.android.editor.core.utils.Toaster
import com.vesdk.RecordInitHelper
import com.vesdk.vebase.task.UnzipTask

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.custom).setGlobalDebounceOnClickListener {
            //先解压强剪辑资源
            EditorManager.initEditorRes(this@MainActivity) {
                //解压拍照资源
                RecordInitHelper.setApplicationContext(applicationContext)
                RecordInitHelper.initResource(object : UnzipTask.IUnzipViewCallback {
                    override fun getContext(): Context {
                        return applicationContext
                    }

                    override fun onStartTask() {
                        runOnUiThread {
                            Toaster.show(context.getString(R.string.cutsame_tips_resource_copying_to_sdcard))
                        }
                    }

                    override fun onEndTask(result: Boolean) {
                        startActivity(CutSameUiIF.createTemplateUIIntent(this@MainActivity))
                    }

                })
            }
        }
    }
}