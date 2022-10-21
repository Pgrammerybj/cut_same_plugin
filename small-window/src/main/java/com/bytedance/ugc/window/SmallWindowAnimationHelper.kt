package com.bytedance.ugc.window

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs

/**
 * Create by caoliangzhao on 2021/3/30.
 * Describe:
 *
 **/
class SmallWindowAnimationHelper(private val smallWindowView: ViewGroup) {
    // 小窗的折叠和展开按钮
    private val smallWindowSwitchBtn = smallWindowView.findViewById<View>(R.id.small_window_switch_btn)

    companion object {
        const val FRAME_GAP = 33
        const val ANIM_DURATION = 200L
    }

    private var lastAnimatorTime = 0L
    var isAnimating = false
        private set
    /**
     * 小窗处于展开态，折叠
     */
    fun startFoldAnim(callback: () -> Unit) {
        val startWidth = smallWindowView.width
        val targetWidth = SmallWindowConfig.smallWindowFoldWidth
        if (abs(startWidth - targetWidth) < 10) return // 状态错误，不处理
        val startHeight = smallWindowView.height
        val targetHeight = SmallWindowConfig.smallWindowFoldHeight
        val rootLp = smallWindowView.layoutParams as ViewGroup.MarginLayoutParams
        val startMargin = rootLp.leftMargin
        val targetMargin = ScreenUtils.getScreenWidth(smallWindowView.context) - SmallWindowConfig.horizontalPadding - SmallWindowConfig.smallWindowFoldWidth

        val iconLp = smallWindowSwitchBtn.layoutParams as ViewGroup.MarginLayoutParams
        val iconStartWidth = smallWindowSwitchBtn.width
        val iconStartMargin = iconLp.marginEnd
        val iconTargetWidth = SmallWindowConfig.unFoldIconWidth
        val iconTargetMargin = (SmallWindowConfig.smallWindowFoldWidth - SmallWindowConfig.unFoldIconWidth) / 2

        val centerX = (smallWindowView.left + smallWindowView.right) / 2
        val animator = ValueAnimator.ofFloat(0F, 1F)
        animator.duration = ANIM_DURATION
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener {
            if (System.currentTimeMillis() - lastAnimatorTime < FRAME_GAP) return@addUpdateListener
            lastAnimatorTime = System.currentTimeMillis()
            // 外部根View的动画
            val curValue = it.animatedValue as Float
            rootLp.width = (startWidth + (targetWidth - startWidth) * curValue).toInt()
            rootLp.height = (startHeight + (targetHeight - startHeight) * curValue).toInt()
            if (centerX > ScreenUtils.getScreenWidth(smallWindowView.context) / 2) { // 在右侧， 往右收，左侧Margin同时也要变大
                rootLp.leftMargin = (startMargin + (targetMargin - startMargin) * curValue).toInt()
            }

            // 内部按钮的动画
            iconLp.width = (iconStartWidth + (iconTargetWidth - iconStartWidth) * curValue).toInt()
            iconLp.height = iconLp.width
            iconLp.marginEnd = (iconStartMargin + (iconTargetMargin - iconStartMargin) * curValue).toInt()

            smallWindowSwitchBtn.layoutParams = iconLp
            smallWindowView.layoutParams = rootLp
        }

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator?) {
            }

            override fun onAnimationEnd(p0: Animator?) {
                foldEnd(centerX <= ScreenUtils.getScreenWidth(smallWindowView.context) / 2)
                callback.invoke()
                isAnimating = false
            }

            override fun onAnimationCancel(p0: Animator?) {
                foldEnd(centerX <= ScreenUtils.getScreenWidth(smallWindowView.context) / 2)
                callback.invoke()
                isAnimating = false
            }

            override fun onAnimationRepeat(p0: Animator?) {}
        })
        isAnimating = true
        animator.start()
    }

    fun foldEnd(isLeft: Boolean) {
        smallWindowSwitchBtn?.apply {
            layoutParams.width = SmallWindowConfig.unFoldIconWidth
            layoutParams.height = SmallWindowConfig.unFoldIconWidth
            (layoutParams as ConstraintLayout.LayoutParams).also {
                it.marginEnd = (SmallWindowConfig.smallWindowFoldWidth - SmallWindowConfig.unFoldIconWidth) / 2
                it.bottomMargin = (SmallWindowConfig.smallWindowFoldHeight - SmallWindowConfig.unFoldIconWidth) / 2
            }
            smallWindowSwitchBtn.layoutParams = layoutParams
        }
        val lp = smallWindowView.layoutParams
        lp.width = SmallWindowConfig.smallWindowFoldWidth
        lp.height = SmallWindowConfig.smallWindowFoldHeight
        if (isLeft.not()) {
            (lp as ViewGroup.MarginLayoutParams).leftMargin = ScreenUtils.getScreenWidth(smallWindowView.context) - SmallWindowConfig.horizontalPadding - SmallWindowConfig.smallWindowFoldWidth
        }
        smallWindowView.layoutParams = lp
    }

    /**
     * 小窗处于折叠态，折叠
     */
    fun startUnFoldAnim(callback: () -> Unit) {
        val startWidth = smallWindowView.width
        val targetWidth = SmallWindowConfig.smallWindowWidth
        if (abs(startWidth - targetWidth) < 10) return // 状态错误，不处理
        val startHeight = smallWindowView.height
        val targetHeight = SmallWindowConfig.smallWindowHeight
        val rootLp = smallWindowView.layoutParams as ViewGroup.MarginLayoutParams
        val startMargin = rootLp.leftMargin
        val targetMargin = ScreenUtils.getScreenWidth(smallWindowView.context) - SmallWindowConfig.horizontalPadding - SmallWindowConfig.smallWindowWidth

        val iconLp = smallWindowSwitchBtn.layoutParams as ViewGroup.MarginLayoutParams
        val iconStartWidth = smallWindowSwitchBtn.width
        val iconStartMargin = iconLp.marginEnd
        val iconTargetWidth = SmallWindowConfig.foldIconWidth
        val iconTargetMargin = SmallWindowConfig.foldIconMargin

        val centerX = (smallWindowView.left + smallWindowView.right) / 2
        val animator = ValueAnimator.ofFloat(0F, 1F)
        animator.duration = ANIM_DURATION
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener {
            if (System.currentTimeMillis() - lastAnimatorTime < FRAME_GAP) return@addUpdateListener
            lastAnimatorTime = System.currentTimeMillis()
            // 外部根View的动画
            val curValue = it.animatedValue as Float
            rootLp.width = (startWidth + (targetWidth - startWidth) * curValue).toInt()
            rootLp.height = (startHeight + (targetHeight - startHeight) * curValue).toInt()
            if (centerX > ScreenUtils.getScreenWidth(smallWindowView.context) / 2) { // 在右侧， 往右收，左侧Margin同时也要变大
                rootLp.leftMargin = (startMargin + (targetMargin - startMargin) * curValue).toInt()
            }

            // 内部按钮的动画
            iconLp.width = (iconStartWidth + (iconTargetWidth - iconStartWidth) * curValue).toInt()
            iconLp.height = iconLp.width
            iconLp.marginEnd = (iconStartMargin + (iconTargetMargin - iconStartMargin) * curValue).toInt()

            smallWindowSwitchBtn.layoutParams = iconLp
            smallWindowView.layoutParams = rootLp
        }

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator?) {
                smallWindowSwitchBtn.setBackgroundResource(R.drawable.camera_ic_narrow)
            }

            override fun onAnimationEnd(p0: Animator?) {
                unFoldEnd(centerX <= ScreenUtils.getScreenWidth(smallWindowView.context) / 2)
                callback.invoke()
                isAnimating = false
            }

            override fun onAnimationCancel(p0: Animator?) {
                unFoldEnd(centerX <= ScreenUtils.getScreenWidth(smallWindowView.context) / 2)
                callback.invoke()
                isAnimating = false
            }

            override fun onAnimationRepeat(p0: Animator?) {}
        })
        isAnimating = true
        animator.start()
    }

    fun unFoldEnd(isLeft: Boolean) {
        smallWindowSwitchBtn?.apply {
            layoutParams.width = SmallWindowConfig.foldIconWidth
            layoutParams.height = SmallWindowConfig.foldIconWidth
            (layoutParams as ConstraintLayout.LayoutParams).also {
                it.marginEnd = SmallWindowConfig.foldIconMargin
                it.bottomMargin = SmallWindowConfig.foldIconMargin
            }
            smallWindowSwitchBtn.layoutParams = layoutParams
        }
        val lp = smallWindowView.layoutParams
        lp.width = SmallWindowConfig.smallWindowWidth
        lp.height = SmallWindowConfig.smallWindowHeight
        if (isLeft.not()) {
            (lp as ViewGroup.MarginLayoutParams).leftMargin = ScreenUtils.getScreenWidth(smallWindowView.context) - SmallWindowConfig.horizontalPadding - SmallWindowConfig.smallWindowWidth
        }
        smallWindowView.layoutParams = lp
    }
}
