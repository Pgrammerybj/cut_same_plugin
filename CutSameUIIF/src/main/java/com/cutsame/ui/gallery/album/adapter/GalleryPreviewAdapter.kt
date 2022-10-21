package com.cutsame.ui.gallery.album.adapter

import android.content.Context
import androidx.viewpager.widget.PagerAdapter
import android.view.View
import android.view.ViewGroup
import com.cutsame.ui.gallery.album.model.MediaData
import com.cutsame.ui.gallery.album.preview.PreviewBaseView
import com.cutsame.ui.gallery.album.preview.PreviewImageView
import com.cutsame.ui.gallery.album.preview.PreviewVideoView

class GalleryPreviewAdapter(
    private val context: Context,
    val previewListener: PreviewBaseView.PreviewListener?
) : PagerAdapter() {
    private var data = ArrayList<MediaData>()
    private val baseViews = ArrayList<PreviewBaseView>()
    private var currentPosition = 0

    fun setData(lists: List<MediaData>) {
        data.clear()
        data.addAll(lists)
        notifyDataSetChanged()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val mediaData = data[position]
        val view = if (mediaData.isImage()) {
            PreviewImageView(context, data[position], position, previewListener, this)
        } else {
            PreviewVideoView(context, data[position], position, previewListener, this)
        }
        baseViews.add(view)
        container.addView(view)
        return view
    }

    override fun isViewFromObject(view: View, objectAny: Any): Boolean {
        return view == objectAny
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun destroyItem(container: ViewGroup, position: Int, objectAny: Any) {
        baseViews.remove(objectAny)
        container.removeView(objectAny as View)
    }

    fun setCurrentPosition(position: Int) {
        currentPosition = position
        for (view in baseViews) {
            view.onViewSelect(position)
        }
    }

    fun getCurrentPosition(): Int {
        return currentPosition
    }

    fun getCurrentItem(): MediaData {
        return data[currentPosition]
    }

    fun getData(): List<MediaData> {
        return data
    }
}