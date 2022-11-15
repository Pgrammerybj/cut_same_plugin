package com.ss.ugc.android.editor.core.api

import androidx.lifecycle.MutableLiveData

/**
 * @author: Jeff Beyond  <ouyanghaibing@bytedance.com>
 * @date: 2021/8/23
 */
interface IEnvVariables {

    fun <T : Any> set(key: String, value: T?, postOnMainThread: Boolean = false)

    fun <T : Any> get(key: String, defaultVal: T? = null): T?

    fun <T : Any> asLiveData(key: String): MutableLiveData<T>
}


