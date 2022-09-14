package com.cutsame.ui.cut.videoedit.customview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.annotation.MainThread
import com.cutsame.ui.utils.SizeUtil
import java.io.File

class ItemFrameView : View {

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
        super(context, attrs, defStyleAttr)

    companion object {
        const val TAG = "ItemFrameView"
    }

    private var trackWidth = SizeUtil.dp2px(50F)
    private var screenWidth = SizeUtil.getScreenWidth(context)

    private var duration = 0L

    private var framesCount = 0

    private var videoFrameHelper: VideoFrameHelper? = null

    private var startFrameNumber = 0
    private var endFrameNumber = 0
    private var leftRefreshThreshold = -1
    private var rightRefreshThreshold = -1

    private var loadPath = ""

    private val rect = Rect()
    private val canvasRect = RectF()

    fun updateData(data: String, dataDuration: Long, duration: Long, scrollX: Int) {
        this.duration = duration
        // 计算每一张图片对应的帧的时间
        // 计算公式:视频长度/(选取的视频长度(ms)/选择框长度(px)*每帧的长度(px))
        TrackConfig.FRAME_DURATION =
            (dataDuration.toFloat() / (SizeUtil.getScreenWidth(context) - SizeUtil.dp2px(120f))
                    * SizeUtil.dp2px(30F)).toInt()
        framesCount = calculateCount().first
        trackWidth = (duration * TrackConfig.PX_MS).toInt()
        layoutParams.width = trackWidth
        loadPath = data
        val currFrameNumber = scrollX / TrackConfig.THUMB_WIDTH
        startFrameNumber = ((scrollX - screenWidth) / TrackConfig.THUMB_WIDTH)
            .coerceAtLeast(0)
        endFrameNumber = ((scrollX + 2 * screenWidth) / TrackConfig.THUMB_WIDTH)
            .coerceAtMost(framesCount - 1)
        rightRefreshThreshold = (currFrameNumber + endFrameNumber) / 2
        leftRefreshThreshold = (currFrameNumber - startFrameNumber) / 2

        initFrames()
        requestLayout()
        internalInvalidate()
    }

    private fun initFrames() {
        if (!LruFrameBitmapCache.isCachedPath(loadPath)) {
            if (!File(loadPath).exists()) {
                return
            }
            LruFrameBitmapCache.addCachePath(loadPath)
            videoFrameHelper = VideoFrameHelper(
                context,
                loadPath
            ) {
                requestLayout()
                internalInvalidate()
            }
        }
        videoFrameHelper?.checkMemoryCache(startFrameNumber, endFrameNumber)
    }

    private fun internalInvalidate() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            invalidate()
        } else {
            postInvalidate()
        }
    }

    @MainThread
    fun updateScrollX(scrollX: Int) {
        val currFrameNumber = scrollX / TrackConfig.THUMB_WIDTH
        if (needRefresh(currFrameNumber)) {
            startFrameNumber = ((scrollX - screenWidth) / TrackConfig.THUMB_WIDTH)
                .coerceAtLeast(0)
            endFrameNumber = ((scrollX + 2 * screenWidth) / TrackConfig.THUMB_WIDTH)
                .coerceAtMost(framesCount - 1)
            leftRefreshThreshold = ((currFrameNumber + startFrameNumber) / 2)
            rightRefreshThreshold = ((currFrameNumber + endFrameNumber) / 2)
            videoFrameHelper?.checkMemoryCache(startFrameNumber, endFrameNumber)
        }
    }

    private fun needRefresh(currFrameNumber: Int): Boolean {
        return currFrameNumber >= rightRefreshThreshold ||
            (currFrameNumber <= leftRefreshThreshold && leftRefreshThreshold != 0)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        videoFrameHelper?.let {
            drawTrack(canvas)
        }
    }

    /**
     * 绘制视频轨道
     */
    private fun drawTrack(canvas: Canvas) {
        canvas.save()
        // 视频源起始时间计算 canvas 绘制区域的起始位置，由于 sourceTimeRange start 未与 speed 关联，需要去掉 speed 影响
        val initLeft = 0
        val initRight = (initLeft + this.duration * TrackConfig.PX_MS).toInt()
        canvasRect.set(
            initLeft.toFloat(),
            0F,
            initRight.toFloat(),
            height.toFloat())
        canvas.clipRect(canvasRect)
        // 绘制每段视频在可视区域的帧
        for (i in startFrameNumber..endFrameNumber) {
            val bitmap = videoFrameHelper?.getPositionBitmap(i, loadPath)
            bitmap?.let {
                drawBitmap(it, i * TrackConfig.THUMB_WIDTH, canvas)
            }
        }
    }

    private fun drawBitmap(bitmap: Bitmap, frameStart: Int, canvas: Canvas) {
        rect.set(
            frameStart,
            0,
            frameStart + TrackConfig.THUMB_WIDTH,
            TrackConfig.THUMB_HEIGHT
        )
        canvas.drawBitmap(bitmap, null, rect, null)
    }

    private fun calculateCount(): Pair<Int, Float> {
        var framesCount =
            (this.duration / TrackConfig.FRAME_DURATION.toFloat()).toInt()
        val realCount =
            this.duration / TrackConfig.FRAME_DURATION.toFloat()
        val lastScale = realCount - framesCount
        if (lastScale > 0) {
            framesCount++
        }
        return Pair(framesCount, lastScale)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        videoFrameHelper?.stopGetFrame = true
        LruFrameBitmapCache.clearCache()
    }
}
