package com.ss.ugc.android.editor.track.tip

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.ss.ugc.android.editor.base.theme.ThemeStore
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.track.utils.PadUtil
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.base.utils.postOnUiThread

class ItemTrackTipsManager(container: ViewGroup, frameLayout: ViewGroup) {
    private val mContainer = container
    private var tvMute: TextView? = null
    private var muteIcon: Int = R.drawable.ic_mute_n_ck

    // 显示美颜等功能
    private var tvFeature: TextView? = null
    private var tvDuration: TextView? = null
    private var tvAudioFilter: TextView? = null
    val llTipsLayout: LinearLayout = LinearLayout(container.context)

    val lpLayout = RelativeLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private var lpTvMute =
        ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    private var lpTvAudioFilter =
        ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 10
        }

    private var lpTvFeature =
        ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    private var lpTvDuration = ViewGroup.MarginLayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private var totalWidth = 0
    private var maxWidth = 0

    init {
        llTipsLayout.orientation = LinearLayout.HORIZONTAL
        lpLayout.topMargin = SizeUtil.dp2px(5F)
        lpLayout.addRule(RelativeLayout.ALIGN_LEFT, frameLayout.id)
        llTipsLayout.layoutParams = lpLayout
        container.addView(llTipsLayout, lpLayout)

        llTipsLayout.setPadding(SizeUtil.dp2px(3F), 0, 0, 0)
        lpTvFeature.leftMargin = SizeUtil.dp2px(3F)
        lpTvMute.leftMargin = SizeUtil.dp2px(3F)
        initCustomUI()
    }

    private fun initCustomUI(){
        ThemeStore.getCustomDisableOriginalVoiceTip()?.apply { muteIcon = this }
    }

    fun hideMuteTips() {
        tvMute?.let {
            lpLayout.width = maxWidth
            llTipsLayout.removeView(it)
            reCalTotalWidth()
        }
    }

    fun hideAudioFilter(){
        tvAudioFilter?.let {
            lpLayout.width = maxWidth
            llTipsLayout.removeView(it)
            reCalTotalWidth()
        }
    }

    fun hideFeatureTips() {
        tvFeature?.let {
            lpLayout.width = maxWidth
            llTipsLayout.removeView(it)
            reCalTotalWidth()
        }
    }

    fun hideDurationTips() {
        tvDuration?.let {
            lpLayout.width = maxWidth
            llTipsLayout.removeView(it)
            reCalTotalWidth()
        }
    }

    private fun showFeatureTips() {
        initFeatureTips()
        tvFeature?.let {
            lpLayout.width = maxWidth
            llTipsLayout.removeView(it)
            val index = if (tvMute != null ) {
                llTipsLayout.indexOfChild(tvMute!!)
            } else {
                llTipsLayout.childCount
            }
            llTipsLayout.addView(it, index, lpTvFeature)
            reCalTotalWidth()
        }
    }

    private fun showDurationTips() {
        initDurationTips()
        tvDuration?.let {
            lpLayout.width = maxWidth
            llTipsLayout.removeView(it)
            llTipsLayout.addView(it, 0, lpTvDuration)
            reCalTotalWidth()
        }
    }

    private fun showAudioFilter(){
        initAudioFilterTips()
        tvAudioFilter?.let {
            lpLayout.width = maxWidth
            llTipsLayout.removeView(it)
            llTipsLayout.addView(it, llTipsLayout.childCount, lpTvAudioFilter)
            reCalTotalWidth()
        }
    }

    fun showMuteTips() {
        initMuteTips()
        tvMute?.let {
            lpLayout.width = maxWidth
            llTipsLayout.removeView(it)
            llTipsLayout.addView(it, llTipsLayout.childCount, lpTvMute)
            reCalTotalWidth()
        }
    }

    private fun reCalTotalWidth() {
        postOnUiThread {
            totalWidth = llTipsLayout.paddingLeft
            for (i in 0.until(llTipsLayout.childCount)) {
                val child = llTipsLayout.getChildAt(i)
                totalWidth += child?.measuredWidth ?: 0
                totalWidth += (child?.layoutParams as MarginLayoutParams).leftMargin
            }
        }
    }

    fun setMaxWidth(width: Int) {
        maxWidth = width
    }

    fun getTotalWidth(): Int {
        return totalWidth
    }

    fun showMuteTipsWithText(text: String) {
        showMuteTips()
        tvMute?.text = text
    }

    fun showDurationTipsWithText(text: String) {
        showDurationTips()
        tvDuration?.text = text
    }

    fun showAudioFilterWithText(text: String){
        showAudioFilter()
        tvAudioFilter?.text = text
    }

    fun showFeatureTipsWithText(text: String = "", resId: Int = 0) {
        showFeatureTips()
        if (!TextUtils.isEmpty(text)) {
            tvFeature?.text = text
        }

        if (resId != 0) {
            tvFeature?.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0)
        }
    }

    fun setTipsTranslateX(transX: Float) {
        llTipsLayout.translationX = transX
    }

    private fun initFeatureTips() {
        if (tvFeature == null) {
            tvFeature = TextView(mContainer.context)
            tvFeature?.let {
                it.setTextColor(Color.WHITE)
                it.background = getDrawale(R.drawable.bg_track_icon_tip)
                it.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8F)
                it.gravity = Gravity.CENTER
                it.setPadding(getIntValue(3F), 0, getIntValue(4F), 0)
                it.compoundDrawablePadding = getIntValue(2F)
                it.minWidth = getIntValue(39F)
                it.maxLines = 1
            }
        }
    }

    private fun initMuteTips() {
        if (tvMute == null) {
            tvMute = TextView(mContainer.context)
            tvMute?.let {
                it.setTextColor(Color.WHITE)
                it.background = getDrawale(R.drawable.bg_track_icon_tip)
                it.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8F)
                it.gravity = Gravity.CENTER
                it.setPadding(SizeUtil.dp2px(1F), 0, SizeUtil.dp2px(1F), 0)
                it.maxLines = 1
                it.compoundDrawablePadding = SizeUtil.dp2px(1F)
//                it.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mute_n, 0, 0, 0)
                it.setCompoundDrawablesWithIntrinsicBounds(muteIcon, 0, 0, 0)
            }
        }
    }

    private fun initAudioFilterTips() {
        if (tvAudioFilter == null) {
            tvAudioFilter = TextView(mContainer.context)
            tvAudioFilter?.let {
                it.setTextColor(Color.WHITE)
                it.background = getDrawale(R.drawable.bg_track_icon_tip)
                it.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 8F)
                it.gravity = Gravity.CENTER
                it.setPadding(SizeUtil.dp2px(1F), 0, SizeUtil.dp2px(1F), 0)
                it.maxLines = 1
                it.compoundDrawablePadding = SizeUtil.dp2px(1F)
                it.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_track_audio_filter, 0, 0, 0)
            }
        }
    }

    private fun initDurationTips() {
        if (tvDuration == null) {
            tvDuration = TextView(mContainer.context)
            tvDuration?.let {
                it.setTextColor(Color.WHITE)
                it.background = getDrawale(R.drawable.bg_track_icon_tip)
                it.setTextSize(TypedValue.COMPLEX_UNIT_DIP, if (PadUtil.isPad) 10F else 8F)
                it.gravity = Gravity.CENTER
                it.setPadding(getIntValue(4F), 0, getIntValue(4F), 0)
                it.maxLines = 1
                it.paint.isFakeBoldText = true
            }
        }
    }

    private fun getDrawale(@DrawableRes resId: Int): Drawable {
        return mContainer.context.resources.getDrawable(resId)
    }

    private fun getIntValue(value: Float): Int {
        return SizeUtil.dp2px(value)
    }
}
