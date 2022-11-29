package com.ola.editor.kit.cutsame.customview

import android.view.View

abstract class GlobalDebounceOnClickListener : View.OnClickListener {
    @JvmOverloads
    constructor(delayInterval: Int = 300, isGlobal: Boolean = true) {
        // 这么写是因为主构造器无法使用 @JvmOverloads 注解
        this.delayInterval = delayInterval
        this.isGlobal = isGlobal
    }

    companion object {
        private var globalEnabled = true
    }

    private val delayInterval: Int
    private val isGlobal: Boolean

    private var localEnabled = true
    private var enabled
        get() = if (isGlobal) globalEnabled else localEnabled
        set(value) {
            if (isGlobal) globalEnabled = value else localEnabled = value
        }
    private val enableAgainRunnable = Runnable { enabled = true }

    override fun onClick(v: View) {
        if (enabled) {
            enabled = false
            v.postDelayed(enableAgainRunnable, delayInterval.toLong())
            doClick(v)
        }
    }

    abstract fun doClick(v: View)
}

fun View.setGlobalDebounceOnClickListener(run: (v:View) -> Unit) {
    setOnClickListener(object : GlobalDebounceOnClickListener() {
        override fun doClick(v: View) = run(v)
    })
}