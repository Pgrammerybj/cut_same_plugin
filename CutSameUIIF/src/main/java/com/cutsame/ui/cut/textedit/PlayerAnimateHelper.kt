package com.cutsame.ui.cut.textedit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.SurfaceView
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.cutsame.ui.R

object PlayerAnimateHelper {

    private const val ANIMA_TIME = 300

    //底部videolist 的高度
    private var videoListViewHeigth: Int = 0
    //文本框的高度
    private var editTextViewHeight: Int = 0

    //标题高度
    var topTitleViewHeight: Int = 0
        private set

    //画布高度
    var surfaceViewHeight: Int = 0
        private set

    //画布宽度
    var surfaceViewWidth: Int = 0
        private set

    //画布两边的margin
    var leftRightMargin: Int = 0
        private set

    fun setViewHeight(surfaceView: SurfaceView?) {
        if (surfaceView == null) {
            return
        }
        videoListViewHeigth = surfaceView.context.resources.getDimensionPixelOffset(R.dimen.video_player_bottom_videolist_height)
        editTextViewHeight = surfaceView.context.resources.getDimensionPixelOffset(R.dimen.video_player_bottom_textedit_height)
        topTitleViewHeight = surfaceView.context.resources.getDimensionPixelOffset(R.dimen.video_player_top_title_height)
        leftRightMargin = surfaceView.context.resources.getDimensionPixelOffset(R.dimen.video_player_left_right_margin)
        surfaceViewHeight = surfaceView.measuredHeight
        surfaceViewWidth = surfaceView.measuredWidth
    }

    fun animateTransHeightShowOrHide(targetView: View?, height: Int, isShow: Boolean, isDelay: Boolean) {
        if (targetView == null) {
            return
        }
        val alpha: ObjectAnimator
        if (isShow) {
            alpha = ObjectAnimator.ofFloat(targetView, "translationY", -height.toFloat())
            alpha.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    if (targetView.visibility == View.GONE) {
                        targetView.visibility = View.VISIBLE
                    }
                }
            })
        } else {
            alpha = ObjectAnimator.ofFloat(targetView, "translationY", height.toFloat())
        }
        alpha.duration = ANIMA_TIME.toLong()
        alpha.interpolator = AccelerateDecelerateInterpolator()
        alpha.startDelay = (if (isDelay) ANIMA_TIME else 0).toLong()
        alpha.start()
    }

    interface PlayerSurfaceScaleListener {
        fun scale(scaleW: Float, scaleH: Float, tranX: Int, tranY: Int, faction: Float, isScaleDown: Boolean)
        fun scaleEnd(targetScaleW: Float, targetScaleH: Float, targetTranx: Int, targetTranY: Int, isScaleDown: Boolean)
    }

}
