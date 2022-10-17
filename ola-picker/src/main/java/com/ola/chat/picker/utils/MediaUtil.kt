package com.ola.chat.picker.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import com.ola.chat.picker.data.VideoMetaDataInfo
import java.io.File
import java.io.FileInputStream

object MediaUtil {
    
    const val TER_OK = 0

    // 未经修改的videoInfo cache
    private val realVideoInfoCache = LruCache<String, VideoMetaDataInfo?>(500)

    /**
     * 获取真实信息
     */
    @Synchronized
    fun getRealVideoMetaDataInfo(path: String): VideoMetaDataInfo {
        var info = realVideoInfoCache[path]
        if (info == null) {
            if (isImage(path)) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(path, options)
                val rotation = ExifUtils.getExifOrientation(path)
                val size: Size = options.run {
                    Size(outWidth, outHeight)
                }
                info = VideoMetaDataInfo(
                    path = path,
                    width = size.width,
                    height = size.height,
                    rotation = rotation,
                    duration = 60 * 1000, // MediaUtil.DEFAULT_DURATION
                    codecInfo = "image"
                )
            } else {
                val veInfo = IntArray(11)
                val ret = VEUtils.getVideoFileInfo(path, veInfo)
                if (ret == TER_OK &&
                    veInfo[0] > 0 && veInfo[1] > 0 && veInfo[10] > 0
                ) {
                    info = VideoMetaDataInfo(
                        path = path,
                        width = veInfo[0],
                        height = veInfo[1],
                        rotation = veInfo[2],
                        duration = veInfo[10],
                        longitude = veInfo[4],
                        latitude = veInfo[5],
                        bitrate = veInfo[6],
                        fps = veInfo[7],
                        codecId = veInfo[8],
                        videoDuration = veInfo[3],
                        codecInfo = VEUtils.getVideoEncodeTypeByID(veInfo[8]),
                        keyFrameCount = veInfo[9]
                    )
                } else {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(path)
                        val width = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                        )?.toInt() ?: 0
                        val height = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                        )?.toInt() ?: 0
                        val rotation = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                        )?.toInt() ?: 0
                        val duration = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_DURATION
                        )?.toInt() ?: 0
                        val bitrate = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_BITRATE
                        )?.toInt() ?: 0
                        val fps = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE
                        )?.toInt() ?: 0
                        val codecInfo = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                        ) ?: "unknown"

                        info = VideoMetaDataInfo(
                            path = path,
                            width = width,
                            height = height,
                            rotation = rotation,
                            duration = duration,
                            bitrate = bitrate,
                            fps = fps,
                            codecInfo = codecInfo
                        )
                    } catch (e: Exception) {
                        info = VideoMetaDataInfo(
                            path = path,
                            width = 0,
                            height = 0,
                            rotation = 0,
                            duration = 0
                        )
                    } finally {
                        retriever.release()
                    }
                }
            }
            realVideoInfoCache.put(path, info)
        }
        return info!!
    }

    private const val NOTIFY_ACTION = "android.intent.action.MEDIA_SCANNER_SCAN_FILE"
    private const val FILE_PROVIDER_AUTHORITY = "com.lemon.lv.provider"

    private val RIFF_HEAD = byteArrayOf(0x52, 0x49, 0x46, 0x46)
    private val WEBP_HEAD = byteArrayOf(0x57, 0x45, 0x42, 0x50)
    private val IMAGE_HEIC_HEAD = byteArrayOf(
        0x00, 0x00, 0x00,
        0x18, 0x66, 0x74,
        0x79, 0x70, 0x68,
        0x65, 0x69, 0x63
    )
    private val IMAGE_JPEG_HEAD = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
    private val IMAGE_PNG_HEAD = byteArrayOf(
        0x89.toByte(),
        0x50,
        0x4E,
        0x47,
        0x0D,
        0x0A,
        0x1A,
        0x0A
    )
    private val IMAGE_GIF87A_HEAD =
        byteArrayOf(0x47.toByte(), 0x49, 0x46, 0x38, 0x37, 0x61)

    private val IMAGE_GIF89A_HEAD =
        byteArrayOf(0x47.toByte(), 0x49, 0x46, 0x38, 0x39, 0x61)

    private val IMAGE_BMP_HEAD =
        byteArrayOf(0x42.toByte(), 0x4D)

    private val imageHeaders = arrayOf(
        IMAGE_JPEG_HEAD,
        IMAGE_PNG_HEAD,
        IMAGE_GIF87A_HEAD,
        IMAGE_GIF89A_HEAD,
        IMAGE_BMP_HEAD,
        IMAGE_HEIC_HEAD
    )

    private const val DEFAULT_DURATION = 3000

    // 保存完成后通知相册scan
    @WorkerThread
    fun notifyAlbum(
        context: Context,
        filePath: String,
        callback: (isSuccess: Boolean, msg: String) -> Unit
    ) {
        scanFileWithScanIntent(
            context,
            filePath,
            callback
        )
    }

    private fun scanFileWithScanIntent(
        context: Context,
        filePath: String,
        callback: (isSuccess: Boolean, msg: String) -> Unit
    ) {
        if (filePath.isBlank()) {
            callback.invoke(false, "the export file path is blank!")
            return
        }
        // android Q使用intent扫描方式，反而会扫描不出图片，改为使用MediaScanner
        if (Build.VERSION.SDK_INT >= 29) {
            scanFileWithMediaScanner(
                context,
                filePath,
                callback
            )
            return
        }
        insertVideoMediaToSysDB(
            context.contentResolver,
            filePath,
            callback
        )
        val scanIntent = Intent(NOTIFY_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            var uri: Uri? = null
            try {
                uri = FileProvider.getUriForFile(
                    context,
                    FILE_PROVIDER_AUTHORITY,
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
        scanFileWithMediaScanner(
            context,
            filePath,
            callback
        )
    }

    private fun scanFileWithMediaScanner(
        context: Context,
        filePath: String,
        callback: (isSuccess: Boolean, msg: String) -> Unit
    ) {
        val file = File(filePath)
        if (file.exists()) {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath), null
            ) { path, uri ->
                if (uri == null || uri.toString().isEmpty()) {
                    callback.invoke(
                        false,
                        "MediaScannerConnection.scanFile fail, uri: $uri path: $path "
                    )
                } else {
                    callback.invoke(true, "MediaScannerConnection.scanFile succeed.")
                }
            }
        } else {
            callback.invoke(false, "scan file, but the file is empty")
        }
    }

    private fun insertVideoMediaToSysDB(
        cr: ContentResolver,
        path: String,
        callback: (isSuccess: Boolean, msg: String) -> Unit
    ) {
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
        val durationMs =
            getDuration(path)
        values.put(MediaStore.Video.Media.DURATION, durationMs)

        try {
            val uri = cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null || uri.toString().isEmpty()) {
                callback.invoke(false, "insert video: $path fail! uri: $uri")
            } else {
                callback.invoke(true, "insert video succeed")
            }
        } catch (e: Exception) {
            callback.invoke(false, "ContentResolver insert video: $path fail! exception: $e")
        }
    }

    fun getDuration(path: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
        } catch (ignored: Throwable) {
            DEFAULT_DURATION.toLong()
        }
    }


    fun isImage(path: String): Boolean {
        if (!ExifUtils.isFileExist(path)) return false
        try {
            val header = ByteArray(12)
            FileInputStream(path).use {
                it.read(header)
            }

            if (RIFF_HEAD.contentEquals(header.sliceArray(0 until 4)) &&
                WEBP_HEAD.contentEquals(header.sliceArray(8 until 12))
            ) {
                return true
            }

            for (i in imageHeaders) {
                if (i.contentEquals(header.sliceArray(i.indices))) {
                    return true
                }
            }

            header.joinToString(" ") {
                it.toString(16).padStart(2, '0')
            }
        } catch (ignored: Exception) {
        }
        return false
    }
}





