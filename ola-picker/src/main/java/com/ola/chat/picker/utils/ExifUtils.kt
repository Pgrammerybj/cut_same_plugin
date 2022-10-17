package com.ola.chat.picker.utils

import android.media.ExifInterface
import android.text.TextUtils
import java.io.File
import java.io.IOException

object ExifUtils {
    private const val TAG = "ExifUtils"
    private const val DEGREE_90 = 90
    private const val DEGREE_180 = 180
    private const val DEGREE_270 = 270

    fun getExifOrientation(filepath: String): Int {
        var degree = 0
        if (TextUtils.isEmpty(filepath)) {
            return degree
        }

        if (!isFileExist(filepath)) {
            return degree
        }

        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(filepath)
        } catch (ex: IOException) {
            //ignore
        }

        // We only recognize a subset of orientation tag values.
        degree = when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1) ?: -1) {
            ExifInterface.ORIENTATION_ROTATE_90 -> DEGREE_90

            ExifInterface.ORIENTATION_ROTATE_180 -> DEGREE_180

            ExifInterface.ORIENTATION_ROTATE_270 -> DEGREE_270

            else -> 0
        }
        return degree
    }

    // 检测文件是否存在
    fun isFileExist(filePath: String?): Boolean {
        if (TextUtils.isEmpty(filePath)) {
            return false
        }
        val file = File(filePath)
        return file.exists() && file.length() > 0
    }
}