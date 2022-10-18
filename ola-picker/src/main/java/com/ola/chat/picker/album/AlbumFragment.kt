package com.ola.chat.picker.album

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ola.chat.picker.PickerCallback
import com.ola.chat.picker.R
import com.ola.chat.picker.album.adapter.GalleryMaterialListAdapter
import com.ola.chat.picker.album.list.BasePickerFragment
import com.ola.chat.picker.album.list.PickerAllFragment
import com.ola.chat.picker.album.list.PickerImageFragment
import com.ola.chat.picker.album.list.PickerVideoFragment
import com.ola.chat.picker.album.model.MediaData
import com.ola.chat.picker.album.model.SingleMediaType
import com.ola.chat.picker.album.model.TitleMediaType
import com.ola.chat.picker.album.preview.GalleryPreviewView
import com.ola.chat.picker.customview.setGlobalDebounceOnClickListener
import com.ola.chat.picker.entry.ImagePickConfig
import com.ola.chat.picker.utils.PickerConstant
import com.ola.chat.picker.viewmodel.GalleryDataViewModel
import com.ola.chat.picker.viewmodel.GalleryPickerViewModel
import kotlinx.android.synthetic.main.fragment_album_layout.*

/**
 * 相册页面
 */
class AlbumFragment(private val isCutSameScene: Boolean, private val imagePickConfig: ImagePickConfig?) : Fragment() {

    private lateinit var galleryPagerAdapter: GalleryPagerAdapter
    private lateinit var pickerAllFragment: PickerAllFragment
    private lateinit var pickerVideoFragment: PickerVideoFragment
    private lateinit var pickerImageFragment: PickerImageFragment

    private lateinit var galleryPickerViewModel: GalleryPickerViewModel
    private lateinit var galleryDataViewModel: GalleryDataViewModel
    private val mediaTypeArray = TitleMediaType.values()
    private val singleMediaType = SingleMediaType.values()
    private var callback: PickerCallback? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_album_layout, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as? PickerCallback
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        initFragment()
        initView()
    }

    private fun initData() {
        galleryPickerViewModel = ViewModelProvider(
            requireActivity(),
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return GalleryPickerViewModel(requireActivity().application) as T
                }
            }
        ).get(GalleryPickerViewModel::class.java)

        galleryDataViewModel = ViewModelProvider(
            requireActivity(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(GalleryDataViewModel::class.java)
    }

    /**
     * 先完成功能后续再考虑架构和涉及
     */
    private fun initFragment() {
        if (isCutSameScene) {
            findAllFragment()
            findVideoFragment()
        } else {
            findImageFragment()
        }
    }

    private fun findImageFragment() {
        pickerImageFragment =
            childFragmentManager.findFragmentByTag(TitleMediaType.TYPE_IMAGE.name) as? PickerImageFragment
                ?: PickerImageFragment(
                    requireContext(),
                    requireActivity(),
                    galleryPickerViewModel,
                    galleryDataViewModel,
                    isCutSameScene
                )


        pickerImageFragment.itemClickListener =
            object : GalleryMaterialListAdapter.ItemClickListener {
                override fun onItemClick(position: Int, datas: List<MediaData>) {
                    callback?.showPreview(
                        position,
                        datas,
                        TitleMediaType.TYPE_IMAGE.name,
                        GalleryPreviewView.VIEW_TYPE_GALLERY
                    )
                }
            }

        pickerImageFragment.itemClickOpenClipListener =
            object : GalleryMaterialListAdapter.ItemClickOpenClipListener {
                override fun openImageClip(mediaData: MediaData) {
                    //展示裁剪页面
                    Toast.makeText(context, "单选图片，准备裁剪", Toast.LENGTH_SHORT).show()
                    val pickerIntent =
                        PickerConstant.createImageClipIntent(context!!,mediaData,imagePickConfig)?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    if (pickerIntent != null) {
                        startActivityForResult(pickerIntent, ImagePickConfig.REQUEST_CODE_IMAGE_CROP)
                    }
                }
            }
    }

    private fun findVideoFragment() {
        pickerVideoFragment =
            childFragmentManager.findFragmentByTag(TitleMediaType.TYPE_VIDEO.name) as? PickerVideoFragment
                ?: PickerVideoFragment(
                    requireContext(),
                    requireActivity(),
                    galleryPickerViewModel,
                    galleryDataViewModel,
                    isCutSameScene
                )


        pickerVideoFragment.itemClickListener =
            object : GalleryMaterialListAdapter.ItemClickListener {
                override fun onItemClick(position: Int, datas: List<MediaData>) {
                    callback?.showPreview(
                        position,
                        datas,
                        TitleMediaType.TYPE_VIDEO.name,
                        GalleryPreviewView.VIEW_TYPE_GALLERY
                    )
                }
            }
    }

    private fun findAllFragment() {
        pickerAllFragment =
            childFragmentManager.findFragmentByTag(TitleMediaType.TYPE_ALL.name) as? PickerAllFragment
                ?: PickerAllFragment(
                    requireContext(),
                    requireActivity(),
                    galleryPickerViewModel,
                    galleryDataViewModel,
                    isCutSameScene
                )

        pickerAllFragment.itemClickListener =
            object : GalleryMaterialListAdapter.ItemClickListener {
                override fun onItemClick(position: Int, datas: List<MediaData>) {
                    callback?.showPreview(
                        position,
                        datas,
                        TitleMediaType.TYPE_ALL.name,
                        GalleryPreviewView.VIEW_TYPE_GALLERY
                    )
                }
            }
    }

    private fun initView() {
        //list
        galleryPagerAdapter = GalleryPagerAdapter(requireActivity(), childFragmentManager)
        materialViewPager.adapter = galleryPagerAdapter
        materialViewPager.offscreenPageLimit = 3
        materialViewPager.currentItem = 0
        pagerTab.setupWithViewPager(materialViewPager)

        backIv.setGlobalDebounceOnClickListener {
            activity?.onBackPressed()
        }

        handleCutSameScene()
    }

    private fun handleCutSameScene() {
        pagerTab.visibility = if (isCutSameScene) View.VISIBLE else View.GONE
        splineView.visibility = if (isCutSameScene) View.VISIBLE else View.GONE
        tvSingleTitle.visibility = if (isCutSameScene) View.GONE else View.VISIBLE
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    private fun currentFragment(position: Int): BasePickerFragment {
        return if (isCutSameScene) {
            when (mediaTypeArray[position]) {
                TitleMediaType.TYPE_ALL -> pickerAllFragment
                TitleMediaType.TYPE_IMAGE -> pickerImageFragment
                TitleMediaType.TYPE_VIDEO -> pickerVideoFragment
            }
        } else {
            pickerImageFragment
        }
    }

    private inner class GalleryPagerAdapter(
        val context: Context,
        fm: androidx.fragment.app.FragmentManager
    ) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return currentFragment(position)
        }

        override fun getCount(): Int {
            return if (isCutSameScene) mediaTypeArray.size else singleMediaType.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return context.getString(if (isCutSameScene) mediaTypeArray[position].stringId else singleMediaType[position].stringId)
        }
    }
}