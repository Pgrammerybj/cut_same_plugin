package com.ola.editor.kit.cutsame.cut.videoedit.customview

import android.graphics.Bitmap
import android.util.LruCache
import androidx.annotation.MainThread

object LruFrameBitmapCache {

    private var lruFrameCache = LruCache<String, Bitmap>(100)
    private var cachePath = mutableListOf<String>()

    fun getKey(path: String, position: Int): String {
        return "$path#$position"
    }

    @MainThread
    fun clearCache() {
        lruFrameCache.evictAll()
        cachePath.clear()
    }

    @MainThread
    fun putBitmap(key: String, bitmap: Bitmap) {
        lruFrameCache.put(key, bitmap)
    }

    @MainThread
    fun getBitmap(key: String): Bitmap? {
        return lruFrameCache.get(key)
    }

    fun addCachePath(path: String) {
        cachePath.add(path)
    }

    fun isCachedPath(path: String): Boolean {
        return cachePath.contains(path)
    }
}
