package com.ss.ugc.android.editor.core.settings

/**
 * @author: Jeff Beyond  <ouyanghaibing@bytedance.com>
 * @date: 2021/8/11
 * @desc: Key-Value设置接口
 */
interface IKVSettings {
    fun onConfig(kvWriter: IKVWriter)
}