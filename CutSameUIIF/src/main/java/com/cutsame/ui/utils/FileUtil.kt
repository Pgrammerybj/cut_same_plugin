package com.cutsame.ui.utils

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*

class FileUtil {
    companion object {

        //转换成字符串的时间
        private val mFormatBuilder = StringBuilder()
        private val mFormatter = Formatter(mFormatBuilder, Locale.getDefault())

        private const val LOCAL_RESOURCE = "LocalResource"
        const val CUTSAME_VIDEO_COVER = "cutsame_video_cover.jpg"

        fun getLocalResourcePath(mContext: Context): String {
            return mContext.getExternalFilesDir("assets")?.absolutePath + File.separator + LOCAL_RESOURCE
        }

        /**
         * 在APP退出的时候注意清理这个文件夹里面的内容
         */
        fun getExternalTmpSaveName(name: String = "", context: Context): String {
            val dir = context.getExternalFilesDir("tmp_save")
                ?: File(context.filesDir.absolutePath, "tmp_save")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir.absolutePath + File.separator + name
        }


        /**
         * 把毫秒转换成：1：20：30这样的形式
         */
        @JvmStatic
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
        private fun makeFilePath(filePath: String?, fileName: String?): File? {
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
        private fun makeRootDirectory(filePath: String?) {
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

        private fun isAssetsDir(assets: AssetManager, path: String): Boolean {
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
}
