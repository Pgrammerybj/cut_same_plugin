package com.cutsame.ui.gallery.album.preview

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.cutsame.ui.R
import com.cutsame.ui.gallery.album.adapter.GalleryPreviewAdapter
import com.cutsame.ui.gallery.customview.scale.ScaleGestureLayout
import com.cutsame.ui.gallery.customview.scale.ScaleGestureLayoutListener
import com.cutsame.ui.gallery.album.model.MediaData

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
        imageView = rootView.findViewById<ImageView>(R.id.imageView)
        imageScaleGestureLayout = rootView.findViewById(R.id.imageScaleGestureLayout)
        imageScaleGestureLayout.setVideoSize(mediaData.width, mediaData.height)
        imageScaleGestureLayout.setZoomListener(object : ScaleGestureLayoutListener{
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
        Glide.with(context)
            .load(mediaData.uri)
            .into(imageView)
    }

}