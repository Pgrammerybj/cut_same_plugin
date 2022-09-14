package com.cutsame.ui.gallery.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import android.view.SurfaceView
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.bytedance.ies.nle.editor_jni.NLEModel
import com.cutsame.solution.source.SourceInfo
import com.cutsame.ui.gallery.album.AlbumFragment
import com.cutsame.ui.gallery.album.model.MediaData
import com.cutsame.ui.gallery.camera.EffectHelper
import com.cutsame.ui.gallery.camera.IRecordCamera
import com.cutsame.ui.gallery.camera.RecordCameraAdapter
import com.cutsame.ui.gallery.data.TabType
import com.cutsame.ui.utils.runOnUiThread
import com.ss.android.ugc.cut_log.LogUtil
import com.ss.android.ugc.cut_ui.MediaItem
import com.ss.android.vesdk.VERecorder
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
    val fragmentMap = mutableMapOf<TabType, androidx.fragment.app.Fragment>()
    val recordCamera by lazy { RecordCameraAdapter() }
    val effectHelper: EffectHelper by lazy { EffectHelper() }
    var curMaterial: MediaItem? = null
    private var isOnCamera = false

    fun init(
        mediaItems: ArrayList<MediaItem>,
        preMediaItems: ArrayList<MediaItem>?,
        cameraCallback: IRecordCamera.CameraCallback,
        videoCache: String
    ) {

        mediaItems.forEach {
            it.source = ""
            it.sourceStartTime = 0
            it.mediaSrcPath = ""
        }
        processPickItem.value = mediaItems
        preProcessPickItem.value = preMediaItems
        setSelected(0)
        pickFull.value = false
        recordCamera.setCameraCallBack(cameraCallback)
        effectHelper.init(getApplication(), videoCache)//初始化效果效果逻辑
    }

    public fun setSelected(pos: Int) {
        LogUtil.e(TAG, "setSelected  pos = ${pos}")
        if (pos == currentPickIndex.value) {
            return
        }
        currentPickIndex.value = pos
        processPickItem.value?.let { list ->
            if (pos < list.size) {
                val mediaItem = list[pos]
                effectHelper.switchMaterial(mediaItem, list, isOnCamera)
                curMaterial = mediaItem
                updateCutSameConstraints()//更新当前素材的约束信息
            }

        }
    }

    private fun updateCutSameConstraints(changeRenderSize: Boolean = false) {
        runOnUiThread {
            recordCamera.updateConstraints(
                effectHelper.cutSameWidth, effectHelper.cutSameHeight,
                effectHelper.cutSameDuration, changeRenderSize
            )
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

    fun getFragmentByType(tabType: TabType): Fragment {
        val fragment = fragmentMap[tabType]
        if (fragment != null) {
            return fragment
        }
        return when (tabType) {
            TabType.Camera -> {
                recordCamera.getCameraInstance()
            }
            TabType.Album -> {
                AlbumFragment()
            }
        }.apply {
            fragmentMap[tabType] = this
        }
    }

    fun onBackPressed(): Boolean {
        if (isOnCamera) {
            return recordCamera.onBackPressed()
        }
        return false
    }

    fun onCameraInit(
        recorder: VERecorder,
        lifecycle: Lifecycle,
        sfView: SurfaceView?
    ) {
        updateCutSameConstraints(true)//更新当前素材的约束信息
        effectHelper.onCameraInit(recorder, lifecycle, sfView)
    }


    fun onRecorderNativeInit(ret: Int, msg: String) {
        updateCutSameConstraints()//更新当前素材的约束信息
    }

    fun loadTemplateRes(sourceInfo: SourceInfo) {
        loadingEvent.postValue(true)
        effectHelper.loadTemplateRes(sourceInfo) {
            loadingEvent.postValue(false)
        }
    }

    /**
     * 拍摄的视频/图片，确认保存时回调
     *
     * @param isVideo 是否视频
     */
    fun onConfirmVideoAction(isVideo: Boolean) {
        recordCamera.onConfirmPreview(isVideo)
    }

    fun fillMatch(path: String): NLEModel? {
        return effectHelper.fillMatch(path)
    }

    /**
     * 相机可见状态变化时回调
     */
    fun onCameraHiddenChanged(hidden: Boolean) {
    }

    fun onSelectChange(onCamera: Boolean) {
        isOnCamera = onCamera
    }
}