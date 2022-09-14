package com.ss.ugc.android.editor.track.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.fuctiontrack.TrackGroup
import com.ss.ugc.android.editor.track.utils.OrientationManager
import com.ss.ugc.android.editor.track.utils.SizeUtil

class FrameScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : EditScroller(context, attrs, defStyleAttr) {

    private var screenWidthByUtil = SizeUtil.getScreenWidth(context)


    init {
        refreshPadding()
        clipToPadding = false
        clipChildren = false
        val size = SizeUtil.getScreenSize(TrackSdk.application)
        screenWidthByUtil = if (OrientationManager.isLand()) {
            size.y / 2
        } else {
            size.x / 2
        }
    }

    fun refreshPadding() {
//        screenWidthByUtil = SizeUtil.getScreenWidth(context)
//        val paddingHorizontal = screenWidthByUtil / 2 - SizeUtil.dp2px(20f)
//        setPadding(paddingHorizontal, paddingTop, paddingHorizontal, paddingBottom)
    }

    override fun checkAddView(child: View) {
    }

    companion object {
        private val TRACK_MARGIN_COVER = SizeUtil.dp2px(5f)
        private val COVER_MARGIN_MUTE = SizeUtil.dp2px(10f)
    }
}
