package com.ss.ugc.android.editor.track.diskcache

import java.security.MessageDigest

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
