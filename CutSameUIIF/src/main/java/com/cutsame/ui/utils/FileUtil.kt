package com.cutsame.ui.utils

import android.content.Context
import android.graphics.Bitmap
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

        fun copyFileToDraft(srcFilePath: String, dstDirPath: String, dstFileName: String): Boolean {
            if (srcFilePath.isEmpty())
                return false

            val srcFile = File(srcFilePath)
            val dstFile = File(dstDirPath + File.separator + dstFileName)
            val defaultFile = File(dstDirPath + File.separator + dstFileName + ".temp")
            if (dstFile.exists()) {
                dstFile.renameTo(defaultFile)
            }
            //开始拷贝字体文件
            val copySuccess = copyFile(srcFile, dstFile)
            if (!copySuccess)
                return false
            //修改拷贝的字体文件为默认文件名yonghuaiti.ttf
            defaultFile.delete()
            return true
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
                    val buffer = ByteArray(4096)
                    val input = BufferedInputStream(FileInputStream(srcFile))
                    val output = BufferedOutputStream(FileOutputStream(dstFile))
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

        fun copyDir(dirPath: String?, newDirPath: String?) {
            if (dirPath == null || dirPath.isEmpty() || newDirPath == null || newDirPath.isEmpty()) {
                return
            }
            try {
                val file = File(dirPath)
                if (!file.exists() && !file.isDirectory) {
                    return
                }
                val childFile = file.listFiles()
                if (childFile == null || childFile.isEmpty()) {
                    return
                }
                val newFile = File(newDirPath)
                newFile.mkdirs()
                for (fileTemp in childFile) {
                    if (fileTemp.isDirectory) {
                        copyDir(fileTemp.path, newDirPath + "/" + fileTemp.name)
                    } else {
                        copyFile(fileTemp.path, newDirPath)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun copyFile(filePath: String?, newDirPath: String?) {
            if (filePath == null || filePath.isEmpty()) {
                return
            }
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return
                }
                //判断目录是否存在，如果不存在，则创建
                val newDir = File(newDirPath)
                if (!newDir.exists()) {
                    newDir.mkdirs()
                }
                //创建目标文件
                val newFile: File? = newFile(newDirPath, file.name)
                val inputStream: InputStream = FileInputStream(file)
                val fos = FileOutputStream(newFile)
                val buffer = ByteArray(4096)
                var byteCount: Int
                while (inputStream.read(buffer).also { byteCount = it } !== -1) {
                    fos.write(buffer, 0, byteCount)
                }
                fos.flush()
                inputStream.close()
                fos.close()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }


        private fun newFile(filePath: String?, fileName: String?): File? {
            if (filePath == null || filePath.isEmpty() || fileName == null || fileName.isEmpty()) {
                return null
            }
            try {
                //判断目录是否存在，如果不存在，递归创建目录
                val dir = File(filePath)
                if (!dir.exists()) {
                    dir.mkdirs()
                }

                //组织文件路径
                val sbFile = java.lang.StringBuilder(filePath)
                if (!filePath.endsWith("/")) {
                    sbFile.append("/")
                }
                sbFile.append(fileName)

                //创建文件并返回文件对象
                val file = File(sbFile.toString())
                if (!file.exists()) {
                    file.createNewFile()
                }
                return file
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
            return null
        }

        fun removeFile(filePath: String?) {
            if (filePath == null || filePath.isEmpty()) {
                return
            }
            try {
                val file = File(filePath)
                if (file.exists()) {
                    removeFile(file)
                }
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
        }

        private fun removeFile(file: File) {
            //如果是文件直接删除
            if (file.isFile) {
                file.delete()
                return
            }
            //如果是目录，递归判断，如果是空目录，直接删除，如果是文件，遍历删除
            if (file.isDirectory) {
                val childFile = file.listFiles()
                if (childFile == null || childFile.isEmpty()) {
                    file.delete()
                    return
                }
                for (f in childFile) {
                    removeFile(f)
                }
                file.delete()
            }
        }
    }
}
