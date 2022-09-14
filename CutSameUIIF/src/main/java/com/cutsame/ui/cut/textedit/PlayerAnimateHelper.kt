package com.cutsame.ui.cut.textedit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
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

    fun scaleIn(videListView: View?, editTextView: View?, scaleListener: PlayerSurfaceScaleListener?) {
        if (videListView == null || editTextView == null) {
            return
        }
        val surfaceChangeHeight = editTextViewHeight - videoListViewHeigth
        //隐藏的头部title也算作展示区域
        val targetScale = (surfaceViewHeight - surfaceChangeHeight) * 1.0f / surfaceViewHeight

        val disappearSet = createAlphaAndTransAnimate(videListView, videoListViewHeigth, false, true)
        val appearSet = createAlphaAndTransAnimate(editTextView, editTextViewHeight, true, false)

        appearSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                editTextView.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                scaleListener?.scaleEnd(targetScale, targetScale, 0, surfaceChangeHeight, true)
            }
        })
        (appearSet.childAnimations[0] as ObjectAnimator).addUpdateListener { animation ->
            val newScale = 1 - (1 - targetScale) * animation.animatedFraction
            val tranY = surfaceChangeHeight * animation.animatedFraction
            scaleListener?.scale(newScale, newScale, 0, tranY.toInt(), animation.animatedFraction, true)
        }

        appearSet.start()
        disappearSet.start()
    }

    fun scaleOut(videListView: View?, editTextView: View?, scaleListener: PlayerSurfaceScaleListener?) {
        if (videListView == null || editTextView == null) {
            return
        }
        val surfaceChangeHeight = editTextViewHeight - videoListViewHeigth
        val targetScale = (surfaceViewHeight - surfaceChangeHeight) * 1.0f / surfaceViewHeight

        val disappearSet = createAlphaAndTransAnimate(editTextView, editTextViewHeight, false, false)
        val appearSet = createAlphaAndTransAnimate(videListView, videoListViewHeigth, true, true)

        disappearSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                editTextView.visibility = View.GONE
            }
        })

        appearSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                scaleListener?.scaleEnd(targetScale, targetScale, 0, surfaceChangeHeight, false)
            }
        })
        (disappearSet.childAnimations[0] as ObjectAnimator).addUpdateListener { animation ->
            val newScale = targetScale - (targetScale - 1) * animation.animatedFraction
            val tranY = surfaceChangeHeight * (1 - animation.animatedFraction)
            scaleListener?.scale(newScale, newScale, 0, tranY.toInt(), animation.animatedFraction, false)
        }

        disappearSet.start()
        appearSet.start()
    }

    fun animateAlphaShowOrHide(targetView: View?, isShow: Boolean, isDelay: Boolean) {
        if (targetView == null) {
            return
        }
        val alpha: ObjectAnimator
        if (isShow) {
            alpha = ObjectAnimator.ofFloat(targetView, "alpha", 0f, 1f)
            alpha.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    if (targetView.visibility == View.GONE) {
                        targetView.visibility = View.VISIBLE
                    }
                }
            })
        } else {
            alpha = ObjectAnimator.ofFloat(targetView, "alpha", 1f, 0f)
        }
        alpha.duration = ANIMA_TIME.toLong()
        alpha.interpolator = AccelerateDecelerateInterpolator()
        alpha.startDelay = (if (isDelay) ANIMA_TIME else 0).toLong()
        alpha.start()
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

    private fun createAlphaAndTransAnimate(targetView: View, transY: Int, isShow: Boolean, isOnlyAlpha: Boolean): AnimatorSet {
        val animatorSet = AnimatorSet()
        val alpha: ObjectAnimator
        var translation: ObjectAnimator? = null

        if (isShow) {
            alpha = ObjectAnimator.ofFloat(targetView, "alpha", 0f, 1f)
            if (!isOnlyAlpha) {
                translation = ObjectAnimator.ofFloat(targetView, "translationY", transY.toFloat(), 0f)
            }
        } else {
            alpha = ObjectAnimator.ofFloat(targetView, "alpha", 1f, 0f)
            if (!isOnlyAlpha) {
                translation = ObjectAnimator.ofFloat(targetView, "translationY", transY.toFloat())
            }
        }
        if (translation != null) {
            animatorSet.playTogether(alpha, translation)
        } else {
            animatorSet.playTogether(alpha)
        }
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.duration = ANIMA_TIME.toLong()
        return animatorSet
    }

    interface PlayerSurfaceScaleListener {
        fun scale(scaleW: Float, scaleH: Float, tranX: Int, tranY: Int, faction: Float, isScaleDown: Boolean)
        fun scaleEnd(targetScaleW: Float, targetScaleH: Float, targetTranx: Int, targetTranY: Int, isScaleDown: Boolean)
    }

}
