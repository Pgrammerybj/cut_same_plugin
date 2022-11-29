package com.ola.editor.kit.cutsame.cut.videoedit.customview

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bytedance.ies.cutsame.util.VEUtils
import com.ola.editor.kit.cutsame.utils.FileUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.coroutines.CoroutineContext


class VideoFrameHelper(
    private val context: Context,
    private val path: String,
    private val refresh: () -> Unit
) : CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO

    companion object {
        const val TAG = "VideoFrameHelper"
        const val MAX_VE_COUNT = 1
    }

    private var currentVEGetFrameCount = 0
    @Volatile
    var stopGetFrame = false

    private var veLoadingFrame = CopyOnWriteArraySet<Int>()

    fun checkMemoryCache(startFrameNumber: Int, endFrameNumber: Int) {
        val ptsList = mutableListOf<Int>()
        for (i in startFrameNumber..endFrameNumber) {
            val cacheKey = LruFrameBitmapCache.getKey(
                path, i * TrackConfig.FRAME_DURATION
            )
            if (LruFrameBitmapCache.getBitmap(cacheKey) == null) {
                ptsList.add(i)
            }
        }
        if (ptsList.isNotEmpty()) {
            loadBitmapsFromDisk(ptsList)
        } else {
            refresh.invoke()
        }
    }

    private fun loadBitmapsFromDisk(ptsList: List<Int>) {
        if (!File(path).exists()) {

            return
        }

        launch(Dispatchers.IO) {
            val tempPtsList = mutableListOf<Int>()
            for (i in ptsList) {
                val key = LruFrameBitmapCache.getKey(
                    path, i * TrackConfig.FRAME_DURATION
                )
                val cacheFile = DiskCacheManager.get(key)
                if (cacheFile != null) {
                    val bitmap =
                        Glide.with(context)
                            .asBitmap()
                            .load(cacheFile)
                            .submit(TrackConfig.THUMB_WIDTH, TrackConfig.THUMB_WIDTH)
                            .get()
                    LruFrameBitmapCache.putBitmap(key, bitmap)
                } else {
                    if (!veLoadingFrame.contains(i * TrackConfig.FRAME_DURATION)) {
                        tempPtsList.add(i * TrackConfig.FRAME_DURATION)
                    }
                }
            }
            if (tempPtsList.isEmpty()) {
                launch(Dispatchers.Main) {
                    refresh.invoke()
                }
            } else {
                synchronized(currentVEGetFrameCount) {
                    if (currentVEGetFrameCount < MAX_VE_COUNT) {
                        currentVEGetFrameCount++
                        extractBitmapsFromVE(tempPtsList)
                        veLoadingFrame.addAll(tempPtsList)
                    }
                }
            }
        }
    }

    private fun extractBitmapsFromVE(ptsList: List<Int>) {
        var loadCount = 0
        VEUtils.getVideoFrames(
            path,
            ptsList.toIntArray(),
            TrackConfig.THUMB_WIDTH,
            -1,
            false
        ) { frame, width, height, ptsMs ->
            frame.let {
                val bitmap =
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(frame.position(0))
                val cacheKey =
                    LruFrameBitmapCache.getKey(
                        path,
                        ptsMs
                    )
                val clipBitmap =
                    Glide.with(context)
                        .asBitmap()
                        .load(bitmap)
                        .submit(TrackConfig.THUMB_WIDTH, TrackConfig.THUMB_WIDTH)
                        .get()
                loadCount++
                launch(Dispatchers.Main) {
                    LruFrameBitmapCache.putBitmap(
                        cacheKey, clipBitmap
                    )
                    if (loadCount < 10 || loadCount % 10 == 0 || loadCount == ptsList.size) {
                        refresh.invoke()
                    }
                }
                writeFileCache(cacheKey, clipBitmap)
                // 判断是否完成取帧或者停止取帧，完成取帧或停止取帧释放取帧 VE count
                synchronized(currentVEGetFrameCount) {
                    veLoadingFrame.remove(ptsMs)
                    if (loadCount == ptsList.size || stopGetFrame) {
                        currentVEGetFrameCount--
                    }
                }
            }
            !(loadCount == ptsList.size || stopGetFrame)
        }
    }

    private fun writeFileCache(cacheKey: String, bitmap: Bitmap) {
        try {
            DiskCacheManager.put(cacheKey, object : IWriter {
                override fun write(file: File): Boolean {
                    return FileUtil.saveBmpToFile(
                        bitmap,
                        file,
                        Bitmap.CompressFormat.JPEG
                    )
                }
            })
        } catch (e: Exception) {
        }
    }



    fun getPositionBitmap(position: Int, path: String): Bitmap? {
        var cacheKey: String?
        var tempPosition = position
        var repeatCount = 0
        var bitmap: Bitmap? = null
        var frameTime: Int
        // 最多回溯40张图片，后续需要减少取帧粒度
        while (bitmap == null && tempPosition >= 0 && repeatCount <= 40) {
            frameTime = calculateFrame(tempPosition)
            cacheKey = LruFrameBitmapCache.getKey(path, frameTime)
            bitmap = LruFrameBitmapCache.getBitmap(cacheKey)
            tempPosition -= 1
            repeatCount++
        }
        return bitmap
    }


    private fun calculateFrame(position: Int): Int =
        (position * TrackConfig.FRAME_DURATION)
}
