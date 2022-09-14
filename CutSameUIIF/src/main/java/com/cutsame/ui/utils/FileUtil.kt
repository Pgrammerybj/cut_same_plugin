package com.cutsame.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.*

class FileUtil {
    companion object {
        fun getExternalTmpSaveName(name: String = "", context: Context): String {
            val dir = context.getExternalFilesDir("tmp_save")
                ?: File(context.filesDir.absolutePath, "tmp_save")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir.absolutePath + File.separator + name
        }


        /**
         * 获取合适的 cache 路径.
         *
         * android 官方推荐使用 external-storage 的 cache 路径, 当 external-storage
         * 不可用的时候, 回退到系统内置的 cache 路径.
         *
         * @see Context.getCacheDir
         * @see Context.getExternalCacheDir
         */
        @JvmStatic
        fun getCacheDir(context: Context): File? {
            var cacheDir = context.externalCacheDir
            if (cacheDir == null) {
                cacheDir = context.cacheDir
            }
            return cacheDir
        }

        /**
         * @return sd卡是否可写
         */
        @JvmStatic
        fun isSdcardWritable(): Boolean {
            try {
                return Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
            } catch (ignored: Exception) {
            }

            return false
        }

        fun saveBmpToFile(bmp: Bitmap?, file: File?, format: Bitmap.CompressFormat): Boolean {
            if (null == bmp || null == file) {
                return false
            }
            val baos = ByteArrayOutputStream()
            bmp.compress(format, 100, baos)
            return writeToFile(baos.toByteArray(), file)
        }

        fun writeToFile(data: ByteArray, file: File): Boolean {
            var fos: FileOutputStream? = null
            return try {
                fos = FileOutputStream(file)
                fos.write(data)
                fos.flush()
                true
            } catch (e: IOException) {
                false
            } finally {
                if (null != fos) {
                    try {
                        fos.close()
                    } catch (e: IOException) {
                    }
                }
            }
        }
        @JvmStatic
        fun copyFile(srcFile: File, dstFile: File): Boolean {
            if (srcFile.exists() && srcFile.isFile) {
                if (dstFile.isDirectory) {
                    return false
                }
                if (dstFile.exists()) {
                    dstFile.delete()
                }
                try {
                    val byteNumber = 2048
                    val buffer = ByteArray(byteNumber)
                    val input = BufferedInputStream(
                        FileInputStream(srcFile)
                    )
                    val output = BufferedOutputStream(
                        FileOutputStream(dstFile)
                    )
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) {
                            break
                        }
                        output.write(buffer, 0, count)
                    }
                    input.close()
                    output.flush()
                    output.close()
                    return true
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return false
        }

        @JvmStatic
        fun readJsonFile(fileName: String?): String? {
            var jsonStr = ""
            return try {
                val jsonFile = File(fileName)
                val fileReader = FileReader(jsonFile)
                val reader: Reader = InputStreamReader(FileInputStream(jsonFile), "utf-8")
                var ch = 0
                val sb = StringBuffer()
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
}
