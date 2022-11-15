package com.ss.ugc.android.editor.core.settings

import com.ss.ugc.android.editor.core.utils.DLog

/**
 * @author: Jeff Beyond  <ouyanghaibing@bytedance.com>
 * @date: 2021/8/12
 */
class KVReaderImpl(private val settingsMap: HashMap<String, Any>) : IKVReader {

    companion object {
        const val TAG = "KVReaderImpl"
    }

    override fun <T : Any> get(key: SettingsKey, defaultVal: T): T {
        var result: T = defaultVal
        if (settingsMap.containsKey(key.name)) {
            try {
                result = settingsMap[key.name] as T
            } catch (e: Exception) {
                DLog.e(TAG, "get::key=$key, error cause = ${e.message}")
            }
        }
        return result
    }

    override fun <T : Any> get(key: SettingsKey): T? {
        if (settingsMap.containsKey(key.name)) {
            try {
                return settingsMap[key.name] as T
            } catch (e: Exception) {
                DLog.e(TAG, "get::key=$key, error cause = ${e.message}")
            }
        }
        return null
    }
}