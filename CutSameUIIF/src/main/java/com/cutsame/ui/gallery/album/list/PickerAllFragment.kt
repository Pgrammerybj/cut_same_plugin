package com.cutsame.ui.gallery.album.list

import android.annotation.SuppressLint
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import android.content.Context
import com.cutsame.ui.gallery.album.model.MediaData
import com.cutsame.ui.gallery.album.model.TitleMediaType
import com.cutsame.ui.gallery.viewmodel.GalleryPickerViewModel
import com.cutsame.ui.gallery.viewmodel.GalleryDataViewModel

@SuppressLint("ValidFragment")
class PickerAllFragment(context: Context,
                        private val lifecycleOwner: LifecycleOwner,
                        galleryPickerViewModel: GalleryPickerViewModel,
                        private val galleryDataViewModel: GalleryDataViewModel
) : BasePickerFragment(context, lifecycleOwner, galleryPickerViewModel) {

    override fun getData(block: (List<MediaData>) -> Unit) {
        galleryDataViewModel.getGalleryMaterialData(TitleMediaType.TYPE_ALL, mContext)
            .observe(lifecycleOwner,
                Observer<List<MediaData>> {
                    it?.let { dataList ->
                        block.invoke(dataList)
                    }
                })
    }
    
}