package com.ss.ugc.android.editor.core.settings

/**
 * @author: Jeff Beyond  <ouyanghaibing@bytedance.com>
 * @date: 2021/8/11
 */
interface IKVWriter {
    fun <T : Any> set(key: SettingsKey, value: T)
}

