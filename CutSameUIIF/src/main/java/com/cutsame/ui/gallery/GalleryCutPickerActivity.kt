package com.cutsame.ui.gallery

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
import com.cutsame.solution.source.SourceInfo
import com.cutsame.solution.template.model.TemplateItem
import com.cutsame.ui.CutSameUiIF
import com.cutsame.ui.R
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.customwidget.LoadingDialog
import com.cutsame.ui.gallery.album.adapter.DeleteClickListener
import com.cutsame.ui.gallery.album.adapter.ItemClickListener
import com.cutsame.ui.gallery.album.adapter.PickingListAdapter
import com.cutsame.ui.gallery.album.model.MediaData
import com.cutsame.ui.gallery.album.model.TitleMediaType
import com.cutsame.ui.gallery.album.preview.GalleryPreviewView
import com.cutsame.ui.gallery.camera.VideoPreviewActivity
import com.cutsame.ui.gallery.data.TabFragmentPagerAdapter
import com.cutsame.ui.gallery.data.TabType
import com.cutsame.ui.gallery.viewmodel.GalleryDataViewModel
import com.cutsame.ui.gallery.viewmodel.GalleryPickerViewModel
import com.cutsame.ui.utils.SizeUtil
import com.cutsame.ui.utils.SpaceItemDecoration
import com.cutsame.ui.utils.showErrorTipToast
import com.ss.android.ugc.cut_log.LogUtil
import com.ss.android.ugc.cut_ui.MediaItem
import kotlinx.android.synthetic.main.activity_default_picker.*
import kotlinx.coroutines.*
import java.util.*

private const val TAG = "cutui.DefaultPicker"
private const val REQUEST_COMPRESS = 1000
const val REQUEST_CODE_CLIP = 1001 // 素材替换 exp: 裁剪/录制
const val REQUEST_CODE_TEMPLATE_PREVIEW = 1002 // 模板效果预览

class GalleryCutPickerActivity : PermissionActivity(), PickerCallback {
    private var preClipMediaItemMap: HashMap<Int, MediaItem> = HashMap()
    private lateinit var pickingListAdapter: PickingListAdapter
    private lateinit var galleryPickerViewModel: GalleryPickerViewModel
    private lateinit var galleryDataViewModel: GalleryDataViewModel
    private lateinit var previewRootLayout: FrameLayout
    private var galleryPreviewView: GalleryPreviewView? = null
    private var showPreview = false
    private lateinit var templateItem: TemplateItem
    private var loadingDialog: LoadingDialog? = null
    private val previewViewControlListener = object : GalleryPreviewView.ControlListener {
        override fun onBackClick() {
            hidePreview()
        }

        override fun onSureClick() {
            startPublishEditPage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        val templateItem = intent.getParcelableExtra<TemplateItem>(CutSameUiIF.ARG_TEMPLATE_ITEM)
            ?.also { templateItem = it }
        val data = CutSameUiIF.getGalleryPickDataByIntent(intent)
        if (data != null) {
            checkPermission(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        } else {
            LogUtil.e(TAG, "no data. finish now")
            finish()
        }
    }

    override fun onPermissionGranted() {
        LogUtil.d(TAG, "onPermissionGranted")
        setContentView(R.layout.activity_default_picker)
        val mediaItems = CutSameUiIF.getGalleryPickDataByIntent(intent)
        val videoCachePath = CutSameUiIF.getTemplateVideoCacheByIntent(intent)
        LogUtil.d(TAG, "onPermissionGranted mediaItems=${mediaItems?.size}")
        val prePickItems =
            intent.getParcelableArrayListExtra<MediaItem>(CutSameUiIF.ARG_DATA_PRE_PICK_RESULT_MEDIA_ITEMS)
        LogUtil.d(TAG, "onPermissionGranted prePickItems=${prePickItems?.size}")
        galleryPickerViewModel = ViewModelProvider(
            this@GalleryCutPickerActivity,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return GalleryPickerViewModel(application) as T
                }
            }
        ).get(GalleryPickerViewModel::class.java)
        galleryPickerViewModel.init(mediaItems!!, prePickItems, videoCachePath!!)

        galleryDataViewModel = ViewModelProvider(
            this@GalleryCutPickerActivity,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(GalleryDataViewModel::class.java)

        initView()
        initComponent()
        initListener()
    }


    override fun onPermissionDenied() {
        super.onPermissionDenied()
        LogUtil.e(TAG, "onPermissionDenied, finish")
        Toast.makeText(
            this,
            R.string.cutsame_common_open_permission, Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun initView() {
        previewRootLayout = findViewById(R.id.previewRootLayout)
        confirmTV.text = String.format(
            resources.getString(R.string.ck_confirm_picker), 0,
            galleryPickerViewModel.processPickItem.value?.size ?: 0
        )
        //picking
        pickingListAdapter = PickingListAdapter(galleryPickerViewModel, this)
        pickingRecyclerView.layoutManager = object : LinearLayoutManager(this, HORIZONTAL, false) {
            override fun smoothScrollToPosition(
                recyclerView: RecyclerView,
                state: RecyclerView.State?,
                position: Int
            ) {
                val linearSmoothScroller = object : LinearSmoothScroller(recyclerView.context) {
                    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                        return super.calculateSpeedPerPixel(displayMetrics) * 6
                    }

                    // scroll to center
                    override fun calculateDxToMakeVisible(view: View, snapPreference: Int): Int {
                        val layoutManager = this.layoutManager
                        return if (layoutManager != null && layoutManager.canScrollHorizontally()) {
                            val params = view.layoutParams as RecyclerView.LayoutParams
                            val left = layoutManager.getDecoratedLeft(view) - params.leftMargin
                            val right = layoutManager.getDecoratedRight(view) + params.rightMargin
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
        pickingRecyclerView.setHasFixedSize(true)
        pickingRecyclerView.addItemDecoration(SpaceItemDecoration(0, 0, 0, SizeUtil.dp2px(8F)))
        pickingRecyclerView.adapter = pickingListAdapter

        val tabList = listOf(TabType.Album)
        viewPager.adapter = TabFragmentPagerAdapter(tabList, this, galleryPickerViewModel)
        viewPager.isUserInputEnabled = false
        viewPager.offscreenPageLimit = OFFSCREEN_PAGE_LIMIT_DEFAULT
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initComponent() {
        galleryPickerViewModel.loadingEvent.observe(this) { show ->
            if (show == true) {
                showLoading(resources.getString(R.string.pick_resource_loadding))
            } else {
                dismissLoading()
            }
        }
        galleryPickerViewModel.currentPickIndex.observe(this,
            { index ->
                index?.apply {
                    pickingRecyclerView.smoothScrollToPosition(index)
                }
            })

        galleryPickerViewModel.pickFull.observe(this, {
            val isFull = it == true
            confirmTV.isSelected = isFull
            val maxPickSize = galleryPickerViewModel.processPickItem.value?.size ?: 0
            if (isFull) {
                confirmTV.background = resources.getDrawable(R.drawable.bg_ok_btn, null)
                confirmTV.setTextColor(Color.WHITE)
                confirmTV.text = String.format(
                    resources.getString(R.string.ck_confirm_picker), maxPickSize,
                    maxPickSize
                )
            } else {
                confirmTV.background = resources.getDrawable(R.drawable.bg_ok_noselect, null)
                confirmTV.setTextColor(Color.parseColor("#858585"))
                confirmTV.text = String.format(
                    resources.getString(R.string.ck_confirm_picker),
                    galleryPickerViewModel.processPickMediaData.size,
                    maxPickSize
                )
            }
        })
    }


    private fun initListener() {
        pickingListAdapter.setItemClickListener(object : ItemClickListener {
            override fun onItemClick(position: Int, empty: Boolean) {
                if (empty) {
                    galleryPickerViewModel.setSelected(position)
                    return
                }
                val mediaItem = galleryPickerViewModel.processPickItem.value?.get(position)
                if (mediaItem?.type == MediaItem.TYPE_VIDEO || mediaItem?.alignMode == MediaItem.ALIGN_MODE_VIDEO) {
                    //进入视频裁剪页面
                    val createClipUIIntent =
                        CutSameUiIF.createClipUIIntent(this@GalleryCutPickerActivity, mediaItem)
                    if (createClipUIIntent != null) {
                        val preClipMediaItem = preClipMediaItemMap[position]
                        preClipMediaItem?.let {
                            //如果要当前要裁剪的素材和之前的素材不一样，则将素材的开始时间至为0
                            if (it.source != mediaItem.source) {
                                mediaItem.sourceStartTime = 0
                            }
                        }
                        preClipMediaItemMap[position] = mediaItem
                        createClipUIIntent.putExtra(
                            CutSameUiIF.ARG_DATA_CLIP_MEDIA_ITEM,
                            mediaItem
                        ) // deliver a
                        createClipUIIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivityForResult(createClipUIIntent, REQUEST_CODE_CLIP)
                    }
                } else {
                    //进入素材预览页面
                    val processPickMediaData = galleryPickerViewModel.processPickMediaData
                    var realIndex = 0
                    galleryPickerViewModel.processPickItem.value?.forEachIndexed { index, mediaItem ->
                        if (index < position && mediaItem.getUri() != Uri.EMPTY) {
                            realIndex++
                        }
                    }
                    showPreview(
                        realIndex,
                        processPickMediaData,
                        TitleMediaType.TYPE_ALL.name,
                        GalleryPreviewView.VIEW_TYPE_PICK
                    )
                }
            }
        })

        pickingListAdapter.setDeleteClickListener(object : DeleteClickListener {
            override fun onDeleteLayoutClick(position: Int) {
                galleryPickerViewModel.deleteOne(position)
            }
        })

        confirmTV.setGlobalDebounceOnClickListener {
            if (galleryPickerViewModel.pickFull.value == true) {
                startPublishEditPage()
            }
        }
    }

    override fun showPreview(
        position: Int,
        datas: List<MediaData>,
        mediaType: String,
        viewType: String
    ) {
        showPreview = true
        galleryPreviewView = GalleryPreviewView(this, galleryPickerViewModel, this)
        galleryPreviewView?.setData(datas, mediaType, viewType)
        galleryPreviewView?.setCurrentItem(position)
        previewRootLayout.addView(galleryPreviewView)
        galleryPreviewView?.setControlListener(previewViewControlListener)
    }

    fun hidePreview() {
        showPreview = false
        galleryPreviewView?.visibility = View.GONE
        previewRootLayout.removeAllViews()
        galleryPreviewView = null
    }

    private fun startPublishEditPage() {
        val model = galleryPickerViewModel
        val items = model.processPickItem.value!!
        if (TextUtils.isEmpty(templateItem.templateUrl)) {
            Toast.makeText(this, R.string.cutsame_compose_need_template_url, Toast.LENGTH_SHORT)
                .show()
            return
        }

        val compressUIIntent =
            CutSameUiIF.createCompressUIIntent(this, ArrayList(items), templateItem.templateUrl)
        if (compressUIIntent == null) {
            LogUtil.d(TAG, "compressUIIntent==null, finish")
            val intent = Intent().apply {
                CutSameUiIF.setGalleryPickResultData(this, ArrayList(items))
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        } else {
            startActivityForResult(
                compressUIIntent,
                REQUEST_COMPRESS
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            //素材合成
            REQUEST_COMPRESS -> {
                LogUtil.d(TAG, "REQUEST_COMPRESS resultCode=$resultCode data=$data")
                if (resultCode == RESULT_OK && data != null) {
                    val compressData = CutSameUiIF.getCompressResultData(data)
                    if (compressData != null) {
                        val intent = Intent().apply {
                            CutSameUiIF.setGalleryPickResultData(this, ArrayList(compressData))
                        }
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Media Compress Error", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    showTipToast(getString(R.string.cutsame_edit_export_failed))
                }
            }
            //素材编辑
            REQUEST_CODE_CLIP -> {
                LogUtil.d(TAG, "REQUEST_CODE_CLIP resultCode=$resultCode")
                val processItem: MediaItem?
                if (resultCode == Activity.RESULT_OK) {
                    processItem =
                        data?.getParcelableExtra(CutSameUiIF.ARG_DATA_CLIP_MEDIA_ITEM)
                    LogUtil.d(TAG, "REQUEST_CODE_CLIP processItem=$processItem")
                    if (processItem != null) {
                        galleryPickerViewModel.updateProcessPickItem(processItem)
                    } else {
                        LogUtil.d(TAG, "REQUEST_CODE_CLIP")
                    }
                } else {
                    LogUtil.d(TAG, "REQUEST_CODE_CLIP resultCode!=RESULT_OK")
                }
            }
            //模板槽位效果预览
            REQUEST_CODE_TEMPLATE_PREVIEW -> {
                VideoPreviewActivity.obtainResult(data)?.let { media ->
                    val success =
                        galleryPickerViewModel.pickOne(media.path, media.isVideo, media.duration)
                    if (media.isVideo && !success) {
                        //素材时长小于要去时长
                        val index = galleryPickerViewModel.currentPickIndex.value ?: 0
                        galleryPickerViewModel.processPickItem.value?.get(index)?.let {
                            galleryPickerViewModel.processPickItem.value?.get(index)?.let {
                                //视频不能小于xs
                                val tipMsg = resources.getString(
                                    R.string.cutsame_pick_tip_duration_invalid,
                                    String.format(
                                        Locale.getDefault(),
                                        resources.getString(R.string.cutsame_common_media_duration_s),
                                        it.duration.toFloat() / 1000
                                    )
                                )
                                showTipToast(tipMsg)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showTipToast(tipMsg: String) {
        showErrorTipToast(this, tipMsg)
    }

    override fun onBackPressed() {
        if (showPreview) {
            hidePreview()
            return
        }
        super.onBackPressed()
    }

    private fun showLoading(msg: CharSequence? = null) {
        loadingDialog = LoadingDialog(this).apply {
            msg?.let { setMessage(it) }
            show()
        }
    }

    private fun dismissLoading() {
        loadingDialog?.dismiss()
    }
}

interface PickerCallback {
    fun showPreview(position: Int, datas: List<MediaData>, mediaType: String, viewType: String)
}