package com.cutsame.ui.utils

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import java.io.File

object MediaUtil {
    private const val TAG = "MediaUtil"
    private const val NOTIFY_ACTION = "android.intent.action.MEDIA_SCANNER_SCAN_FILE"
    private const val DEFAULT_DURATION = 3000

    // 保存完成后通知相册scan
    @WorkerThread
    fun notifyAlbum(context: Context, filePath: String?) {
        filePath?.let {
            scanFileToMediaStore(context, it)
        }
    }

    private fun scanFileToMediaStore(context: Context, filePath: String) {
        scanFileWithScanIntent(context, filePath)
    }

    private fun scanFileWithScanIntent(context: Context?, filePath: String) {
        if (context == null) {
            return
        }
        if (filePath.isBlank()) {
            return
        }
        // android Q使用intent扫描方式，反而会扫描不出图片，改为使用MediaScanner
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            scanFileWithMediaScanner(context, filePath)
            return
        }
        insertMediaToSysDB(context.contentResolver, filePath)
        val scanIntent = Intent(NOTIFY_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            var uri: Uri? = null
            try {
                uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    File(filePath)
                )
            } catch (e: Exception) {
            }
            scanIntent.data = uri
        } else {
            scanIntent.data = Uri.fromFile(File(filePath))
        }
        // 添加这一句表示对目标应用临时授权该Uri所代表的文件
        scanIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.sendBroadcast(scanIntent)
        scanFileWithMediaScanner(context, filePath)
    }

    private fun scanFileWithMediaScanner(context: Context?, filePath: String) {
        if (context == null) {
            return
        }
        val file = File(filePath)
        if (file.exists()) {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath), null
            ) { path, uri ->  }
        } else {
        }
    }

    private fun insertMediaToSysDB(cr: ContentResolver, path: String) {
        insertVideoMediaToSysDB(cr, path)
    }

    private fun insertVideoMediaToSysDB(cr: ContentResolver, path: String) {
        val nowInMs = System.currentTimeMillis()
        val extension = "mp4"
        val nowInSec = nowInMs / 1000
        val file = File(path)
        val values = ContentValues()
        values.put(MediaStore.Video.Media.TITLE, File(path).name)
        values.put(MediaStore.Video.Media.DISPLAY_NAME, File(path).nameWithoutExtension)
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/$extension")
        values.put(MediaStore.Video.Media.DATE_TAKEN, nowInMs)
        values.put(MediaStore.Video.Media.DATE_MODIFIED, nowInSec)
        values.put(MediaStore.Video.Media.DATE_ADDED, nowInSec)
        values.put(MediaStore.Video.Media.DATA, path)
        values.put(MediaStore.Video.Media.SIZE, file.length())
        val durationMs = getDuration(path)
        values.put(MediaStore.Video.Media.DURATION, durationMs)

        try {
            cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        } catch (e: Exception) {
        }
    }

    private fun getDuration(path: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            retriever.extractMetadata(METADATA_KEY_DURATION)?.toLong() ?: DEFAULT_DURATION.toLong()
        } catch (ignored: Throwable) {
            DEFAULT_DURATION.toLong()
        }
    }

    fun getNewSavePath(name: String,context: Context): String {
        val cameraDir = File(getMediaDir(context))
        if (getNoMediaPath(cameraDir) == null && cameraDir.parentFile?.let {
                getNoMediaPath(it)
            } == null) {
            return cameraDir.absolutePath + File.separatorChar + name
        }
        val pictureDir = File("${getSDPath()}/${Environment.DIRECTORY_PICTURES}")
        if (pictureDir.exists() && getNoMediaPath(pictureDir) == null) {
            val lvDir = File(pictureDir, "lv")
            if (!lvDir.exists()) {
                lvDir.mkdirs()
            }
            return lvDir.absolutePath + File.separatorChar + name
        }
        val moviesDir = File("${getSDPath()}/${Environment.DIRECTORY_MOVIES}/lv")
        if (!moviesDir.exists()) {
            moviesDir.mkdirs()
        }
        return moviesDir.absolutePath + File.separatorChar + name
    }

    fun getMediaDirForAndroid11(context: Context): String {
        val cameraDir = File(getMediaDir(context))
        if (getNoMediaPath(cameraDir) == null && cameraDir.parentFile?.let {
                getNoMediaPath(it)
            } == null
        ) {
            return "${Environment.DIRECTORY_DCIM}/Camera"
        }
        val pictureDir = File("${getSDPath()}/${Environment.DIRECTORY_PICTURES}")
        if (pictureDir.exists() && getNoMediaPath(pictureDir) == null) {
            return Environment.DIRECTORY_PICTURES
        }
        return Environment.DIRECTORY_MOVIES
    }

    fun getSDPath(): String {
        var sdDir: File? = null
        // 判断sd卡是否存在
        val sdCardExist = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory() // 获取根目录
        }
        return sdDir?.toString() ?: ""
    }

    private fun getMediaDir(context: Context): String {
        val sdcardPath = getSDPath()
        val mediaDir = getSpecialCameraPath(context).ifBlank { "$sdcardPath/相机" }
        val xjDir = File(mediaDir)
        return xjDir.absolutePath
    }


    private val OEM_SYSTEM_PKGS = arrayOf(
        "com.htc", "com.meizu.mstore",
        "com.sonyericsson.android.camera", "com.yulong.android.settings.backup",
        "com.bbk.account", "com.gionee.account"
    )

    private fun getSpecialCameraPath(context: Context): String {
        val sdcardPath = getSDPath()
        var path = ""
        var pkgIndex = 0
        var file: File
        for (i in 1 until OEM_SYSTEM_PKGS.size) {
            try {
                if (context.packageManager.getPackageInfo(OEM_SYSTEM_PKGS[i], 0) != null) {
                    pkgIndex = i
                    break
                }
            } catch (e: Throwable) {
            }
        }

        when (pkgIndex) {
            0 -> {
                file = File(sdcardPath, "/DCIM/100MEDIA")
                if (file.exists()) {
                    path = file.absolutePath
                }
            }
            1 -> {
                file = File(sdcardPath, "/Camera")
                if (file.exists()) {
                    path = file.absolutePath
                } else {
                    file = File(sdcardPath, "/DCIM")
                    if (file.exists()) {
                        path = file.absolutePath
                    }
                }
            }
            2 -> {
                file = File(sdcardPath, "/DCIM/100ANDRO")
                if (file.exists()) {
                    path = file.absolutePath
                }
            }
            3 -> {
                file = File(sdcardPath, "/Camera")
                if (file.exists()) {
                    path = file.absolutePath
                }
            }
            4 -> {
                file = File(sdcardPath, "/相机")
                if (file.exists()) {
                    path = file.absolutePath
                } else {
                    file = File(sdcardPath, "/相机/照片")
                    if (file.exists()) {
                        path = file.absolutePath
                    }
                }
            }
            5 -> {
                file = File(sdcardPath, "/照相机/Camera")
                if (file.exists()) {
                    path = file.absolutePath
                }
            }
            else -> {
                path = ""
            }
        }
        return path
    }

    private fun getNoMediaPath(dir: File): String? {
        if (dir.isDirectory) {
            return if (File(dir, ".nomedia").exists()) {
                File(dir, ".nomedia").absolutePath
            } else {
                null
            }
        }
        return null
    }
}
