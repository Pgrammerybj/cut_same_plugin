package com.ola.chat.picker.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.ola.chat.picker.album.AlbumFragment
import com.ola.chat.picker.album.model.MediaData
import com.ola.chat.picker.entry.ImagePickConfig
import com.ola.chat.picker.entry.MediaItem
import com.ola.chat.picker.utils.PickerConstant
import java.io.File

/**
 * 提供素材操作能力，包括增加素材，删除素材
 */
class GalleryPickerViewModel(application: Application) :
    AndroidViewModel(application) {
    companion object {
        const val TAG: String = "TGalleryPickerViewModel"
    }

    val currentPickIndex = MutableLiveData<Int>()
    val preProcessPickItem = MutableLiveData<List<MediaItem>>()
    val loadingEvent = MutableLiveData<Boolean>()

    val addItem = MutableLiveData<MediaItem?>()
    val deleteItem = MutableLiveData<MediaItem?>()
    val processPickItem = MutableLiveData<ArrayList<MediaItem>>()
    val processPickMediaData = ArrayList<MediaData>()
    val pickFull = MutableLiveData<Boolean>()
    val fragmentMap = mutableMapOf<PickerConstant.TabType, Fragment>()
    var curMaterial: MediaItem? = null


    fun init(
        mediaItems: ArrayList<MediaItem>?,
        preMediaItems: ArrayList<MediaItem>?
    ) {

        mediaItems?.forEach {
            it.source = ""
            it.sourceStartTime = 0
            it.mediaSrcPath = ""
        }
        processPickItem.value = mediaItems
        preProcessPickItem.value = preMediaItems
        setSelected(0)
        pickFull.value = false
    }

    fun setSelected(pos: Int) {
        if (pos == currentPickIndex.value) {
            return
        }
        currentPickIndex.value = pos
        processPickItem.value?.let { list ->
            if (pos < list.size) {
                val mediaItem = list[pos]
                curMaterial = mediaItem
            }

        }
    }

    fun updateProcessPickItem(mediaItem: MediaItem) {
        Log.e(
            TAG,
            "updateProcessPickItem mediaItem = ${mediaItem.materialId} ${mediaItem.source}"
        )
        processPickItem.value?.let { mediaItems ->
            mediaItems.forEachIndexed { index, item ->
                if (item.materialId == mediaItem.materialId) {
                    mediaItems[index] = mediaItems[index].copy(
                        sourceStartTime = mediaItem.sourceStartTime,
                        crop = mediaItem.crop
                    )
                }
            }
            processPickItem.value = mediaItems
        }
    }

    fun pickOne(path: String, video: Boolean, duration: Long): Boolean {
        val mediaData = if (video) {
            MediaData(
                "video/mp4",
                path,
                Uri.fromFile(File(path)),
                "",
                0,
                0,
                0,
                0,
                0,
                duration,
                0
            )
        } else {
            MediaData(
                "image/jpeg",
                path,
                Uri.fromFile(File(path)),
                "",
                0,
                0,
                0,
                0,
                0,
                duration,
                0
            )
        }
        return pickOne(mediaData)
    }

    fun pickOne(one: MediaData): Boolean {
        Log.e(TAG, "pickOne MediaData = ${one.toString()}")
        val index = currentPickIndex.value!!
        if (index >= processPickItem.value?.size ?: 0) {
            return false
        }
        processPickItem.value?.let { mediaItems ->
            return if (one.isImage() || one.duration >= mediaItems[index].duration) {
                // MediaData => MediaItem
                mediaItems[index] = mediaItems[index].copy(
                    source = one.path,
                    mediaSrcPath = one.path,
                    type = if (one.isVideo()) MediaItem.TYPE_VIDEO else MediaItem.TYPE_PHOTO,
                    oriDuration = if (one.isVideo()) one.duration else mediaItems[index].duration
                )
                processPickMediaData.add(
                    index.coerceAtMost(processPickMediaData.size),
                    one
                )
                processPickItem.value = mediaItems
                addItem.value = mediaItems[index]

                pickFull.value = isFull(processPickItem.value)
                // notify
                selectNext(mediaItems)
                true
            } else {
                false
            }
        }
        return false
    }

    fun deleteOne(position: Int) {
        Log.e(TAG, "deleteOne position = $position")
        processPickItem.value?.let { mediaItems ->
            val delete = mediaItems[position]
            mediaItems[position] = delete.copy(source = "", mediaSrcPath = "")
            processPickItem.value = mediaItems
            deleteItem.value = delete

            var realIndex = 0
            processPickItem.value?.forEachIndexed { index, mediaItem ->
                if (index < position && mediaItem.getUri() != Uri.EMPTY) {
                    realIndex++
                }
            }
            processPickMediaData.removeAt(realIndex)

            pickFull.value = isFull(processPickItem.value)

            selectNext(mediaItems)
        }
    }

    private fun selectNext(
        mediaItems: ArrayList<MediaItem>
    ) {
        if (pickFull.value != true) {
            var circleIndex = 0
            while (circleIndex < mediaItems.size && mediaItems[circleIndex].getUri() != Uri.EMPTY) {
                circleIndex++
            }
            if (circleIndex < mediaItems.size) {
                setSelected(circleIndex)
            }
        }
    }

    fun isFull(mediaList: List<MediaItem>?): Boolean {
        if (mediaList != null) {
            mediaList.forEach {
                if (it.getUri() == Uri.EMPTY) {
                    return false
                }
            }
            return true
        } else {
            return false
        }
    }

    fun whetherExistPickData(data: MediaData): Boolean {
        processPickItem.value?.forEach {
            if (it.source == data.path) {
                return true
            }
        }
        return false
    }

    fun getFragmentByType(
        tabType: PickerConstant.TabType,
        isCutSameScene: Boolean,
        imagePickConfig: ImagePickConfig?
    ): Fragment {
        val fragment = fragmentMap[tabType]
        if (fragment != null) {
            return fragment
        }
        return AlbumFragment(isCutSameScene,imagePickConfig).apply {
            fragmentMap[tabType] = this
        }
    }
}