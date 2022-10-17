package com.ola.chat.picker.album

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.ola.chat.picker.album.model.TitleMediaType
import com.ola.chat.picker.album.preview.GalleryPreviewView
import com.ola.chat.picker.customview.setGlobalDebounceOnClickListener
import com.ola.chat.picker.viewmodel.GalleryDataViewModel
import com.ola.chat.picker.viewmodel.GalleryPickerViewModel
import kotlinx.android.synthetic.main.fragment_album.*

/**
 * 相册页面
 */
class AlbumFragment : Fragment() {

    private lateinit var galleryPagerAdapter: GalleryPagerAdapter
    private lateinit var pickerAllFragment: PickerAllFragment
    private lateinit var pickerVideoFragment: PickerVideoFragment
    private lateinit var pickerImageFragment: PickerImageFragment

    private lateinit var galleryPickerViewModel: GalleryPickerViewModel
    private lateinit var galleryDataViewModel: GalleryDataViewModel
    private val mediaTypeArray = TitleMediaType.values()
    private var callback: PickerCallback? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_album, container, false)
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

    private fun initFragment() {
        pickerAllFragment =
            childFragmentManager.findFragmentByTag(TitleMediaType.TYPE_ALL.name) as? PickerAllFragment
                ?: PickerAllFragment(
                    requireContext(),
                    requireActivity(),
                    galleryPickerViewModel,
                    galleryDataViewModel
                )

        pickerVideoFragment =
            childFragmentManager.findFragmentByTag(TitleMediaType.TYPE_VIDEO.name) as? PickerVideoFragment
                ?: PickerVideoFragment(
                    requireContext(),
                    requireActivity(),
                    galleryPickerViewModel,
                    galleryDataViewModel
                )

        pickerImageFragment =
            childFragmentManager.findFragmentByTag(TitleMediaType.TYPE_IMAGE.name) as? PickerImageFragment
                ?: PickerImageFragment(
                    requireContext(),
                    requireActivity(),
                    galleryPickerViewModel,
                    galleryDataViewModel
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
    }

    private fun initView() {
        //list
        galleryPagerAdapter = GalleryPagerAdapter(requireActivity(), childFragmentManager)
        materialViewPager.adapter = galleryPagerAdapter
        materialViewPager.offscreenPageLimit = 3
        pagerTab.setupWithViewPager(materialViewPager)

        backIv.setGlobalDebounceOnClickListener {
            activity?.onBackPressed()
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    private fun currentFragment(position: Int): BasePickerFragment {
        return when (mediaTypeArray[position]) {
            TitleMediaType.TYPE_ALL -> pickerAllFragment
            TitleMediaType.TYPE_IMAGE -> pickerImageFragment
            TitleMediaType.TYPE_VIDEO -> pickerVideoFragment
        }
    }

    private inner class GalleryPagerAdapter(val context: Context, fm: androidx.fragment.app.FragmentManager) :
        FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return currentFragment(position)
        }

        override fun getCount(): Int {
            return mediaTypeArray.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return context.getString(mediaTypeArray[position].stringId)
        }
    }
}