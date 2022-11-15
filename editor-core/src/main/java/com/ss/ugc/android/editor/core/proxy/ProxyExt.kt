package com.ss.ugc.android.editor.core.proxy

import java.lang.reflect.Proxy

inline fun <reified T : Any> T.dynamicProxy(): T {
    val dynamicProxy = DynamicProxy(this)
    val javaClass = T::class.java
    return Proxy.newProxyInstance(classLoader(), arrayOf(javaClass), dynamicProxy) as T
}

fun Any.classLoader(): ClassLoader? {
    return this::class.java.classLoader
}
