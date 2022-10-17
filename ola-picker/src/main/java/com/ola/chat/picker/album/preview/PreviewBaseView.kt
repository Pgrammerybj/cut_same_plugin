package com.ola.chat.picker.album.preview

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.ola.chat.picker.album.adapter.GalleryPreviewAdapter

@SuppressLint("ViewConstructor")
open class PreviewBaseView(
    context: Context,
    val position: Int,
    var adapter: GalleryPreviewAdapter?
) : FrameLayout(context, null) {
    var isResume: Boolean = false

    fun init() {
        if (position == adapter?.getCurrentPosition()) {
            isResume = true
            onResume()
        }
    }

    open fun onResume() {
    }

    open fun onPause() {
    }

    fun onViewSelect(currentPosition: Int) {
        if (currentPosition == position) {
            if (!isResume) {
                isResume = true
                onResume()
            }
        } else {
            if (isResume) {
                isResume = false
                onPause()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        adapter = null
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == View.VISIBLE && !isResume && adapter?.getCurrentPosition() == position) {
            isResume = true
            onResume()
        }

        if (visibility == View.GONE && isResume && adapter?.getCurrentPosition() == position) {
            isResume = false
            onPause()
        }
    }

    interface PreviewListener {
        fun onExit()
    }
}