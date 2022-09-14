package com.cutsame.ui.gallery.customview

import android.animation.TimeInterpolator
import android.graphics.Path
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.cutsame.ui.gallery.customview.utils.PathMotion

class AppearOrDisappearAnimationConfiguration {


    var translationInterpolator: TimeInterpolator = FastOutSlowInInterpolator()
    var scaleInterpolator: TimeInterpolator = FastOutSlowInInterpolator()
    var clipInterpolator: TimeInterpolator = FastOutSlowInInterpolator()
    var alphaInterpolator: TimeInterpolator = FastOutSlowInInterpolator()
    var originImageDisappearAlphaInterpolator: TimeInterpolator = FastOutSlowInInterpolator()
    var translationDuration = 250L
    var scaleDuration = 250L
    var clipDuration = 250L
    var alphaDuration = 250L
    var originImageDisappearAlphaDuration = 250L
    var pathMotion = STRAIGHT_PATH_MOTION

    companion object {
        val STRAIGHT_PATH_MOTION: PathMotion = object : PathMotion() {
            override fun getPath(startX: Float, startY: Float, endX: Float, endY: Float): Path {
                val path = Path()
                path.moveTo(startX, startY)
                path.lineTo(endX, endY)
                return path
            }
        }
    }
}