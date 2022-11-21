package com.cutsame.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.*
import java.nio.charset.StandardCharsets

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
         * 校验文件是否有效
         *
         * @param path
         */
        @JvmStatic
        fun check(path: String?): Boolean {
            val file = File(path)
            return file.exists() && file.isFile && file.length() > 0
        }


        //读取json文件
        fun readJsonFile(fileName: String?): String {
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
                ""
            }
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
    }
}
