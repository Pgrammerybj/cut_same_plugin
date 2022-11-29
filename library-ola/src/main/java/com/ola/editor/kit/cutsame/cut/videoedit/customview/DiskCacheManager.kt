package com.ola.editor.kit.cutsame.cut.videoedit.customview

import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper
import com.ola.editor.kit.cutsame.AppContext
import com.ola.editor.kit.cutsame.utils.FileUtil
import java.io.File
import java.security.MessageDigest

object DiskCacheManager {

    private val diskCache: DiskCache? =
        DiskLruCacheWrapper.get(AppContext.videoFrameCacheFile, (500 * 1024 * 1024).toLong())

    fun get(key: String) = diskCache?.get(StringKey(key))

    fun put(key: String, file: File) {
        diskCache?.put(StringKey(key), FileWriter(file))
    }

    fun put(key: String, data: ByteArray) {
        diskCache?.put(StringKey(key), DataWriter(data))
    }

    fun put(key: String, writer: IWriter) {
        diskCache?.put(StringKey(key), WriterWrapper(writer))
    }

    fun delete(key: String) {
        diskCache?.delete(StringKey(key))
    }
}

private class FileWriter(private val source: File) : DiskCache.Writer {
    override fun write(file: File): Boolean {
        return FileUtil.copyFile(source, file)
    }
}

private class DataWriter(private val data: ByteArray) : DiskCache.Writer {
    override fun write(file: File): Boolean {
        return FileUtil.writeToFile(data, file)
    }
}

private class WriterWrapper(private val writer: IWriter) : DiskCache.Writer {
    override fun write(file: File): Boolean {
        return writer.write(file)
    }
}

interface IWriter {
    fun write(file: File): Boolean
}
class StringKey(private val key: String) : Key {
    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(key.toByteArray(Key.CHARSET))
    }

    override fun equals(other: Any?): Boolean {
        return key == (other as? StringKey)?.key
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}
