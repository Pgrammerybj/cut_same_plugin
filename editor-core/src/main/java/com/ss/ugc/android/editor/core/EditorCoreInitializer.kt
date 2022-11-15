package com.ss.ugc.android.editor.core

import android.content.Context
import android.text.TextUtils
import com.ss.ugc.android.editor.core.monitor.IMonitorService
import com.ss.ugc.android.editor.core.utils.AssetsUtils
import java.io.File
import java.util.Locale

class EditorCoreInitializer {

    companion object {

        @JvmStatic
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            EditorCoreInitializer()
        }
    }

    var appContext: Context? = null

    var resourcePath: String? = null

    var monitorService: IMonitorService? = null

//    var draftHelper: IDraftHelper? = null

    var draftDir: File? = null

    fun getWaterMarkPath(): String {
        resourcePath ?: throw IllegalStateException("ResourceRootPath is null.")
        // file/editor/EditorResource/Filter/
        return File(resourcePath, "watermark").absolutePath + File.separator + "ve-watermark.png"
    }

    fun getDefaultTextStyle(): String? {
        appContext ?: throw java.lang.IllegalStateException("appContext is null")
        val language = Locale.getDefault().language
        val filePath = if (!TextUtils.equals(language, "zh")) {
            "LocalResource/default.bundle/textStyleEn.json"
        } else {
            "LocalResource/default.bundle/textStyle.json"
        }
        return AssetsUtils.readFile(appContext, filePath)
    }
}