package com.angelstar.ola

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cutsame.editor.EditorManager
import com.cutsame.ui.utils.runOnUiThread
import com.ss.ugc.android.editor.core.utils.Toaster
import com.vesdk.RecordInitHelper
import com.vesdk.vebase.task.UnzipTask

/**
 * @Author：yangbaojiang
 * @Date: 2022/11/1 10:52
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
object ResourceInitCheck {

    fun isSourceReady(activity: Activity) {
        //先解压强剪辑资源
        EditorManager.initEditorRes(activity) {
            //解压拍照资源
            RecordInitHelper.setApplicationContext(activity.applicationContext)
            RecordInitHelper.initResource(object : UnzipTask.IUnzipViewCallback {
                override fun getContext(): Context {
                    return activity.applicationContext
                }

                override fun onStartTask() {
                    runOnUiThread {
                        Toaster.show(context.getString(R.string.cutsame_tips_resource_copying_to_sdcard))
                    }
                }

                override fun onEndTask(result: Boolean) {
//                    Toaster.show("资源处理结束，onEndTask")
                    Log.i("ResourceInitCheck", "JackYang-onEndTask: 资源处理结束，onEndTask")
                    val intent = Intent("com.angelstar.ola.process.OlaTemplateFeedActivity")
                    intent.setPackage (activity.packageName)
                    activity.startActivity(intent)
                }
            })
        }
    }

}