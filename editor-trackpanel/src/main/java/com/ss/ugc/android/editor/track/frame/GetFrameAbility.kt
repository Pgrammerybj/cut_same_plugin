package com.ss.ugc.android.editor.track.frame

import android.graphics.Bitmap
import android.util.Log
import com.ss.android.vesdk.VEUtils
import com.ss.ugc.android.editor.base.imageloder.ImageLoader
import com.ss.ugc.android.editor.base.logger.ILog
import com.ss.ugc.android.editor.track.TrackSdk
import java.nio.ByteBuffer

/**
 *  author : wenlongkai
 *  date : 2020/5/24 4:06 PM
 *  description :
 */
class GetFrameAbility {

    companion object {
        const val TAG = "GetFrameAbility"
    }

    fun getVideoBitmap(
        path: String,
        ptsMs: IntArray,
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
        isRough: Boolean,
        bitmapAvailable: VEBitmapAvailableListener
    ): Int {
        ILog.i(TAG, "GetFrameAbility start get frame")
        return VEUtils.getVideoFrames2(
            path,
            ptsMs,
            sourceWidth,
            sourceHeight,
            isRough
        ) { frame, width, height, timestamp ->
            val bitmap = frame?.let {
                decodeBitmap(it, path, width, height, targetWidth, targetHeight)
            }
            val continueGetFrame = bitmapAvailable.processBitmap(bitmap, timestamp)
            continueGetFrame
        }
    }

    private fun decodeBitmap(
        frame: ByteBuffer,
        path: String,
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        val bitmap = Bitmap.createBitmap(sourceWidth, sourceHeight, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(frame.position(0))
        Log.d(TAG,"decodeBitmap $sourceWidth // $sourceHeight // $targetWidth // $targetHeight" )

       val target =  ImageLoader.resizeBitmapSync(TrackSdk.application, bitmap,targetWidth, targetHeight)

       /* val target = Glide.with(TrackSdk.application).asBitmap()
            .apply(RequestOptions().centerCrop().skipMemoryCache(true).diskCacheStrategy(
                DiskCacheStrategy.NONE))
            .load(bitmap)
            .submit(targetWidth, targetHeight).get()*/
        return try {
            return  target
//            target.get(500, TimeUnit.MILLISECONDS)
        } catch (ignore: Exception) {
            ILog.e(TAG, "decode bitmap failed with path $path reason $ignore")
            null
        }
    }
}
