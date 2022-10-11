package com.cutsame.ui.template.play

import android.content.Context
import com.danikula.videocache.HttpProxyCacheServer
import java.io.File

object PlayCacheServer {
    private var proxy: HttpProxyCacheServer? = null

    fun getProxy(context: Context): HttpProxyCacheServer {
        val proxyServer = proxy
        return if (proxyServer != null) {
            proxyServer
        } else {
            val newProxy = newProxy(context)
            proxy = newProxy
            newProxy
        }
    }

    private fun newProxy(context: Context): HttpProxyCacheServer {
        val externalFilesDir = context.getExternalFilesDir("videoCache") ?: File(
            context.filesDir.absolutePath,
            "videoCache"
        )
        return HttpProxyCacheServer.Builder(context)
            .cacheDirectory(externalFilesDir)
            .maxCacheSize((1024 * 1024 * 1024).toLong())  // 1 Gb for cache
            .build()
    }
}