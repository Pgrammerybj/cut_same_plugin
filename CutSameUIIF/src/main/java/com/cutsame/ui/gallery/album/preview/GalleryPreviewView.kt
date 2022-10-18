package com.cutsame.ui.gallery.album.preview

import android.annotation.SuppressLint
import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.cutsame.ui.R
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.gallery.album.adapter.DeleteClickListener
import com.cutsame.ui.gallery.album.adapter.GalleryPreviewAdapter
import com.cutsame.ui.gallery.album.adapter.ItemClickListener
import com.cutsame.ui.gallery.album.adapter.PickingListAdapter
import com.cutsame.ui.gallery.album.model.MediaData
import com.cutsame.ui.gallery.album.model.TitleMediaType
import com.cutsame.ui.gallery.viewmodel.GalleryPickerViewModel
import com.cutsame.ui.utils.SizeUtil
import com.cutsame.ui.utils.SpaceItemDecoration
import com.cutsame.ui.utils.showErrorTipToast
import com.ss.android.ugc.cut_ui.MediaItem
import kotlinx.android.synthetic.main.activity_default_picker.*
import kotlinx.android.synthetic.main.view_gallery_preview.view.*
import java.util.*

@SuppressLint("ViewConstructor")
class GalleryPreviewView(
    context: Context,
    private val galleryPickerViewModel: GalleryPickerViewModel,
    private val lifeCycleOwner: LifecycleOwner
) :
    FrameLayout(context, null) {
    private lateinit var rootPreViewView: View
    private lateinit var previewViewPager: androidx.viewpager.widget.ViewPager
    private lateinit var pickingRecycleView: androidx.recyclerview.widget.RecyclerView
    private lateinit var nextTv: TextView
    private lateinit var selectImageView: TextView
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
        pickingRecycleView = rootPreViewView.findViewById(R.id.pickingRecycleView)
        selectImageView = rootPreViewView.findViewById(R.id.selectImageView)
        nextTv = rootPreViewView.findViewById(R.id.nextTv)

        pickingListAdapter = PickingListAdapter(galleryPickerViewModel, lifeCycleOwner)

        pickingRecycleView.layoutManager =
            object : androidx.recyclerview.widget.LinearLayoutManager(context, HORIZONTAL, false) {
                override fun smoothScrollToPosition(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    state: androidx.recyclerview.widget.RecyclerView.State?,
                    position: Int
                ) {
                    val linearSmoothScroller =
                        object : androidx.recyclerview.widget.LinearSmoothScroller(recyclerView.context) {
                            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                                return super.calculateSpeedPerPixel(displayMetrics) * 6
                            }

                            override fun calculateDxToMakeVisible(
                                view: View,
                                snapPreference: Int
                            ): Int {
                                val layoutManager = this.layoutManager
                                return if (layoutManager != null && layoutManager.canScrollHorizontally()) {
                                    val params = view.layoutParams as androidx.recyclerview.widget.RecyclerView.LayoutParams
                                    val left =
                                        layoutManager.getDecoratedLeft(view) - params.leftMargin
                                    val right =
                                        layoutManager.getDecoratedRight(view) + params.rightMargin
                                    val start = layoutManager.paddingLeft
                                    val end = layoutManager.width - layoutManager.paddingRight
                                    return start + (end - start) / 2 - (right - left) / 2 - left
                                } else {
                                    0
                                }
                            }
                        }
                    linearSmoothScroller.targetPosition = position
                    startSmoothScroll(linearSmoothScroller)
                }
            }

        pickingRecycleView.setHasFixedSize(true)
        pickingRecycleView.addItemDecoration(SpaceItemDecoration(0, 0, 0, SizeUtil.dp2px(8F)))
        pickingRecycleView.adapter = pickingListAdapter
        initComponent()
        initListener(rootPreViewView)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initComponent() {
        galleryPickerViewModel.pickFull.observe(lifeCycleOwner, {
            nextTv.isSelected = it == true
            if (it == true) {
                nextTv.background = resources.getDrawable(R.drawable.bg_ok_btn, null)
                selectLayout.visibility = View.GONE
            } else {
                nextTv.background = resources.getDrawable(R.drawable.bg_ok_noselect, null)
                selectLayout.visibility = View.VISIBLE
            }
        })

        galleryPickerViewModel.currentPickIndex.observe(lifeCycleOwner,
            { index ->
                index?.apply {
                    pickingRecycleView.smoothScrollToPosition(index)
                }
            })
    }

    private fun initListener(rootPreViewView: View) {
        rootPreViewView.findViewById<ImageView>(R.id.backIv).setGlobalDebounceOnClickListener {
            controlListener?.onBackClick()
        }

        rootPreViewView.findViewById<LinearLayout>(R.id.selectLayout)
            .setGlobalDebounceOnClickListener {
                if (galleryPickerViewModel.pickFull.value == true) {
                    val size = galleryPickerViewModel.processPickItem.value?.size
                    val tipMsg =
                        context.resources.getString(R.string.cutsame_pick_tip_most_count, size)
                    showTipToast(tipMsg)
                    return@setGlobalDebounceOnClickListener
                }

                if (!galleryPickerViewModel.pickOne(galleryPreviewAdapter.getCurrentItem())) {
                    //素材时长小于要去时长
                    val index = galleryPickerViewModel.currentPickIndex.value ?: 0
                    galleryPickerViewModel.processPickItem.value?.get(index)?.let {
                        //视频不能小于xs
                        val tipMsg = context.resources.getString(
                            R.string.cutsame_pick_tip_duration_invalid,
                            String.format(
                                Locale.getDefault(),
                                context.resources.getString(R.string.cutsame_common_media_duration_s),
                                it.duration.toFloat() / 1000
                            )
                        )
                        showTipToast(tipMsg)
                    }
                }
            }

        nextTv.setGlobalDebounceOnClickListener {
            if (galleryPickerViewModel.pickFull.value == true) {
                controlListener?.onSureClick()
            }
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
                showTipToast(context.resources.getString(R.string.cutsame_pick_tip_support_preview_video))
                return
            }
        } else {
            if (mediaType == TitleMediaType.TYPE_IMAGE.name) {
                //此类型不允许浏览视频
                showTipToast(context.resources.getString(R.string.cutsame_pick_tip_support_preview_image))
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