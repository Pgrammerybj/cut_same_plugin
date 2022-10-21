package com.ola.chat.picker.album.list

import android.annotation.SuppressLint
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import android.content.Context
import com.ola.chat.picker.album.model.MediaData
import com.ola.chat.picker.album.model.TitleMediaType
import com.ola.chat.picker.viewmodel.GalleryPickerViewModel
import com.ola.chat.picker.viewmodel.GalleryDataViewModel

@SuppressLint("ValidFragment")
class PickerVideoFragment(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    galleryPickerViewModel: GalleryPickerViewModel,
    private val galleryDataViewModel: GalleryDataViewModel,
    isCutSameScene: Boolean
) : BasePickerFragment(context, lifecycleOwner, galleryPickerViewModel, isCutSameScene) {

    override fun getData(block: (List<MediaData>) -> Unit) {
        galleryDataViewModel.getGalleryMaterialData(TitleMediaType.TYPE_VIDEO, mContext)
            .observe(lifecycleOwner,
                Observer<List<MediaData>> {
                    it?.let { dataList ->
                        block.invoke(dataList)
                    }
                })
    }
}