package com.ola.chat.picker.album.preview

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.ola.chat.picker.R
import com.ola.chat.picker.album.adapter.DeleteClickListener
import com.ola.chat.picker.album.adapter.GalleryPreviewAdapter
import com.ola.chat.picker.album.adapter.ItemClickListener
import com.ola.chat.picker.album.adapter.PickingListAdapter
import com.ola.chat.picker.album.model.MediaData
import com.ola.chat.picker.album.model.TitleMediaType
import com.ola.chat.picker.customview.setGlobalDebounceOnClickListener
import com.ola.chat.picker.entry.MediaItem
import com.ola.chat.picker.utils.showErrorTipToast
import com.ola.chat.picker.viewmodel.GalleryPickerViewModel

@SuppressLint("ViewConstructor")
class GalleryPreviewView(
    context: Context,
    private val galleryPickerViewModel: GalleryPickerViewModel,
    private val lifeCycleOwner: LifecycleOwner
) :
    FrameLayout(context, null) {
    private lateinit var rootPreViewView: View
    private lateinit var previewViewPager: androidx.viewpager.widget.ViewPager
//    private lateinit var pickingRecycleView: androidx.recyclerview.widget.RecyclerView
//    private lateinit var nextTv: TextView
//    private lateinit var selectImageView: TextView
    private lateinit var pickingListAdapter: PickingListAdapter
    private var controlListener: ControlListener? = null
    private var viewType: String = VIEW_TYPE_GALLERY
    private var mediaType: String = TitleMediaType.TYPE_ALL.name
    private val previewListener = object : PreviewBaseView.PreviewListener {
        override fun onExit() {
            controlListener?.onBackClick()
        }
    }
    private var galleryPreviewAdapter: GalleryPreviewAdapter = GalleryPreviewAdapter(context, previewListener)

    companion object {
        const val VIEW_TYPE_GALLERY = "gallery"//从相册素材列表跳转进来
        const val VIEW_TYPE_PICK = "pick" //从已经选择的列表跳转进来，
    }

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        rootPreViewView =
            LayoutInflater.from(context).inflate(R.layout.picker_gallery_preview, this, true)
        previewViewPager = rootPreViewView.findViewById(R.id.previewViewPager)
        previewViewPager.adapter = galleryPreviewAdapter
        previewViewPager.isSaveFromParentEnabled = false
//        pickingRecycleView = rootPreViewView.findViewById(R.id.pickingRecycleView)
//        selectImageView = rootPreViewView.findViewById(R.id.selectImageView)
//        nextTv = rootPreViewView.findViewById(R.id.nextTv)

        pickingListAdapter = PickingListAdapter(galleryPickerViewModel, lifeCycleOwner)

        initListener(rootPreViewView)
    }

    private fun initListener(rootPreViewView: View) {
        rootPreViewView.findViewById<ImageView>(R.id.backIv).setGlobalDebounceOnClickListener {
            controlListener?.onBackClick()
        }

        pickingListAdapter.setDeleteClickListener(object : DeleteClickListener {
            override fun onDeleteLayoutClick(position: Int) {
                galleryPickerViewModel.deleteOne(position)
            }
        })

        pickingListAdapter.setItemClickListener(object :
            ItemClickListener {
            override fun onItemClick(position: Int,empty: Boolean) {
                if (empty) {
                    galleryPickerViewModel.setSelected(position)
                    return
                }
                pickingListAdapter.getData()[position].let {
                    if (viewType == VIEW_TYPE_PICK) {
                        //如果是从已经选择的列表跳转进来，不进行处理
                        return
                    }
                    jumpToTargetMediaData(it)
                }
            }
        })

        previewViewPager.addOnPageChangeListener(object : androidx.viewpager.widget.ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                galleryPreviewAdapter.setCurrentPosition(position)
            }
        })
    }

    private fun showTipToast(tipMsg: String) {
        showErrorTipToast(context, tipMsg)
    }

    fun jumpToTargetMediaData(mediaItem: MediaItem) {
        if (mediaItem.type == MediaItem.TYPE_PHOTO) {
            if (mediaType == TitleMediaType.TYPE_VIDEO.name) {
                //此类型不允许浏览图片
                showTipToast(context.resources.getString(R.string.pick_tip_support_preview_video))
                return
            }
        } else {
            if (mediaType == TitleMediaType.TYPE_IMAGE.name) {
                //此类型不允许浏览视频
                showTipToast(context.resources.getString(R.string.pick_tip_support_preview_image))
                return
            }
        }
        val data = galleryPreviewAdapter.getData()
        data.forEachIndexed { index, mediaData ->
            if (mediaData.path == mediaItem.source) {
                setCurrentItem(index)
            }
        }
    }

    fun setData(datas: List<MediaData>, mediaType: String, viewType: String) {
        this.viewType = viewType
        this.mediaType = mediaType
        galleryPreviewAdapter.setData(datas)
    }

    fun setCurrentItem(position: Int) {
        if (position == 0) {
            galleryPreviewAdapter.setCurrentPosition(0)
        }
        previewViewPager.setCurrentItem(position, true)
    }

    fun setControlListener(listener: ControlListener) {
        controlListener = listener
    }

    interface ControlListener {
        fun onBackClick()
        fun onSureClick()
    }

}