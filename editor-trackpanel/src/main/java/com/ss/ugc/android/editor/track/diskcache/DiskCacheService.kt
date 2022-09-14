package com.ss.ugc.android.editor.track.diskcache

import com.ss.ugc.android.editor.base.path.PathConstant
import com.ss.ugc.android.editor.base.utils.FileUtil
import com.ss.ugc.android.editor.track.utils.IOUtils
import java.io.File

object DiskCacheService {

    private val diskCache =
        DiskLruCacheWrapper.get(File(PathConstant.DISK_CACHE_DIR), 500 * 1024 * 1024)

    fun get(key: String) = diskCache.get(StringKey(key))

    fun put(key: String, file: File) {
        diskCache.put(StringKey(key), FileWriter(file))
    }

    fun put(key: String, data: ByteArray) {
        diskCache.put(StringKey(key), DataWriter(data))
    }

    fun put(key: String, writer: IWriter) {
        diskCache.put(StringKey(key), WriterWrapper(writer))
    }

    fun delete(key: String) {
        diskCache.delete(StringKey(key))
    }
}

private class FileWriter(private val source: File) : DiskCache.Writer {
    override fun write(file: File): Boolean {
        return IOUtils.copyFile(source, file)
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
