package com.ss.ugc.android.editor.core.utils

import android.content.res.AssetManager
import android.util.Log
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * 文件工具类
 */
object FileUtil {

    //转换成字符串的时间
    private val mFormatBuilder = StringBuilder()
    private val mFormatter = Formatter(mFormatBuilder, Locale.getDefault())

    /**
     * 校验文件是否有效
     *
     * @param path
     * @return
     */
    fun check(path: String?): Boolean {
        val file = File(path)
        return file.exists() && file.isFile && file.length() > 0
    }

    /**
     * 把毫秒转换成：1：20：30这样的形式
     */
    fun stringForTime(timeMs: Long): String {
        val totalSeconds = (timeMs / 1000).toInt()
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        mFormatBuilder.setLength(0)
        return if (hours > 0) {
            mFormatter.format(
                "%d:%02d:%02d",
                hours,
                minutes,
                seconds
            ).toString()
        } else {
            mFormatter.format(
                "%02d:%02d",
                minutes,
                seconds
            ).toString()
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

    // 将字符串写入到文本文件中
    fun writeTxtToFile(strContent: String, filePath: String, fileName: String): String {
        //生成文件夹之后，再生成文件，不然会出错
        makeFilePath(filePath, fileName)
        val strFilePath = filePath + File.separator + fileName
        try {
            val file = File(strFilePath)
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            val raf = RandomAccessFile(file, "rw")
            raf.seek(file.length())
            raf.write(strContent.toByteArray())
            raf.close()
            return strFilePath
        } catch (e: Exception) {
            //igoner
        }
        return ""
    }

    //生成文件
    fun makeFilePath(filePath: String?, fileName: String?): File? {
        var file: File? = null
        makeRootDirectory(filePath)
        try {
            file = File(filePath, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }

    //生成文件夹
    fun makeRootDirectory(filePath: String?) {
        val file: File
        try {
            file = File(filePath)
            if (!file.exists()) {
                file.mkdir()
            }
        } catch (e: Exception) {
            Log.i("error:", e.toString() + "")
        }
    }

    /**
     * 递归拷贝Asset目录中的文件到rootDir中
     * Recursively copy the files in the Asset directory to rootDir
     * @param assets
     * @param path  assets 下的path  eg: resource/duet.bundle/duet.json
     * @param dstRootDir
     * @throws IOException
     */
    @Throws(IOException::class)
    fun copyAssets(assets: AssetManager, path: String, dstRootDir: String) {
        if (isAssetsDir(assets, path)) {
            val dir = File(dstRootDir + File.separator + path)
            check(!(!dir.exists() && !dir.mkdirs())) { "mkdir failed" }
            for (s in assets.list(path)!!) {
                copyAssets(assets, "$path/$s", dstRootDir)
            }
        } else {
            val input = assets.open(path)
            val dest = File(dstRootDir, path)
            copyToFileOrThrow(input, dest)
        }
    }

    fun isAssetsDir(assets: AssetManager, path: String): Boolean {
        try {
            val files = assets.list(path)
            return files != null && files.isNotEmpty()
        } catch (e: IOException) {
            Log.e("isAssetsDir", "isAssetsDir:", e)
        }
        return false
    }

    @Throws(IOException::class)
    private fun copyToFileOrThrow(inputStream: InputStream, destFile: File) {
        if (destFile.exists()) {
            return
        }
        val file = destFile.parentFile
        if (file != null && !file.exists()) {
            file.mkdirs()
        }
        inputStream.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}