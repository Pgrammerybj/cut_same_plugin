package com.ss.ugc.android.editor.core.settings


/**
 * @author: Jeff Beyond  <ouyanghaibing@bytedance.com>
 * @date: 2021/8/11
 */
class KVWriterImpl(private val settingsMap: HashMap<String, Any>) : IKVWriter {

    override fun <T : Any> set(key: SettingsKey, value: T) {
        settingsMap[key.name] = value
    }

}