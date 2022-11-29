package com.ola.editor.kit.cutsame

import android.app.Application
import java.io.File

object AppContext {
    lateinit var context: Application
    fun init(context: Application) {
        this.context = context
    }

    //视频帧缓存路径
    val videoFrameCacheFile: File?
        get() = if (this::context.isInitialized) {
            val externalFilesDir =
                context.getExternalFilesDir("cache") ?: File(context.filesDir.absolutePath, "cache")
            if (!externalFilesDir.exists()) {
                externalFilesDir.mkdirs()
            }
            externalFilesDir
        } else null

    val allFileCache: List<File>
        get() = mutableListOf<File>().apply {
            videoFrameCacheFile?.let {
                add(it)
            }
        }

}