package com.ss.ugc.android.editor.track.utils

import android.graphics.Bitmap
import android.widget.ImageView
import com.bytedance.ies.nlemediajava.Log
import com.ss.ugc.android.editor.base.imageloder.ImageLoader
import com.ss.ugc.android.editor.base.imageloder.ImageOption
import com.ss.ugc.android.editor.base.logger.ILog
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.diskcache.DiskCacheService
import com.ss.ugc.android.editor.track.frame.FrameLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.math.log

object VideoFrameUtil : CoroutineScope {

    private const val TAG = "VideoFrameUtil"

    private const val DEFAULT_WIDTH = 150
    private const val DEFAULT_HEIGHT = 150

    override val coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    fun getVideoFrame(
        resourcePath: String,
        timeStamp: Int,
        isImage: Boolean,
        onCompleted: (videoFrame: Bitmap?) -> Unit
    ) {
        val keyTimeStamp = if (isImage) {
            0
        } else {
            (timeStamp / 1000) * 1000
        }
        val fileCache = DiskCacheService.get(FrameLoader.getKey(resourcePath, keyTimeStamp))
        fileCache?.let {
            try {
                launch(Dispatchers.IO) {
                    val bitmap = ImageLoader.loadBitmapSync(
                        TrackSdk.application,
                        fileCache.path,
                        ImageOption.Builder().width(DEFAULT_WIDTH).height(DEFAULT_HEIGHT)
                            .skipMemoryCached(true).skipDiskCached(true).format(Bitmap.Config.RGB_565)
                            .scaleType(ImageView.ScaleType.CENTER_CROP).build()
                    )
                    bitmap?.let {
                        val outPutBitmap = it.copy(it.config, true)
                        onCompleted(outPutBitmap)
                    }
                }
            } catch (ignore: Exception) {
                ILog.e(TAG, "decode file cache failed: $resourcePath  $ignore")
            }
        } ?: onCompleted(null) // 当缓存中没有想要的缓存帧
    }

}