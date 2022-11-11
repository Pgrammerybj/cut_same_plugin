package com.cutsame.ui.exten

import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.android.asCoroutineDispatcher
import java.lang.reflect.Constructor

var FastMain = Looper.getMainLooper().asHandler().asCoroutineDispatcher("fast-main")

internal fun Looper.asHandler(): Handler {

    if (Build.VERSION.SDK_INT >= 28) {
        // TODO compile against API 28 so this can be invoked without reflection.
        val factoryMethod = Handler::class.java.getDeclaredMethod("createAsync", Looper::class.java)
        return factoryMethod.invoke(null, this) as Handler
    }

    val constructor: Constructor<Handler>
    try {
        constructor = Handler::class.java.getDeclaredConstructor(
            Looper::class.java,
            Handler.Callback::class.java, Boolean::class.javaPrimitiveType
        )
    } catch (ignored: NoSuchMethodException) {
        return Handler(this)
    }
    return constructor.newInstance(this, null, true)
}