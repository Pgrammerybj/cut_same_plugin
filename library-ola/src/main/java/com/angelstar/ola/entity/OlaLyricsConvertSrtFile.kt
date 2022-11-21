package com.angelstar.ola.entity;

import android.content.Context
import com.cutsame.ui.utils.JsonHelper
import com.ss.ugc.android.editor.core.api.params.AudioParam
import com.ss.ugc.android.editor.core.utils.FileUtil
import java.io.File

/**
 * @Author：yangbaojiang
 * @Date: 2022/11/16 15:48
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 将录制的歌词转换成火山歌词贴纸需要的格式
 * @Wiki：https://bytedance.feishu.cn/docx/ODGxdYOQAoNvkHxx8ZBcZCkanwf
 */

object OlaLyricsConvertSrtFile {

    private const val LOCAL_RESOURCE = "LocalResource"

    fun startConvert(audioMixingEntry: AudioMixingEntry, mContext: Context): AudioParam? {
        if (audioMixingEntry.singTimeLyricList.size == 0) {
            return null
        }
        val header = SongHeader(audioMixingEntry.songName, "artist", "musicBy", "writtenBy")
        val lyricsList = ArrayList<SongContent>()
        audioMixingEntry.singTimeLyricList.forEach {
            //:todo写死逻辑需要处理
            lyricsList.add(SongContent(it.lyric, (it.startTime / 1000 - 25).toString()))
        }

        //1️⃣获取到SRT格式的歌词文件实体
        val lyricsSrtEntry = LyricsSrtEntry(header, lyricsList)
        //2️⃣TODO：将实体装换成JSON写入文件
        val lyricsSrtString = JsonHelper.toJsonString(lyricsSrtEntry)
        val localResourcePath = getLocalResourcePath(mContext)
        val audioPath = getLocalResourcePath(mContext) + File.separator
        val lyricName = audioMixingEntry.songName
        FileUtil.writeTxtToFile(
            lyricsSrtString,
            localResourcePath,
            "$lyricName.json"
        )

        //将对应的asset文件也一并copy
        FileUtil.copyAssets(mContext.assets, "lyricStyle", localResourcePath)
        return AudioParam(
            audioName = lyricName,
            audioPath = audioPath + "ola_jackyang_lyrics",
            startTime = 0,
            isAudioEffect = false,
            srtPath = "$audioPath$lyricName.json"
        )
    }


    private fun getLocalResourcePath(mContext: Context): String {
        return mContext.getExternalFilesDir("assets")?.absolutePath + File.separator + LOCAL_RESOURCE
    }
}
