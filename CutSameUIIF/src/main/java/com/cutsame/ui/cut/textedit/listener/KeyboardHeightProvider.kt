package com.cutsame.ui.cut.textedit.listener

import android.app.Activity
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.PopupWindow

open class KeyboardHeightProvider(private val activity: Activity) {

    private val parentView: View = activity.findViewById(android.R.id.content)
    private val keyboardAdjustWindow = KeyboardAdjustWindow(activity)
    private val keyboardIgnoreWindow = KeyboardIgnoreWindow(activity)

    private var keyboardIgnoreRect = Rect()

    private val screenOrientation: Int
        get() = activity.resources.configuration.orientation

    private var observer: ((height: Int, orientation: Int) -> Unit)? = null

    init {
        keyboardAdjustWindow.onGlobalLayout = {
            handleOnKeyboardStateChanged()
        }
        // 因为popupWindow的Size始终是受限于Activity的，
        // 如果Activity的输入模式是adjustPan，那弹出输入法后，popupWindow的高度也会变小
        // 所以这里只记录一次，之后就可以关闭忽略键盘的popupWindow
        // 当然，这种方式还是可能在一打开adjustPan的Activity就立即显示键盘的场景下出现问题
        keyboardIgnoreWindow.onGlobalLayout = {
            if (keyboardIgnoreRect.height() == 0) {
                keyboardIgnoreWindow.popupView.getWindowVisibleDisplayFrame(keyboardIgnoreRect)
            }

            if (keyboardIgnoreWindow.isShowing) {
                keyboardIgnoreWindow.dismiss()
            }
        }
    }

    fun start() {
        if (parentView.windowToken == null) return
        if (!keyboardIgnoreWindow.isShowing) {
            keyboardIgnoreWindow.setBackgroundDrawable(ColorDrawable(0))
            keyboardIgnoreWindow.showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0)
        }
        if (!keyboardAdjustWindow.isShowing) {
            keyboardAdjustWindow.setBackgroundDrawable(ColorDrawable(0))
            keyboardAdjustWindow.showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0)
        }
    }

    fun close() {
        this.observer = null
        keyboardIgnoreWindow.dismiss()
        keyboardAdjustWindow.dismiss()
    }

    fun setKeyboardHeightObserver(observer: (height: Int, orientation: Int) -> Unit) {
        this.observer = observer
    }

    private fun handleOnKeyboardStateChanged() {
        val keyboardAdjustRect = Rect()
        keyboardAdjustWindow.popupView.getWindowVisibleDisplayFrame(keyboardAdjustRect)

        if (keyboardIgnoreRect.height() > 0) {
            val height = keyboardIgnoreRect.bottom - keyboardAdjustRect.bottom
            notifyKeyboardHeightChanged(height, screenOrientation)
        }
    }

    private fun notifyKeyboardHeightChanged(height: Int, orientation: Int) {
        observer?.invoke(height, orientation)
    }
}

private class KeyboardAdjustWindow(activity: Activity) : PopupWindow(activity) {
    val popupView: View = FrameLayout(activity)
    var onGlobalLayout: (() -> Unit)? = null

    init {
        val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        popupView.layoutParams = params
        contentView = popupView

        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        inputMethodMode = INPUT_METHOD_NEEDED

        width = 0
        height = WindowManager.LayoutParams.MATCH_PARENT

        popupView.viewTreeObserver.addOnGlobalLayoutListener {
            onGlobalLayout?.invoke()
        }
    }
}

private class KeyboardIgnoreWindow(activity: Activity) : PopupWindow(activity) {
    val popupView: View = FrameLayout(activity)
    var onGlobalLayout: (() -> Unit)? = null

    init {
        val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        popupView.layoutParams = params
        contentView = popupView

        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        inputMethodMode = INPUT_METHOD_NOT_NEEDED

        width = 0
        height = WindowManager.LayoutParams.MATCH_PARENT

        popupView.viewTreeObserver.addOnGlobalLayoutListener {
            onGlobalLayout?.invoke()
        }
    }
}
