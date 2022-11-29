package com.ss.ugc.android.editor.core.utils

import android.content.Context
import java.io.*
import java.lang.StringBuilder
import java.nio.charset.StandardCharsets

/**
 * @date: 2021/3/1
 */
object AssetsUtils {
    fun readFile(context: Context?, filePath: String): String? {
        context ?: return null
        return try {
            val assetManager = context.assets
            BufferedReader(InputStreamReader(assetManager.open(filePath))).use {
                val stringBuilder = StringBuilder()
                var line: String?
                while (it.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                return stringBuilder.toString()
            }
        } catch (e: Exception) {
            DLog.e("AssetUtils#loadJson::jsonFilePath=$filePath, Exception = $e")
            null
        }
    }

    //读取json文件
    fun readJsonFile(fileName: String?): String? {
        val jsonStr: String
        return try {
            val jsonFile = File(fileName)
            val fileReader = FileReader(jsonFile)
            val reader: Reader =
                InputStreamReader(FileInputStream(jsonFile), StandardCharsets.UTF_8)
            var ch: Int
            val sb = StringBuilder()
            while (reader.read().also { ch = it } != -1) {
                sb.append(ch.toChar())
            }
            fileReader.close()
            reader.close()
            jsonStr = sb.toString()
            jsonStr
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
