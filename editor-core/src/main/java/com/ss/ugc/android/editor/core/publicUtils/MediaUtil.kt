package com.ss.ugc.android.editor.core.publicUtils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.FileUtils
import android.text.TextUtils
import android.util.Log
import android.util.LruCache
import androidx.annotation.RequiresApi
import com.ss.android.ttve.nativePort.TEVideoUtils
import com.ss.android.vesdk.*
import com.ss.android.vesdk.filterparam.VECanvasFilterParam
import com.ss.ugc.android.editor.core.settings.KVSettingsManager
import com.ss.ugc.android.editor.core.settings.SettingsKey
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

object MediaUtil {

    // 未经修改的videoInfo cache
    private val REAL_VIDEO_INFO_CACHE = LruCache<String, VideoMetaDataInfo?>(100)
    private val AUDIO_INFO_CACHE = LruCache<String, AudioMetaDataInfo?>(100)

    const val EXPECT_HEIGHT_VALUE: Int = 1920
    const val EXPECT_WIDTH_VALUE: Int = 1920

    private const val TAG = "MediaUtil"
    private const val DEGREE_90 = 90
    private const val DEGREE_180 = 180
    private const val DEGREE_270 = 270
    const val IMAGE_TYPE = "image"

    // Used for VEEditor workspace
    private const val NLETempFolder = "NLETemp"

    private val RIFF_HEAD = byteArrayOf(0x52, 0x49, 0x46, 0x46)
    private val WEBP_HEAD = byteArrayOf(0x57, 0x45, 0x42, 0x50)
    private val HEIC_HEAD = byteArrayOf(0x68, 0x65, 0x69, 0x63)

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

    private val IMAGE_HEADERS = arrayOf(
        IMAGE_JPEG_HEAD,
        IMAGE_PNG_HEAD,
        IMAGE_GIF87A_HEAD,
        IMAGE_GIF89A_HEAD,
        IMAGE_BMP_HEAD
    )

    fun getExifOrientation(filepath: String): Int {
        var degree = 0
        if (TextUtils.isEmpty(filepath)) {
            return degree
        }

        if (!File(filepath).exists()) {
            return degree
        }

        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(filepath)
        } catch (ex: IOException) {
        }

        // We only recognize a subset of orientation tag values.
        degree = when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1) ?: -1) {
            ExifInterface.ORIENTATION_ROTATE_90 -> DEGREE_90

            ExifInterface.ORIENTATION_ROTATE_180 -> DEGREE_180

            ExifInterface.ORIENTATION_ROTATE_270 -> DEGREE_270

            else -> 0
        }
        return degree
    }

    fun isImage(path: String): Boolean {
        if (!File(path).exists()) return false
        try {
            val header = ByteArray(12)
            FileInputStream(path).use {
                it.read(header)
            }

            if (Arrays.equals(RIFF_HEAD, header.sliceArray(0 until 4)) &&
                Arrays.equals(WEBP_HEAD, header.sliceArray(8 until 12))
            ) {
                return true
            }

            for (i in IMAGE_HEADERS) {
                if (Arrays.equals(i, header.sliceArray(0 until i.size))) {
                    return true
                }
            }

            if (path.toUpperCase(Locale.getDefault()).endsWith(".HEIC") ||
                Arrays.equals(HEIC_HEAD, header.sliceArray(8 until 12))
            ) {
                return true
            }

            val headerStr = header.joinToString(" ") {
                it.toString(16).padStart(2, '0')
            }
        } catch (ignored: Exception) {
        }
        return false
    }

    /**
     * 获取真实信息
     * TODO: now can't support URL path, ex: content://
     */
    @Synchronized
    fun getRealVideoMetaDataInfo(path: String): VideoMetaDataInfo {
        val key = path + File(path).lastModified()
        var info = REAL_VIDEO_INFO_CACHE[key]
        if (info == null) {
            if (isImage(path)) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(path, options)
                val rotation = getExifOrientation(path)
                info = VideoMetaDataInfo(
                    path = path,
                    width = options.outWidth,
                    height = options.outHeight,
                    rotation = rotation,
                    duration = KVSettingsManager.get(SettingsKey.PICTURE_TRACK_TIME, 4000L).toInt(),
                    codecInfo = IMAGE_TYPE
                )
            } else {
                val veInfo = VEUtils.getVideoFileInfo(path)
                if (veInfo != null && veInfo.width > 0 && veInfo.height > 0 && veInfo.maxDuration > 0) {
                    info = VideoMetaDataInfo(
                        path = path,
                        width = veInfo.width,
                        height = veInfo.height,
                        rotation = veInfo.rotation,
                        duration = veInfo.maxDuration,
                        bitrate = veInfo.bitrate,
                        fps = veInfo.fps,
                        codecId = veInfo.codec,
                        videoDuration = veInfo.duration,
                        codecInfo = VEUtils.getVideoEncodeTypeByID(veInfo.codec)
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
            REAL_VIDEO_INFO_CACHE.put(key, info)
        }
        return info!!
    }

    @Synchronized
    fun removeRealVideoCache(path: String?) {
        path?.let {
            REAL_VIDEO_INFO_CACHE.remove(it)
        }
    }

    fun getVideoFileInfo(strInVideo: String):NLEVideoFileInfo? {
        val outInfo = IntArray(16)
        val ret = TEVideoUtils.getVideoFileInfo(strInVideo, outInfo)
        return if (ret == VEResult.TER_OK) {
            val info = NLEVideoFileInfo()
            info.width = outInfo[0]
            info.height = outInfo[1]
            info.rotation = outInfo[2]
            info.duration = outInfo[3]
            // 4 and 5 is deleted
            info.bitrate = outInfo[6]
            info.fps = outInfo[7]
            info.codec = outInfo[8]
            info.keyFrameCount = outInfo[9]
            info.maxDuration = outInfo[10]
            info
        } else {
            null
        }
    }

    // sampleRate, channelSize, sampleSize, duration
    fun getAudioMetaDataInfo(path: String): AudioMetaDataInfo {
        val getAudioInfo = {
            val veInfo = IntArray(10)
            val ret = VEUtils.getAudioFileInfo(path, veInfo)
            val audioInfo = if (path.isEmpty() || !File(path).exists()) {
                AudioMetaDataInfo(
                    0
                )
            } else if (ret == VEResult.TER_OK && veInfo[3] > 0) {
                AudioMetaDataInfo(
                    veInfo[3]
                )
            } else {
                val retriever = MediaMetadataRetriever()
                val duration = try {
                    retriever.setDataSource(path)

                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0
                } catch (e: Exception) {
                    0
                }
                AudioMetaDataInfo(
                    duration = duration
                )
            }
            AUDIO_INFO_CACHE.put(path, audioInfo)
            audioInfo
        }
        return AUDIO_INFO_CACHE[path] ?: getAudioInfo()
    }

    fun getVideoSize(videoPath: String): Pair<Int, Int> {
        val videoInfo = getRealVideoMetaDataInfo(videoPath)
        return getVideoSize(videoInfo)
    }

    fun getVideoSize(videoInfo: VideoMetaDataInfo): Pair<Int, Int> {
        var width = videoInfo.width
        var height = videoInfo.height
        if (videoInfo.rotation == 90 || videoInfo.rotation == 270) {
            width = videoInfo.height
            height = videoInfo.width
        }
        return Pair(width, height)
    }

    fun getAudioFileInfo(strInVideo: String): NLEAudioFileInfo? {
        val outInfo = IntArray(16)
        val ret = TEVideoUtils.getAudioFileInfo(strInVideo, outInfo)
        return if (ret == VEResult.TER_OK) {
            val info = NLEAudioFileInfo()
            info.sampleRate = outInfo[0]
            info.channelSize = outInfo[1]
            info.sampleFormat = outInfo[2]
            info.duration = outInfo[3]
            info
        } else {
            VELogUtil.e(TAG, "getAudioFileInfo error with code=$ret")
            null
        }
    }

    @JvmStatic
    fun isJpeg(path: String): Boolean {
        if (!File(path).exists()) return false
        try {
            val header = ByteArray(IMAGE_JPEG_HEAD.size)
            FileInputStream(path).use {
                it.read(header)
            }

            return Arrays.equals(header, IMAGE_JPEG_HEAD)
        } catch (ignored: Exception) {
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @JvmStatic
    fun convertImageToJpeg(imagePath: String, outputPath: String): Boolean {
        var result = false
        File(outputPath).let {
            if(it.exists()) it.deleteRecursively()
        }
        if(isJpeg(imagePath)) {
            FileOutputStream(outputPath).use { ofs ->
                FileInputStream(imagePath).use { ifs ->
                    try {
                        FileUtils.copy(ifs, ofs)
                        result = true
                    } catch (ignored: IOException) {
                    }
                }
            }
        } else {
            FileOutputStream(outputPath).use { ofs ->
                BitmapFactory.decodeFile(imagePath).also { bmp ->
                    result = bmp.compress(Bitmap.CompressFormat.JPEG, 100, ofs)
                }.recycle()
            }
        }
        return result
    }

    fun convertJpegToMp4(jpegPath: String, mp4Path: String, duration: Int, width: Int, height: Int, fps: Int): Int {
        return convertJpegToMp4WithVEEditor(jpegPath, mp4Path, duration, width, height, fps)

        // TODO(zhangkui): Use this API when VESDK fix the bugs of it.
        return VEUtils.convertJpegToMp4(jpegPath,  mp4Path, duration, false)
    }

    private fun convertJpegToMp4WithVEEditor(jpegPath: String, mp4Path: String, duration: Int, width: Int, height: Int, fps: Int): Int {
        if(!File(jpegPath).exists()) return -1
        // val ed = Environment.getExternalStorageDirectory()
        val tempDirectory = File(System.getProperty("java.io.tmpdir", ".")!! + File.separatorChar + NLETempFolder)
        if(!tempDirectory.exists()) tempDirectory.mkdir()
        var editor = VEEditor(tempDirectory.absolutePath)
        editor.setDestroyVersion(false)

        val size = 1
        val videoPaths = arrayOfNulls<String>(size)
        val vTrimIn = IntArray(size)
        val vTrimOut = IntArray(size)
        val speeds = FloatArray(size)

        for (i in 0 until size) {
            videoPaths[i] = jpegPath
            vTrimIn[i] = 0
            vTrimOut[i] = duration
            speeds[i] = 1.0f
        }

        val canvasFilterParam = VECanvasFilterParam()
        // canvasFilterParam.sourceType = VECanvasFilterParam.SourceType.COLOR.ordinal
        // For dev test
        // canvasFilterParam.color = Color.parseColor("#00ff00");
        canvasFilterParam.width = width
        canvasFilterParam.height = height

        val canvasFilterParams = arrayOfNulls<VECanvasFilterParam>(size)
        for (i in 0 until size) {
            canvasFilterParams[i] = canvasFilterParam
        }
        val result = editor.initWithCanvas(
            videoPaths, vTrimIn, vTrimOut,
            null,
            null, null, null,
            speeds, canvasFilterParams,
            VEEditor.VIDEO_RATIO.VIDEO_OUT_RATIO_ORIGINAL,
            VEEditor.VIDEO_GRAVITY.CENTER_IN_PARENT,
            VEEditor.VIDEO_SCALETYPE.CENTER)
        if(result != VEResult.TER_OK) return result

        val videoSettings = VEVideoEncodeSettings.Builder(VEVideoEncodeSettings.USAGE_COMPILE)
            // 使用 initwithcanvas 初始化时，想带上画布背景，必须设置4
            .setResizeMode(4)
            .setFps(fps)
            .setVideoRes(width, height)
            .build()

        //音频配置
        val audioSettings = VEAudioEncodeSettings.Builder()
            .setCodec(VEAudioEncodeSettings.ENCODE_STANDARD.ENCODE_STANDARD_AAC).Build()

        val isSuccess = editor.compile(
            mp4Path,
            null,
            videoSettings,
            audioSettings,
            object : VEListener.VEEditorCompileListener {
                override fun onCompileError(error: Int, ext: Int, f: Float, msg: String) {
                    Log.e(TAG, "JpegToMp4 compile error: $error ext: $ext f: $f msg: $msg")
                    // TODO(zhangkui): This may run several times.
                    editor.destroy()
                }

                override fun onCompileDone() {
                    Log.i(TAG, "JpegToMp4 compile success")
                    editor.destroy()
                }

                override fun onCompileProgress(progress: Float) {
                    Log.d(TAG, "JpegToMp4 compile progress: $progress")
                }
            })
        // TODO(zhangkui): Do we need to manually release VEEditor? If so, How?
        return if(isSuccess) 0 else -1

    }

}

class NLEVideoFileInfo {
    /**
     * 视频宽度
     */
    var width = 0

    /**
     * 视频高度
     */
    var height = 0

    /**
     * 视频旋转角度
     */
    var rotation = 0

    /**
     * 视频长度
     */
    var duration = 0

    /**
     * 码率 in (kbps)
     */
    var bitrate = 0

    /**
     * 帧率
     */
    var fps = 0

    /**
     * 视频编码ID
     * @see com.ss.android.vesdk.VEConstant.CODEC_ID
     */
    var codec = 0

    /**
     * 关键帧数量
     */
    var keyFrameCount = 0

    /**
     * 视频里所有轨道中的最大时长
     */
    var maxDuration = 0

    /**
     * 视频封装格式
     */
    var formatName: String? = null
}

fun imagePixelCount(filepath: String?): Int {

    if (TextUtils.isEmpty(filepath)) {
        return 0
    }
    val file = File(filepath)
    if (!file.exists()) {
        return 0
    }

    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(filepath, options)
    return options.outWidth * options.outHeight
}

fun videoFramePixelCount(filepath: String?): Int {
    if (TextUtils.isEmpty(filepath)) {
        return 0
    }

    val mmr = MediaMetadataRetriever()
    try {
        mmr.setDataSource(filepath)
        val widthString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: return 0
        val heightString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: return 0
        val width = widthString.toInt()
        val height = heightString.toInt()
        return width * height
    } catch (e: Exception) {
    } finally {
        mmr.release()
    }
    return 0
}

/**
 * rotate the material angle to 0
 * swap width height if angle is 90 or 270
 */
fun VideoMetaDataInfo.ignoreAngle(): VideoMetaDataInfo {
    return if ((rotation == 90 || rotation == 270)) {
        copy(width = height, height = width, rotation = 0)
    } else {
        copy(width = width, height = height, rotation = 0)
    }
}
