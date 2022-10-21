package com.ola.chat.picker.album.preview

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.ola.chat.picker.customview.scale.ScaleGestureLayoutListener
import com.ola.chat.picker.R
import com.ola.chat.picker.album.adapter.GalleryPreviewAdapter
import com.ola.chat.picker.album.model.MediaData
import com.ola.chat.picker.customview.scale.ScaleGestureLayout

@SuppressLint("ViewConstructor")
class PreviewImageView(
    context: Context,
    val mediaData: MediaData,
    position: Int,
    val previewListener: PreviewListener?,
    adapter: GalleryPreviewAdapter
) : PreviewBaseView(context, position, adapter){

    private lateinit var imageView: ImageView
    private lateinit var imageScaleGestureLayout: ScaleGestureLayout

    init {
        initView()
        super.init()
    }

    fun initView() {
        val rootView =
            LayoutInflater.from(context).inflate(R.layout.picker_preview_image, this, true)
        imageView = rootView.findViewById(R.id.imageView)
        imageScaleGestureLayout = rootView.findViewById(R.id.imageScaleGestureLayout)
        imageScaleGestureLayout.setVideoSize(mediaData.width, mediaData.height)
        imageScaleGestureLayout.setZoomListener(object : ScaleGestureLayoutListener {
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

    override fun onResume(){
        super.onResume()
        Glide.with(context).load(mediaData.uri).into(imageView)
    }

}