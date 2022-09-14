package com.ss.ugc.android.editor.track.frame

import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import android.widget.ImageView
import com.bytedance.ies.nle.editor_jni.NLESegmentVideo
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.android.vesdk.VEResult
import com.ss.ugc.android.editor.base.imageloder.ImageLoader
import com.ss.ugc.android.editor.base.imageloder.ImageOption
import com.ss.ugc.android.editor.base.logger.ILog
import com.ss.ugc.android.editor.base.utils.FileUtil
import com.ss.ugc.android.editor.base.utils.MediaUtil
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.diskcache.DiskCacheService
import com.ss.ugc.android.editor.track.diskcache.IWriter
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by biyun
 * on 2019-10-08.
 */
object FrameLoader {

    private const val TAG = "FrameLoader"

    const val CLIP_REVERSE_VIDEO_PATH = "clip_reverse_video_path"


    @Volatile
    var stopped = false

    suspend fun loadFrame(
        path: String,
        frames: List<PriorityFrame>,
        frameCacheSize: Size,
        callback: FrameCallback,
        statisticCallback: ((Boolean, Float) -> Unit)? = null
    ) {
        if (frames.isEmpty()) return
        val videoMetaDataInfo = MediaUtil.getRealVideoMetaDataInfo(path)
        val isRough = videoMetaDataInfo.keyFrameCount < videoMetaDataInfo.fps
        val first = frames.first()
        if (first.isImage) {

            frames.forEach {
                Log.d(
                    TAG,
                    "load image frame $path ${it.timestamp} ${frameCacheSize.width} ${frameCacheSize.height}"
                )
            }
            val target = ImageLoader.loadBitmapSync(TrackSdk.application, path, ImageOption.Builder().width(frameCacheSize.width).height(frameCacheSize.height).scaleType(ImageView.ScaleType.CENTER_CROP).build())

          /*  val target = Glide.with().asBitmap()
                .load(path).apply(
                    RequestOptions().skipMemoryCache(true).diskCacheStrategy(
                        DiskCacheStrategy.NONE
                    ).centerCrop()
                )
                .submit(frameCacheSize.width, frameCacheSize.height)*/
            try {
                target?.also {
                    writeToFileCache(getKey(first.path, first.timestamp), it)
                    callback.onCompleted(CacheKey(path, 0), it)
                }
            } catch (ignore: Exception) {
                ILog.e(TAG, "catch exception $ignore")
//                EnsureManager.ensureNotReachHere(ignore, path)
            }
        } else {
            frames.forEach {
                Log.d(
                    TAG,
                    "load video frame $path ${it.timestamp} ${frameCacheSize.width} ${frameCacheSize.height}"
                )
            }
            loadVideoFrame(
                path,
                frames.map { it.timestamp },
                frameCacheSize,
                callback,
                {
                    statisticCallback?.invoke(isRough, it)
                },
                isRough
            )
        }
    }

    private suspend fun loadVideoFrame(
        path: String,
        ptsMsList: List<Int>,
        cacheSize: Size,
        callback: FrameCallback,
        statisticCallback: ((Float) -> Unit),
        isRough: Boolean
    ) = suspendCoroutine<Any?> { con ->
        val retainedPts = ptsMsList.toMutableList()
        val size = MediaUtil.getSizeForThumbnail(path, cacheSize.width)
        var startTs = System.currentTimeMillis()
        var totalCost = 0L
        var cnt = 0L
        // todo 后面再补充，先跑起来
        val ret = LVEditAbility.getEditAbility().getVideoBitmap(path,
            ptsMsList.toIntArray(),
            size.width,
            size.height,
            cacheSize.width,
            cacheSize.height,
            isRough,
            object : VEBitmapAvailableListener {
                override fun processBitmap(
                    bitmap: Bitmap?,
                    timestamp: Int
                ): Boolean {
                    val endTs = System.currentTimeMillis()
                    totalCost += endTs - startTs
                    cnt++
                    val ave = totalCost.toFloat() / cnt.toFloat()
                    ILog.i(
                        TAG,
                        " isRough: $isRough cost: ${endTs - startTs} average: $ave ${ptsMsList.size}"
                    )
                    if (cnt >= ptsMsList.size) {
                        statisticCallback.invoke(ave)
                    }
                    startTs = System.currentTimeMillis()
                    bitmap?.let {
                        try {
                            ILog.d(TAG, "success to obtain bitmap with $path in $timestamp ")
                            writeToFileCache(getKey(path, timestamp), it)
                        } catch (exception: Exception) {
                            ILog.e(TAG,
                                "Disk cache exception ${exception.message}"
                            )
                        }
                        callback.onCompleted(CacheKey(path, timestamp), bitmap)
                    }?: ILog.e(TAG, "failed to obtain bitmap with $path in $timestamp ")
                    retainedPts.remove(timestamp)
                    val empty = retainedPts.isEmpty()
                    return (!empty && !stopped).also {
                        if (!it) con.resume(null)
                    }
                }
            })
        if (ret != VEResult.TER_OK) {
            ILog.e(TAG, " ret is $ret path is $path")
            con.resume(null)
        }
    }

    private fun writeToFileCache(key: String, bitmap: Bitmap) {
        DiskCacheService.put(key, object : IWriter {
            override fun write(file: File): Boolean {
                return FileUtil.saveBmpToFile(
                    bitmap,
                    file,
                    Bitmap.CompressFormat.JPEG
                )
            }
        })
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun getKey(path: String, timestamp: Int) = "$path#$timestamp"

    fun accurateToSecond(time: Int): Int {
        val roundTime = time / 1000 * 1000
        return if (roundTime + 500 < time) roundTime + 1000 else roundTime
    }

    fun getFrameLoadPath(slot: NLETrackSlot): @ParameterName(name = "path") String {
        val convertToSegmentVideo = NLESegmentVideo.dynamicCast(slot.mainSegment)
        var path = ""
        if (convertToSegmentVideo.rewind) {
            // 倒放
            path =  convertToSegmentVideo.reversedAVFile?.resourceFile?:""
        }
        return if (path.isNotEmpty()) path else convertToSegmentVideo.resource?.resourceFile?:""
    }

    /**
     * 降噪得到的视频跟这两个是一样的，所以没有必要再做额外的取帧，直接使用当前路径对应的帧即可
     */
//    fun getFrameLoadPath(segment: SegmentInfo): String {
//        return if (segment.reverse) segment.reversePath else segment.path
//    }
}

data class CacheKey(val path: String, val timestamp: Int)

data class PriorityFrame(
    val path: String,
    val timestamp: Int,
    val priority: Int,
    val isImage: Boolean
) {
    val key = CacheKey(path, timestamp)

    companion object {
        const val PRIORITY_UNKNOWN = -1
        const val PRIORITY_LOW = 0
        const val PRIORITY_MEDIUM = 1
        const val PRIORITY_HIGH = 2
        const val PRIORITY_SUPER = 3
    }
}

interface FrameCallback {
    fun onCompleted(key: CacheKey, b: Bitmap)
}
