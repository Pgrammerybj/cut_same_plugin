package com.ss.ugc.android.editor.core.api

import androidx.lifecycle.MutableLiveData
import com.ss.ugc.android.editor.core.utils.DLog

/**
 * @author: Jeff Beyond  <ouyanghaibing@bytedance.com>
 * @date: 2021/8/23
 */
class LiveDataEnvVariables : IEnvVariables {

    companion object {
        const val TAG = "LiveDataEnvProperties"
    }

    private val envLiveDataMap = mutableMapOf<String, MutableLiveData<Any>>()

    var enableLog = true

    override fun <T : Any> set(key: String, value: T?, postOnMainThread: Boolean) {
        if (enableLog) {
            DLog.d(TAG, "setProperty::set->key=${key}, value = $value")
        }
        if (!envLiveDataMap.contains(key)) {
            val liveData = MutableLiveData<Any>()
            envLiveDataMap[key] = liveData
            if (postOnMainThread) {
                liveData.postValue(value)
            } else {
                liveData.value = value
            }
        } else {
            val liveData = envLiveDataMap[key]
            if (postOnMainThread) {
                liveData?.postValue(value)
            } else {
                liveData?.value = value
            }
        }
    }

    override fun <T : Any> get(key: String, defaultVal: T?): T? {
        if (envLiveDataMap.containsKey(key)) {
            try {
                val result = envLiveDataMap[key]?.value as T
                if (enableLog) {
                    DLog.d(TAG, "getProperty::key=${key}, value = $result")
                }
                return result
            } catch (e: NullPointerException) {
                // 避免大量打印 强转抛出的空指针异常
                if (enableLog) {
                    DLog.d(TAG, "get::key=$key, error cause = ${e.message}")
                }
            } catch (e: Exception) {
                DLog.e(TAG, "get::key=$key, error cause = ${e.message}", e)
            }
        }
        return defaultVal
    }

    override fun <T : Any> asLiveData(key: String): MutableLiveData<T> {
        if (!envLiveDataMap.containsKey(key)) {
            val liveData = MutableLiveData<Any>()
            envLiveDataMap[key] = liveData
        }
        return envLiveDataMap[key] as MutableLiveData<T>
    }
}