package com.angelstar.ola.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/21
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: CK-CutSame_union
 * 参考文章：https://juejin.cn/post/7049983952496361503
 */
open class JsonUtils {
 
    /**
     * fromJson2List
     */
    inline fun <reified T> fromJson2List(json: String) = fromJson<List<T>>(json)

    /**
     * fromJson
     */
    inline fun <reified T> fromJson(json: String): T? {
        return try {
            val type = object : TypeToken<T>() {}.type
            return Gson().fromJson(json, type)
        } catch (e: Exception) {
            println("try exception,${e.message}")
            null
        }
    }

}