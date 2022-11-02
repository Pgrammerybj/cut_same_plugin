package com.ola.chat.picker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout.LayoutParams.*
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
import com.ola.chat.picker.album.adapter.DeleteClickListener
import com.ola.chat.picker.album.adapter.ItemClickListener
import com.ola.chat.picker.album.adapter.PickingListAdapter
import com.ola.chat.picker.album.adapter.TabFragmentPagerAdapter
import com.ola.chat.picker.album.model.MediaData
import com.ola.chat.picker.album.model.TitleMediaType
import com.ola.chat.picker.album.preview.GalleryPreviewView
import com.ola.chat.picker.customview.LoadingDialog
import com.ola.chat.picker.customview.setGlobalDebounceOnClickListener
import com.ola.chat.picker.entry.ImagePickConfig
import com.ola.chat.picker.entry.MediaItem
import com.ola.chat.picker.entry.TemplateItem
import com.ola.chat.picker.utils.*
import com.ola.chat.picker.viewmodel.GalleryPickerViewModel
import kotlinx.android.synthetic.main.activity_picker_layout.*
import java.util.*

private const val TAG = "Ola.DefaultPicker"
private const val REQUEST_COMPRESS = 1000
const val REQUEST_CODE_CLIP = 1001 // 素材替换 exp: 裁剪/录制
const val REQUEST_CODE_IMAGE_CROP = 1004 // 单选图片的裁剪

class GalleryCutPickerActivity : PermissionActivity(), PickerCallback {
    private var preClipMediaItemMap: HashMap<Int, MediaItem> = HashMap()
    private lateinit var pickingListAdapter: PickingListAdapter
    private lateinit var galleryPickerViewModel: GalleryPickerViewModel
    private lateinit var previewRootLayout: FrameLayout
    private var galleryPreviewView: GalleryPreviewView? = null
    private var showPreview = false
    private lateinit var templateItem: TemplateItem
    private var imagePickConfig: ImagePickConfig? = null
    private var isCutSameScene: Boolean = true
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
        super.onCreate(savedInstanceState)
        MTUtils.makeStatusBarTransparent(this)
        intent.getParcelableExtra<TemplateItem>(PickerConstant.ARG_TEMPLATE_ITEM)?.also { templateItem = it }
        intent.getParcelableExtra<ImagePickConfig>(PickerConstant.ARG_DATA_PICK_CONFIG)?.also { imagePickConfig = it }
        //只有剪同款模式【ImagePickConfig.PICKER_CUT_SAME】才需要检验data
        if (imagePickConfig != null) {
            isCutSameScene = imagePickConfig?.sceneType == ImagePickConfig.PICKER_CUT_SAME
        }

        if (isCutSameScene) {
            val data = PickerConstant.getGalleryPickDataByIntent(intent)
            if (data != null) {
                checkOlaPickerPermission()
            } else {
                finish()
            }
        } else {
            checkOlaPickerPermission()
        }
    }

    private fun checkOlaPickerPermission() {
        checkPermission(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    override fun onPermissionGranted() {
        Log.d(TAG, "onPermissionGranted")
        setContentView(R.layout.activity_picker_layout)
        //剪同款需要的参数
        val mediaItems = PickerConstant.getGalleryPickDataByIntent(intent)
        val prePickItems =
            intent.getParcelableArrayListExtra<MediaItem>(PickerConstant.ARG_DATA_PRE_PICK_RESULT_MEDIA_ITEMS)
        Log.d(TAG, "onPermissionGranted mediaItems=${mediaItems?.size}")
        Log.d(TAG, "onPermissionGranted prePickItems=${prePickItems?.size}")

        galleryPickerViewModel =
            ViewModelProvider(this@GalleryCutPickerActivity, object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return GalleryPickerViewModel(application) as T
                }
            }).get(GalleryPickerViewModel::class.java)

        galleryPickerViewModel.init(mediaItems, prePickItems)

        initView()
        if (isCutSameScene) {
            //初始化剪同款特有的布局视图
            initCutSameLayout()
            initComponent()
            initListener()
        }
    }


    override fun onPermissionDenied() {
        super.onPermissionDenied()
        Log.e(TAG, "onPermissionDenied, finish")
        Toast.makeText(
            this,
            R.string.cutsame_common_open_permission, Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun initView() {
        handleCutSameScene()
        previewRootLayout = findViewById(R.id.previewRootLayout)
        val tabList = listOf(PickerConstant.TabType.Album)
        viewPager.adapter =
            TabFragmentPagerAdapter(
                tabList,
                this,
                galleryPickerViewModel,
                isCutSameScene,
                imagePickConfig
            )
        viewPager.isUserInputEnabled = false
        viewPager.offscreenPageLimit = OFFSCREEN_PAGE_LIMIT_DEFAULT
    }

    private fun initCutSameLayout() {
        confirmTV.text = String.format(
            resources.getString(R.string.confirm_picker), 0,
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
    }

    private fun handleCutSameScene() {
        viewPager.setBackgroundResource(if (isCutSameScene) R.drawable.bg_image_picker else R.color.white)
        pickingListLayout.visibility = if (isCutSameScene) View.VISIBLE else View.GONE
        val layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        pickingListLayout.post {
            viewPager.setPadding(0, 0, 0, SizeUtil.dp2px(16F))
            layoutParams.setMargins(
                0, SizeUtil.dp2px(if (isCutSameScene) 40F else 0F),
                0, if (isCutSameScene) pickingListLayout.height - SizeUtil.dp2px(16F) else 0
            )
            viewPager.layoutParams = layoutParams
        }
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
                    resources.getString(R.string.confirm_picker), maxPickSize,
                    maxPickSize
                )
            } else {
                confirmTV.background = resources.getDrawable(R.drawable.bg_ok_noselect, null)
                confirmTV.setTextColor(Color.parseColor("#858585"))
                confirmTV.text = String.format(
                    resources.getString(R.string.confirm_picker),
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
                        PickerConstant.createClipUIIntent(this@GalleryCutPickerActivity, mediaItem)
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
                            PickerConstant.ARG_DATA_CLIP_MEDIA_ITEM,
                            mediaItem
                        ) // deliver a
                        createClipUIIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivityForResult(createClipUIIntent, REQUEST_CODE_CLIP)
                    }
                } else if (imagePickConfig?.sceneType == ImagePickConfig.PICKER_SINGLE) {
                    //其他业务的单选+裁剪逻辑
                    Log.i(TAG, "onItemClick: 单选图片模式，准备进入子尧裁剪")
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

    /**
     * 大图预览
     */
    override fun showPreview(
        position: Int,
        data: List<MediaData>,
        mediaType: String,
        viewType: String
    ) {
        showPreview = true
        galleryPreviewView = GalleryPreviewView(this, galleryPickerViewModel, this)
        galleryPreviewView?.setData(data, mediaType, viewType)
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
        if (TextUtils.isEmpty(templateItem.template_url)) {
            Toast.makeText(this, R.string.cutsame_compose_need_template_url, Toast.LENGTH_SHORT)
                .show()
            return
        }

        //ArrayList(items)
        val catSameMediaItemList = CutSameMediaUtils.olaMediaItemListToCutSame(ArrayList(items))

        val compressUIIntent = PickerConstant.createCompressUIIntent(this, catSameMediaItemList, templateItem.template_url)
        if (compressUIIntent == null) {
            Log.d(TAG, "compressUIIntent==null, finish")
            val intent = Intent().apply {
                PickerConstant.setGalleryPickResultData(this, catSameMediaItemList)
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
                Log.d(TAG, "REQUEST_COMPRESS resultCode=$resultCode data=$data")
                if (resultCode == RESULT_OK && data != null) {
                    val compressData = PickerConstant.getCompressResultData(data)
                    if (compressData != null) {
                        val intent = Intent().apply {
                            PickerConstant.setGalleryPickResultData(this, ArrayList(compressData))
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
                Log.d(TAG, "REQUEST_CODE_CLIP resultCode=$resultCode")
                val processItem: MediaItem?
                if (resultCode == Activity.RESULT_OK) {
                    processItem =
                        data?.getParcelableExtra(PickerConstant.ARG_DATA_CLIP_MEDIA_ITEM)
                    Log.d(TAG, "REQUEST_CODE_CLIP processItem=$processItem")
                    if (processItem != null) {
                        galleryPickerViewModel.updateProcessPickItem(processItem)
                    } else {
                        Log.d(TAG, "REQUEST_CODE_CLIP")
                    }
                } else {
                    Log.d(TAG, "REQUEST_CODE_CLIP resultCode!=RESULT_OK")
                }
            }
            //单选图片裁剪的结果
            REQUEST_CODE_IMAGE_CROP -> {
                val imagePath = data?.getStringExtra(ImagePickConfig.EXTRA_RESULT_IMAGE_FILE)
                val intent = Intent()
                intent.putExtra(ImagePickConfig.EXTRA_RESULT_IMAGE_FILE, imagePath)
                setResult(ImagePickConfig.REQUEST_CODE_IMAGE_CROP, intent) //单选不需要裁剪，返回数据
                finish()
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
    fun showPreview(position: Int, data: List<MediaData>, mediaType: String, viewType: String)
}