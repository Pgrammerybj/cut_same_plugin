package com.cutsame.ui.gallery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.content.Context
import com.cutsame.ui.gallery.album.model.MediaData
import com.cutsame.ui.gallery.album.model.TitleMediaType
import com.cutsame.ui.gallery.album.model.reposity.MediaDataManager

/**
 * 提供相册素材数据获取能力
 */
class GalleryDataViewModel(application: Application) : AndroidViewModel(application) {

    private val liveDataList = mapOf(
        Pair(TitleMediaType.TYPE_ALL, MutableLiveData<List<MediaData>>()),
        Pair(TitleMediaType.TYPE_IMAGE, MutableLiveData<List<MediaData>>()),
        Pair(TitleMediaType.TYPE_VIDEO, MutableLiveData())
    )

    fun getGalleryMaterialData(
        mediaType: TitleMediaType,
        context: Context
    ): LiveData<List<MediaData>> {
        val mediaData = when (mediaType) {
            TitleMediaType.TYPE_ALL -> MediaDataManager()
                .loadAllData(context)
            TitleMediaType.TYPE_VIDEO -> MediaDataManager()
                .loadAllData(context)
                .filter { it.isVideo() }
            TitleMediaType.TYPE_IMAGE -> MediaDataManager()
                .loadAllData(context)
                .filter { it.isImage() }
        }
        liveDataList[mediaType]?.value = mediaData
        return liveDataList[mediaType] ?: error("not have live data")
    }
}