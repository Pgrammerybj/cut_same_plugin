package com.ss.ugc.android.editor.core.settings


interface IKVReader {
    fun <T : Any> get(key: SettingsKey, defaultVal: T): T

    fun <T : Any> get(key: SettingsKey): T?
}