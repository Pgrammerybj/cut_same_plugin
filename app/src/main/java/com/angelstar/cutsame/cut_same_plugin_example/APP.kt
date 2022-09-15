package com.angelstar.cutsame.cut_same_plugin_example

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.bytedance.ies.cutsame.util.SharedPreferencesUtils
import com.cutsame.editor.EditorManager
import com.cutsame.solution.AuthorityConfig
import com.cutsame.solution.CutSameSolution
import com.cutsame.solution.EffectFetcherConfig
import com.cutsame.solution.TemplateFetcherConfig
import com.cutsame.ui.ApiUtil
import com.ss.android.ugc.cut_log.LogConfig
import com.ss.android.ugc.cut_log.LogIF
import com.ss.android.ugc.cut_log.LogWrapper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class APP : Application() {
    private val TAG = "cut_APP"
    private val LICENSE_NAME = "labcv_test_20220629_20221231_com.bytedance.solution.cutsame_4.0.2.6union.licbag"
    private val AUDIO_AUTH_KEY = "iWwiXvXhlN"
    private val AUDIO_AUTH_TOKEN = "RFUnZxken6kOX3vPi8qfdVqP398MoG+N3Hz1VfbMLgUl+mH1O/uA/lvxITb6T/8orIt3jsH0+3tlX2vlyM9F+To/cCIctLgX6GbSzOXGmUEHnHfkAljqN6/KpRZkqQ4zrQGsewFmdqH68vuazV8qKLRFsuiVoKNU0xd0jDuvAQ8HqHLrWWVwU6vP+AdDlXB9mOnql34Q25aZWb95XTpJJpAEr3OZTNSep7J1TgECuv+8thrOZl/8ZighHbgub/5PH1cfjX1UboZwQNV6guJQOd6+T13CYxffg7cLGfS74ulJc5h4yYX/M3wd5WVfsfqqIv5QSq6T2yjM5zOEIc/lSKzLZTvXcSYr+QB4eW5Fg39PpEEpxc1ZsTqCxTvsQZ/vmVWA9Zt790bngbHKG7I31Bzm8qHikFV5s8ldVmWliGrYh+u51l/MPIwaGDkFr5e+qIl79gKCUJY9jOgXX2K4Lu1Dm1eE1HARRv2EA675ZgePKRtM8tirrQEwdEzGc1XROKlTvMNfF7Zel8HiaCHndfyAIwT+QjPDDT7D3IwL/2L+pvJ8tt7DqaZApqBNs1rYry7DUlR1NdERLbXPLNUq5kDNlULLFyVoMuDt3JkpnJYd7L4x0w81c77bHPOrkmT2VCxOmxy/5wa4PWXOVZVcCSGykQk3wKz21y7AaOQNvpQ4iqCF87bBiqdfo82b/6eaMr+/sRJYZlZ9D5fVUbveGj6D5lQBz/ob1on6XozWSHTrwjQDKE1YG7Eykq5aQ5i1fCsR6tgfSayGH6KySkja0547mBMJsjnYeuWX0oJ8gCNFWXFkdEjH46Hs447eheQWDmLVWr/eYnxsB1ShN8oeGPi5hXvY2fkaAjcn34Rg59NfUKhEbhvGOjEYpM74RO2lvckJnJMFP2PeEl1P7LHJFFWISSDSDhSBvuRzuPjZvF3rv9TbrBkJ+Pdch7SnUGjiNIVN7/jX5bSXhYRvkVIh+PUxIgvDa31Fujy6PcjHMEJiLg4L6UFyvQY1BgXe5NwPNHozw3ujjvMVN+7TQIkiNdsG9ZHdftKZlBJmi1uomN5x8nXcRAyj7P+8g1Y9JdM0CS3xh9IBF6g+oPYd1P38XMK2LqDXNxBkyL8pQgYdjmBHJ/X4PFSsPPGcXiwpK9Gwq4DxLI0P104e4Lhsn5yRrThy5xb+mXx/phHv2zKyOPPtXuD/Pw36EP38No/Jqm4r2hdeQK908ouYaTnGllukfp96voPFZNmlQzhyT6Ayv4bekruJjsBy/HF05cbcquRSdfLb8rBK07CR/oYpcSgZtivG0+7jmXF4jf8Z7ktTJ/RNroK2X1uDoC44nsYfRxNG3pEPSNYlEDQfYw6BuoJ4dg=="



    override fun onCreate() {
        super.onCreate()
        SharedPreferencesUtils.init(this)
        val logWrapper = LogWrapper().apply {
            init(
                logConfig = LogConfig.Builder()
                    .logcatLevel(LogIF.LOG_LEVER.DEBUG)
                    .localLevel(LogIF.LOG_LEVER.WARNING)
                    .toLocal(true)
                    .toLogcat(true)
                    .showThreadInfo(true)
                    .localPath(Environment.getExternalStorageDirectory().absolutePath)
                    .build()
            )
        }
        Log.d(TAG, "order_id: ${ApiUtil.extra_param_order_id}")
        CutSameSolution.setLogIf(logWrapper)
        CutSameSolution.init(
            context = this,
            //authorityConfig：鉴权配置
            authorityConfig = AuthorityConfig.Builder()
                .licensePath(getLicenseFilePath())
                .audioAuthKey(AUDIO_AUTH_KEY)
                .audioAuthToken(AUDIO_AUTH_TOKEN)
                .authorityListener(object : AuthorityConfig.AuthorityListener {
                    override fun onError(errorCode: Int, errorMsg: String) {
                        Log.d(TAG, "onError $errorCode $errorMsg")
                        Toast.makeText(this@APP, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }).build(),

            //http://ck-common.byteintl.com 海外域名
            //http://common.voleai.com  国内域名
            //templateFetcherConfig: 模版拉取配置
            templateFetcherConfig = TemplateFetcherConfig.Builder()
                .host(ApiUtil.host)
                .pageSize(20)
                .build(),

            //effectFetcherConfig: 特效拉取配置
            effectFetcherConfig = EffectFetcherConfig.Builder()
                .host(ApiUtil.host)
                .modelPath("/api/modellistinfo")
                .effectLitPath("/api/effectlist")
                .effectLitExtraMap(
                    mutableMapOf(
                        "order_id" to ApiUtil.extra_param_order_id
                    )
                )
                .build()
        )
        EditorManager.initEditor(this)
    }

    private fun getExternalLicenseName(name: String = "test.licbag", context: Context): String {
        val dir = context.getExternalFilesDir("license")
            ?: File(context.filesDir.absolutePath, "license")
        Log.d(TAG, " externalLicenseName: ${dir.path}")
        return dir.absolutePath + File.separator + name
    }

    private fun getLicenseFilePath(): String {
        val externalLicenseName = getExternalLicenseName(context = this)
        Log.d(TAG, "externalLicenseFilePath: $externalLicenseName")
        val externalLicenseNameFile = File(externalLicenseName)
        if (externalLicenseNameFile.exists()) {
            Log.d(TAG, "externalLicenseFilePath is exit ")
            return externalLicenseName
        }
        val copyToPath = applicationContext.filesDir.toString() + "/$LICENSE_NAME"
        copyAssets(applicationContext, LICENSE_NAME, copyToPath)
        Log.d(TAG, "copyToPath: $copyToPath")
        return copyToPath
    }

    private fun copyAssets(context: Context, assetName: String, dstFilePath: String) {
        val outFile = File(dstFilePath)
        if (outFile.exists()) { // 已存在则不复制
            Log.d(TAG, "dstFilePath is exists")
            return
        }
        var inputStream: InputStream? = null
        var outStream: OutputStream? = null
        try {
            inputStream = context.assets.open(assetName)
            outStream = FileOutputStream(outFile)
            inputStream.copyTo(outStream)
        } finally {
            outStream?.close()
            inputStream?.close()
        }
    }
}