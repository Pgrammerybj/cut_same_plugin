package com.cutsame.ui.gallery.album.preview

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import com.cutsame.ui.R
import com.cutsame.ui.gallery.album.adapter.GalleryPreviewAdapter
import com.cutsame.ui.gallery.customview.scale.ScaleGestureLayout
import com.cutsame.ui.gallery.customview.scale.ScaleGestureLayoutListener
import com.cutsame.ui.gallery.album.model.MediaData

class PreviewVideoView(
    context: Context,
    val mediaData: MediaData,
    position: Int,
    val previewListener: PreviewListener?,
    adapter: GalleryPreviewAdapter
) : PreviewBaseView(context, position, adapter) {
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var surfaceView: SurfaceView
    private lateinit var videoScaleGestureLayout: ScaleGestureLayout

    init {
        initView()
        super.init()
    }

    fun initView() {
        val rootView =
            LayoutInflater.from(context).inflate(R.layout.picker_preview_video, this, true)
        surfaceView = rootView.findViewById<SurfaceView>(R.id.surfaceView)
        videoScaleGestureLayout = rootView.findViewById(R.id.videoScaleGestureLayout)
        videoScaleGestureLayout.setVideoSize(mediaData.width, mediaData.height)
        videoScaleGestureLayout.setZoomListener(object : ScaleGestureLayoutListener {
            override fun onAlphaPercent(percent: Float) {
            }

            override fun onClick() {
            }

            override fun onExit() {
                previewListener?.onExit()
            }

            override fun onScaleBegin() {
            }

            override fun onScaleEnd(scaleFactor: Float) {
            }
        })
    }

    override fun onDetachedFromWindow() {
        mediaPlayer?.apply {
            if (this.isPlaying) {
                this.stop()
            }
            this.release()
        }
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, mediaData.uri)
        mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mediaPlayer?.setOnPreparedListener {
            resizeDisplayView(
                surfaceView,
                mediaPlayer?.videoWidth ?: 0,
                mediaPlayer?.videoHeight ?: 0
            )
            it.setDisplay(surfaceView.holder)
            mediaPlayer?.start()
        }
        mediaPlayer?.isLooping = true
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.stop()
    }

    private fun resizeDisplayView(displayView: View, videoW: Int, videoH: Int) {
        if (videoW <= 0 || videoH <= 0) {
            return
        }

        val displayOriWidth = displayView.width
        val displayOriHeight = displayView.height
        val videoRatio = videoW.toFloat() / videoH
        val displayOriRatio = displayOriWidth.toFloat() / displayOriHeight
        val params = displayView.layoutParams
        if (videoRatio < displayOriRatio) {
            params.height = displayOriHeight
            params.width = (displayOriHeight * videoRatio).toInt()
        } else {
            params.width = displayOriWidth
            params.height = (displayOriWidth / videoRatio).toInt()
        }
        displayView.layoutParams = params
    }
}