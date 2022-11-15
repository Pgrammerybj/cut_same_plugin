package com.ss.ugc.android.editor.core.settings

import com.ss.ugc.android.editor.core.utils.DLog
import com.ss.ugc.android.editor.core.utils.GsonUtil


/**
 * @author: Jeff Beyond  <ouyanghaibing@bytedance.com>
 * @date: 2021/8/12
 */
object KVSettingsManager {

    const val TAG = "KVSettings"
    private val settingsMap: HashMap<String, Any> = hashMapOf()

    private val kvWriter = KVWriterImpl(settingsMap)
    private val kvReader = KVReaderImpl(settingsMap)

    fun init(kvSettings: IKVSettings) {
        kvSettings.onConfig(kvWriter)
        //设置日志开关
        val enableLog = get(SettingsKey.ENABLE_LOG, true)
        DLog.setEnableLog(enableLog)
        toJson()?.let { DLog.d(TAG, it) }
    }

    fun <T : Any> get(key: SettingsKey, defaultVal: T): T {
        return kvReader.get(key, defaultVal)
    }

    fun <T : Any> get(key: SettingsKey): T? {
        return kvReader.get(key)
    }


    private fun toJson(): String? {
        return GsonUtil.toJson(settingsMap)
    }

}