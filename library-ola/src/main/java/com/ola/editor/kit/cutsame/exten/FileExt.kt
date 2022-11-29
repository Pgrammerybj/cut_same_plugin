package com.ola.editor.kit.cutsame.exten

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.ss.android.ugc.cut_log.LogUtil
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


fun File.moveTo(targetFile: File, overwrite: Boolean = false): Boolean {
    return kotlin.runCatching {
        if (this.renameTo(targetFile).not()) {
            val fileLength = this.length()
            this.copyTo(targetFile, overwrite).let {
                this.delete()
                it.exists() && it.length() == fileLength
            }
        } else true
    }.onFailure {
    }.getOrDefault(false)
}

@RequiresApi(Build.VERSION_CODES.Q)
fun File.copyToMediaDir(
    context: Context,
    isImage: Boolean,
    dir: String? = null,
    fileName: String? = null
): Boolean {
    var result = false
    val resolver = context.applicationContext.contentResolver
    val videoCollection = if (isImage)
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    else
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    val newVideoDetails = makeContentValues(isImage, fileName ?: this@copyToMediaDir.name, dir)

    val newVideoUri = resolver.insert(videoCollection, newVideoDetails)
    if (newVideoUri != null) {
        resolver.openFileDescriptor(newVideoUri, "w")?.use {
            var input: FileInputStream? = null
            var output: FileOutputStream? = null
            try {
                output = FileOutputStream(it.fileDescriptor)
                input = FileInputStream(this)
                input.copyTo(output)
                result = true
                output?.flush()
            } finally {
                input?.close()
                output?.close()
            }
        }
    } else {
        LogUtil.e("fileutils", "export file copy fail! source path: ${this.absolutePath} target path: $newVideoUri")
    }
    newVideoDetails.clear()
    newVideoDetails.put(MediaStore.Video.Media.IS_PENDING, 0)
    newVideoUri?.let { resolver.update(it, newVideoDetails, null, null) }

    return result
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun makeContentValues(isImage: Boolean, fileName: String, dir: String?): ContentValues {
    return if (isImage)
        ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.IS_PENDING, 1)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            dir?.let {
                put(MediaStore.MediaColumns.RELATIVE_PATH, it)
            }
        }
    else
        ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.IS_PENDING, 1)
            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            dir?.let {
                put(MediaStore.MediaColumns.RELATIVE_PATH, it)
            }
        }
}