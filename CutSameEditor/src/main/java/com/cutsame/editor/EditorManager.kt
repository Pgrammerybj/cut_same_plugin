package com.cutsame.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.ss.ugc.android.editor.base.EditorConfig
import com.ss.ugc.android.editor.base.EditorSDK
import com.ss.ugc.android.editor.base.utils.ToastUtils
import com.ss.ugc.android.editor.base.view.export.WaitingDialog
import com.ss.ugc.android.editor.core.utils.DLog
import com.ss.ugc.android.editor.core.utils.Toaster
import com.ss.ugc.android.editor.main.EditorActivity
import com.ss.ugc.android.editor.main.EditorResCopyTask
import com.ss.ugc.android.editor.main.config.CKTobFunctionBarConfig
import com.ss.ugc.android.editor.resource.BuildInResourceProvider
import java.io.File

object EditorManager {
    private val TAG = "Ola-EditorManager"
    private var isCopying: Boolean = false

    fun initEditorRes(
        context: Context, action: () -> Unit
    ) {

        // 判断是不是有copy 资源
        if (isCopying) {
            Toaster.show(context.getString(R.string.cutsame_tips_resource_copying_to_sdcard))
            return
        }
        val resReady =
            context.getSharedPreferences(EditorSDK.EDITOR_SP, AppCompatActivity.MODE_PRIVATE)
                .getBoolean("copy", false)
        val isShowDialog = true
        if (!resReady) {
            val dialog = WaitingDialog(context)
            // 设置ProgressDialog 标题
            dialog.setTitle(context.getString(R.string.ck_tips_resource_copying))
            // 设置ProgressDialog 提示信息
            // 设置ProgressDialog 是否可以按退回按键取消
            dialog.setCancelable(false)
            dialog.setOnCancelListener { }
            var startTime = System.currentTimeMillis()
            EditorResCopyTask(context, object : EditorResCopyTask.IUnzipViewCallback {
                override fun onStartTask() {
                    Toaster.show("正在复制资源到 SD 卡，请稍后，复制完成后会自动跳转到编辑页面")
                    isCopying = true
                    if (isShowDialog) {
                        dialog.show()
                        dialog.setProgress(context.getString(R.string.cutsame_tips_resource_copying_to_sdcard))
                    }
                    Log.w(TAG, "onStartTask: 正在复制资源到 SD 卡，请稍后，复制完成后会自动跳转到编辑页面")

                    startTime = System.currentTimeMillis()

                }

                // 这里要注意一下，在子线程回调目前
                override fun onEndTask(result: Boolean) {
                    isCopying = false
                    if (isShowDialog) {
                        dialog.dismiss()
                    }
                    Log.w(TAG, "onStartTask: 复制资源到 SD 卡，结束，准备回调")

                    val endTime = System.currentTimeMillis()
                    DLog.d("copy cost ", "cost = ${endTime - startTime}")

                    if (result) {
                        context.getSharedPreferences(
                            EditorSDK.EDITOR_SP,
                            AppCompatActivity.MODE_PRIVATE
                        ).edit().putBoolean("copy", true).apply()
                        action.invoke()
                    }
                }
            }).execute(EditorResCopyTask.DIR, EditorResCopyTask.LOCAL_DIR)
        } else {
            action.invoke()
        }
    }


    fun initEditor(context: Context) {
        val compilerAction: EditorConfig.IVideoCompilerConfig =
            object : EditorConfig.IVideoCompilerConfig {
                override fun onVideoCompileIntercept(
                    duration: Long,
                    size: Long,
                    activity: Activity
                ): Boolean {
                    Log.d(TAG, "video compile before \$size")
                    return false
                }

                override fun onVideoCompileDone(path: String, activity: Activity) {
                    val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    intent.data = Uri.fromFile(File(path))
                    context.sendBroadcast(intent)
                    //调用系统播放器播放视频
                    previewExportVideo(path, context)
                    // 点击完成后保存到本地，同时存入草稿箱，提示toast文案“已保存到本地和草稿箱”
                    Log.d(TAG, "video compile done , just finish \$path")
                    if (activity is EditorActivity) {
//                    activity.saveDraft()
                        ToastUtils.show(context.getString(R.string.ck_has_saved_local_and_draft))
                    }
                    activity.finish()
                }

                override fun onCloseEdit(activity: Activity): Boolean {
                    Log.d(TAG, "now exit edit page")
                    return false
                }

                override fun onCustomCloseMethodIntercept(activity: Activity): Boolean {
                    Log.d(TAG, "custom close method")
                    // 业务方的代码逻辑
                    //
                    return false
                }

                override fun onEditResume(activity: Activity) {}
            }

        val waterPath =
            context.getExternalFilesDir("editor")!!.absolutePath.toString() + File.separator + "EditorResource" + File.separator + "watermark" + File.separator + "ve-watermark.png"
        val builder: EditorConfig = EditorConfig.Builder()
            .setLocalStickerEnable(true)
            .enableTemplateFunction(false)
            .enableLog(false)
            .resourceProvider(BuildInResourceProvider())
            .waterMarkPath(waterPath)
            .functionBarConfig(CKTobFunctionBarConfig())
            .imageLoader(GlideImageLoader())
            .jsonConverter(JSONConverterImpl())
            .enableDraftBox(false)
            .context(context)
            .compileActionConfig(compilerAction)
            .fileProviderAuthority(String.format(EditorActivity.URI_PREVIEW, context.packageName))
            .builder()
        EditorSDK.instance.init(builder)
    }

    fun previewExportVideo(exportFilePath: String?, context: Context) {
        val file = File(exportFilePath)
        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uriPreview = String.format(EditorActivity.URI_PREVIEW, context.packageName)
            FileProvider.getUriForFile(
                context,
                uriPreview,
                file
            )
        } else {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(uri, "video/*")
        context.startActivity(intent)
    }

}