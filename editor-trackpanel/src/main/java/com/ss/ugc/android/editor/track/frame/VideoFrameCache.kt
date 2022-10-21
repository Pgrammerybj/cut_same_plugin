package com.ss.ugc.android.editor.track.frame

import android.graphics.Bitmap
import android.os.Looper
import android.util.Log
import android.util.Size
import android.widget.ImageView
import androidx.annotation.MainThread
import com.ss.ugc.android.editor.base.imageloder.ImageLoader
import com.ss.ugc.android.editor.base.imageloder.ImageOption
import com.ss.ugc.android.editor.base.logger.ILog
import com.ss.ugc.android.editor.track.BuildConfig
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.diskcache.DiskCacheService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.CoroutineContext

class VideoFrameCache(
    context: CoroutineContext,
    private val frameCacheSize: Size,
    private val veFrameTaskStateCallback: (executing: Boolean) -> Unit
) : CoroutineScope, FrameCallback {
    override val coroutineContext: CoroutineContext = context

    private val cache = MainThreadCache()

    private val requests = mutableListOf<FrameRequest>()
    private val fileTasks = LinkedList<LoadFileTask>()
    private var fileTaskCount = 0
    private val frameTasks = LinkedList<List<PriorityFrame>>()
    private var frameTaskCount = 0
        set(value) {
            field = value
            veFrameTaskStateCallback(value > 0)
        }

    /**
     * 保持数据同步，并且保证只执行最后一个操作：刷新 or 取消
     */
    private val mergeChannel = Channel<Operation>(4)

    /**
     * 不断地从mergeChannel中获取操作并执行
     */
    private val processChannel = Channel<Any>(1)

    init {
        CoroutineScope(Dispatchers.Default).launch {
            var lastOp: Operation? = null

            for (op in mergeChannel) {
//                TrackLog.d(TAG, "get op $op")
                when (op) {
                    is Refresh, is Cancel -> {
                        lastOp = op
                        processChannel.trySend(op).isSuccess
                    }
                    is Take -> {
                        op.response.complete(lastOp)
                        lastOp = null
                    }
                }
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            while (processChannel.iterator().hasNext()) {
//                TrackLog.d(TAG, "get one process task ")
                try {
                    val response = CompletableDeferred<Operation?>()
                    mergeChannel.send(Take(response))
                    when (val operation = response.await()) {
                        is Refresh -> {
//                            TrackLog.d(TAG, "get one refresh process task ")
                            FrameLoader.stopped = false
                            doRefresh(operation.onlyRefreshFile)
                        }
                        is Cancel -> {
//                            TrackLog.d(TAG, "get one cancel process task ")
                            FrameLoader.stopped = true
                            doCancel()
                        }
                    }
                } catch (ignore: Throwable) {
                    ILog.e(TAG, ignore.toString())
                }
            }
        }
    }

    fun cancel() {
        launch(Dispatchers.Main) {
            mergeChannel.send(Cancel())
        }
    }

    private fun doCancel() {
        synchronized(fileTasks) { fileTasks.clear() }
        synchronized(frameTasks) { frameTasks.clear() }
    }

    // when scrolling, just refresh from file
    fun refresh(onlyRefreshFile: Boolean = false) {
        launch(Dispatchers.Main) {
//            TrackLog.d(TAG, "public Refresh before ")
            mergeChannel.send(Refresh(onlyRefreshFile))
//            TrackLog.d(TAG, "public Refresh after ")

        }
    }

    private suspend fun doRefresh(onlyRefreshFile: Boolean = false) {
//        TrackLog.d(TAG, "private doRefresh")
        FrameLoader.stopped = onlyRefreshFile
        doCancel()

        val requestInfoList = mutableListOf<RequestInfo>()
        val requests = synchronized(requests) { requests.toList() }
        requests.forEach { request ->
            requestInfoList.add(request.getRequestFrames())
        }

        val noCache = cache.getNoCacheTask(requestInfoList)
        val loadFileTasks = mutableListOf<LoadFileTask>()
        val priorityFrameSetMap = mutableMapOf<PriorityFrameSetKey, MutableList<PriorityFrame>>()
        noCache.forEach {
            val fileCache = DiskCacheService.get(FrameLoader.getKey(it.path, it.timestamp))
            if (fileCache == null) {
                val key = PriorityFrameSetKey(it.path, it.priority)
                var list = priorityFrameSetMap[key]
                if (list == null) {
                    list = mutableListOf()
                    priorityFrameSetMap[key] = list
                }
                list.add(it)
            } else {
                loadFileTasks.add(LoadFileTask(it.key, fileCache.absolutePath, it.priority))
            }
        }

        val sortedFileTasks = loadFileTasks.sortedByDescending { it.priority }
        synchronized(fileTasks) {
            fileTasks.addAll(sortedFileTasks)
            if (fileTasks.isNotEmpty()) {
                while (fileTaskCount < MAX_FILE_COUNT) {
                    fileTaskCount++
                    executeLoadFromFile()
                }
            }
        }
        if (!onlyRefreshFile) {
            val sortedFrameTasks =
                priorityFrameSetMap.entries.sortedByDescending { it.key.priority }.map { it.value }
            synchronized(frameTasks) {
                frameTasks.addAll(sortedFrameTasks)
                if (frameTasks.isNotEmpty()) {
                    while (frameTaskCount < MAX_FRAME_COUNT) {
                        frameTaskCount++
                        executeLoadFromVideo()
                    }
                }
            }
        }
    }

    private fun executeLoadFromFile() = launch(Dispatchers.Default) {
        while (true) {
            val task = synchronized(fileTasks) {
                fileTasks.poll().also { if (it == null) fileTaskCount-- }
            } ?: break


            /*   val target =
                   Glide.with(TrackSdk.application).asBitmap()
   //                    .centerCrop()
                       .load(task.path).apply(RequestOptions().centerCrop().skipMemoryCache(true).diskCacheStrategy(
                           DiskCacheStrategy.NONE).format(DecodeFormat.PREFER_RGB_565))
                       .submit(frameCacheSize.width, frameCacheSize.height)*/
            try {
                val bitmap = ImageLoader.loadBitmapSync(
                    TrackSdk.application,
                    task.path,
                    ImageOption.Builder().width(frameCacheSize.width).height(frameCacheSize.height)
                        .skipMemoryCached(true).skipDiskCached(true).format(Bitmap.Config.RGB_565)
                        .scaleType(ImageView.ScaleType.CENTER_CROP).build()
                )
//                val bitmap = target.get(500, TimeUnit.MILLISECONDS)
                bitmap?.let {
                    onCompleted(task.key, it)
                }
            } catch (ignore: Exception) {
                ILog.e(TAG, "decode file cache failed: $task.path  $ignore")
            }
        }
    }

    override fun onCompleted(key: CacheKey, b: Bitmap) {
        launch(Dispatchers.Default) {
            cache.setBitmap(key, b)
            val requests = synchronized(requests) { requests.toList() }
            requests.forEach { it.onLoadFinished(key) }
        }
    }

    private fun executeLoadFromVideo() = launch(Dispatchers.Default) {
        val totalSize = frameTasks.size
        var cnt = 0
        var totalCost = 0.0f
        while (true) {
            val frames = synchronized(frameTasks) {
                frameTasks.poll().also { if (it == null) frameTaskCount-- }
            } ?: break

            val needLoadFrames = frames.filter {
                DiskCacheService.get(
                    FrameLoader.getKey(it.path, it.timestamp)
                ) == null
            }
            if (needLoadFrames.isNotEmpty()) {
                val path = frames.first().path
                cnt++
                FrameLoader.loadFrame(
                    path,
                    needLoadFrames,
                    frameCacheSize,
                    this@VideoFrameCache
                ) { isRough, ave ->
                    totalCost += ave
                    if (cnt >= totalSize) {
                        ILog.i(
                            TAG,
                            "isRough: $isRough, total average cost: ${totalCost / cnt.toFloat()}"
                        )
                        cnt = 0
                    }
                }
            }
        }
    }

    fun addRequest(request: FrameRequest) {
        launch(Dispatchers.Default) { synchronized(requests) { requests.add(request) } }
    }

    fun removeRequest(request: FrameRequest) {
        launch(Dispatchers.Default) { synchronized(requests) { requests.remove(request) } }
    }

    fun mainThreadGet(path: String, timestamp: Int): Bitmap? {
        val key = CacheKey(path, timestamp)
        val bitmap = cache.mainThreadGet(key)?.bitmap
        Log.d("bitmap-track", "path:$path timestamp $timestamp ${bitmap ?: "null "}")
        return bitmap
    }

    companion object {
        /**
         * 解码磁盘图片的最大线程数
         */
        private const val MAX_FILE_COUNT = 5

        /**
         * 调用VE接口取帧的最大线程数
         */
        private const val MAX_FRAME_COUNT = 1

        private const val TAG = "VideoFrameCache"
    }

    private interface Operation
    private data class Refresh(var onlyRefreshFile: Boolean) : Operation
    private class Cancel : Operation
    private class Take(val response: CompletableDeferred<Operation?>) : Operation
}

interface FrameRequest {
    fun getRequestFrames(): RequestInfo

    fun onLoadFinished(key: CacheKey)
}

data class RequestInfo(
    val removed: List<PriorityFrame>,
    val add: List<PriorityFrame>,
    val all: List<PriorityFrame>
)

private data class BitmapCache(var referenceCount: Int, var bitmap: Bitmap?)

private class LoadFileTask(val key: CacheKey, val path: String, val priority: Int)

private data class PriorityFrameSetKey(val path: String, val priority: Int)

private class MainThreadCache {
    private val cache = mutableMapOf<CacheKey, BitmapCache>()

    suspend fun setBitmap(key: CacheKey, bitmap: Bitmap) = withContext(Dispatchers.Main) {
        mainThreadGet(key)?.let { it.bitmap = bitmap }
    }

    @MainThread
    fun mainThreadGet(key: CacheKey): BitmapCache? {
        if (BuildConfig.DEBUG && Looper.getMainLooper() != Looper.myLooper()) {
            error("Only work in main thread")
        }
        return cache[key]
    }

    @Suppress("ComplexMethod")
    suspend fun getNoCacheTask(
        requestInfoList: List<RequestInfo>
    ) = withContext(Dispatchers.Main) {
        val priorityFrames = mutableMapOf<CacheKey, PriorityFrame>()
        requestInfoList.forEach { requestInfo ->
            requestInfo.add.forEach add@{
                val bitmapCache = cache[it.key]
                if (bitmapCache == null) {
                    cache[it.key] = BitmapCache(1, null)
                } else {
                    bitmapCache.referenceCount++
                }
            }
            requestInfo.removed.forEach removed@{
                val bitmapCache = cache[it.key] ?: return@removed
                bitmapCache.referenceCount--
                if (bitmapCache.referenceCount <= 0) {
                    cache.remove(it.key)
                    bitmapCache.referenceCount = 0
                }
            }
            requestInfo.all.forEach all@{
                if (cache[it.key] == null) cache[it.key] = BitmapCache(1, null)
                val frame = priorityFrames[it.key]
                if (frame == null || frame.priority < it.priority) {
                    priorityFrames[it.key] = it
                }
            }
        }

        val invalidCaches = mutableListOf<CacheKey>()
        val noCacheFrames = mutableListOf<PriorityFrame>()
        cache.forEach { (key, bitmapCache) ->
            val frame = priorityFrames[key]
            if (frame == null) {
                invalidCaches.add(key)
                return@forEach
            }
            if (bitmapCache.bitmap == null) noCacheFrames.add(frame)
        }
        invalidCaches.forEach { cache.remove(it) }

        noCacheFrames
    }
}
