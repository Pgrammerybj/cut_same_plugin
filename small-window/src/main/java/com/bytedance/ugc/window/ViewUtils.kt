package com.ss.android.ugc.effectdemo.util

import android.app.Activity
import android.content.ContextWrapper
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.Group

var View.visible: Boolean
    get() = this.visibility == View.VISIBLE
    set(value) {
        this.visibility = if (value) View.VISIBLE else View.GONE
    }

fun View.gone() {
    this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.INVISIBLE
}

fun View.getLocationOnScreen(): Size {

    val appScreenLocation = IntArray(2) { 0 }
    val rootLayout =
        (context as? Activity)?.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0)
            ?: rootView
    rootLayout.getLocationOnScreen(appScreenLocation)

    val screenLocation = IntArray(2) { 0 }
    getLocationOnScreen(screenLocation)

    val drawingLocation = IntArray(2) { 0 }
    drawingLocation[0] = screenLocation[0] - appScreenLocation[0]
    drawingLocation[1] = screenLocation[1] - appScreenLocation[1]

    return Size(drawingLocation[0], drawingLocation[1])
}

fun View.getRealLocationOnScreen(): Size {
    val screenLocation = IntArray(2) { 0 }
    getLocationOnScreen(screenLocation)
    return Size(screenLocation[0], screenLocation[1])
}

fun View.onGlobalLayout(block: () -> Unit) {
    val v = this
    v.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                block()
                v.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
}

const val DELAY_TAG = 1123461123
const val DELAY_LAST_TAG = 1123460103

/***
 * 带延迟过滤的点击事件View扩展
 * @param delay Long 延迟时间，默认600毫秒
 * @param block: (T) -> Unit 函数
 * @return Unit
 */
fun <T : View> T.clickWithTrigger(time: Long = 600, block: (T) -> Unit) {
    triggerDelay = time
    setOnClickListener {
        if (clickEnable()) {
            block(this)
        }
    }
}

fun <T : View> Group.addClickWithTrigger(time: Long = 600, block: (T) -> Unit) {
    referencedIds.forEach { id ->
        rootView.findViewById<View>(id)?.clickWithTrigger {
            block(it as T)
        }
    }
}

private var <T : View> T.triggerLastTime: Long
    get() = if (getTag(DELAY_LAST_TAG) != null) getTag(DELAY_LAST_TAG) as Long else -601
    set(value) {
        setTag(DELAY_LAST_TAG, value)
    }

private var <T : View> T.triggerDelay: Long
    get() = if (getTag(DELAY_TAG) != null) getTag(DELAY_TAG) as Long else 600
    set(value) {
        setTag(DELAY_TAG, value)
    }

private fun <T : View> T.clickEnable(): Boolean {
    var flag = false
    val currentClickTime = System.currentTimeMillis()
    if (currentClickTime - triggerLastTime >= triggerDelay) {
        flag = true
    }
    triggerLastTime = currentClickTime
    return flag
}




fun View.setMarginTop(margin: Int) {
    setMarginLayoutParams { it.topMargin = margin }
}

fun View.setMarginStart(margin: Int) {
    setMarginLayoutParams { it.marginStart = margin }
}

fun View.setMarginBottom(margin: Int) {
    setMarginLayoutParams { it.bottomMargin = margin }
}

fun View.setMarginEnd(margin: Int) {
    setMarginLayoutParams { it.marginEnd = margin }
}

fun View.setHorizonMargin(margin: Int) {
    setMarginLayoutParams {
        it.marginStart = margin
        it.marginEnd = margin
    }
}

fun View.setHorizonMargin(marginStart: Int, marginEnd: Int) {
    setMarginLayoutParams {
        it.marginStart = marginStart
        it.marginEnd = marginEnd
    }
}

fun View.setViewHeight(height: Int) {
    setMarginLayoutParams {
        it.height = height
    }
}

fun View.setViewWidth(width: Int) {
    setMarginLayoutParams {
        it.width = width
    }
}

fun View.setMarginLayoutParams(action: (lp: ViewGroup.MarginLayoutParams) -> Unit) {
    layoutParams = (this.layoutParams as ViewGroup.MarginLayoutParams).apply {
        action.invoke(this)
    }
}


// TextView 按照指定后缀截断前面字符：如 一二三四...制作的模板, 不要设置drawableEnd
fun TextView.setFixText(oriStr: String, fixStr: String, maxWidth: Int) {
    val originTextWidth = paint.measureText(oriStr)
    if (maxWidth > originTextWidth) {
        text = oriStr
    } else {
        val lastIndexOfPoint = oriStr.lastIndexOf(fixStr)
        if (lastIndexOfPoint == -1) {
            text = oriStr
        } else {
            var prefixText = oriStr.substring(0, lastIndexOfPoint)
            val suffixText = "..." + oriStr.substring(lastIndexOfPoint, oriStr.length)

            var prefixWidth = paint.measureText(prefixText)
            val suffixWidth = paint.measureText(suffixText)

            while (maxWidth - prefixWidth < suffixWidth && prefixText.isNotEmpty()) {
                prefixText = prefixText.substring(0, prefixText.length - 1)
                prefixWidth = paint.measureText(prefixText)
            }
            text = prefixText + suffixText
        }
    }
}

fun TextView.setFixTextEnd(oriStr: String, maxWidth: Int) {
    val originTextWidth = paint.measureText(oriStr)
    if (maxWidth > originTextWidth) {
        text = oriStr
    } else {
        var textWidth = paint.measureText(oriStr)
        var targetText = oriStr
        var isAddSuffix = false
        val sizeText = paint.measureText("映") // 刚好贴着裁剪会导致TextView后面的View的间距看上去过大，这里接受多裁剪一个字符
        while (textWidth > maxWidth - sizeText) { // 加一个buffer，设计同学能接受多裁一点
            targetText = targetText.substring(0, targetText.length - 2)
            textWidth = paint.measureText("$targetText…")
            isAddSuffix = true
        }
        text = if (isAddSuffix) "$targetText…" else targetText
    }
}

fun EditText.clear() {
    this.setText("")
}

//fun isLightColor(color: Int): Boolean {
//    return ColorUtils.calculateLuminance(color) >= 0.5
//}

fun View.isRTL(): Boolean {
    return context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
}



fun View.pickActivity(): Activity? {
    var ctx = this.context
    if (ctx is Activity) {
        return ctx
    }
    if (ctx is ContextWrapper && ctx.baseContext is Activity) {
        return ctx.baseContext as Activity
    }
    ctx = rootView.context
    return ctx as? Activity
}

// view 沉浸式适配
fun View.requestTopInset(topInset: Int) {
    when {
        this is ViewGroup && this.layoutParams.height < 0 -> {
            setPadding(
                paddingLeft,
                paddingTop + topInset,
                paddingRight,
                paddingBottom
            )
        }
        layoutParams.height == 0 -> {
            layoutParams = layoutParams.apply { height = topInset }
        }
        layoutParams is ViewGroup.MarginLayoutParams -> {
            layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                topMargin += topInset
            }
        }
    }
}
