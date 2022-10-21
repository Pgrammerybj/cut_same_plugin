package com.cutsame.ui.gallery.data

import android.content.Context
import com.cutsame.ui.R

enum class TabType {
    Album, Camera;

    fun getTabName(context: Context): String {
        return when (this) {
            Album -> context.getString(R.string.pick_from_album)
            Camera -> context.getString(R.string.pick_from_camera)
        }
    }
}