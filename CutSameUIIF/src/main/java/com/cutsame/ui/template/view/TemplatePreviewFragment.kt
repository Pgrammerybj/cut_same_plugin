package com.cutsame.ui.template.view

import android.content.Context
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.cutsame.solution.template.model.TemplateItem
import com.cutsame.ui.CutSameUiIF
import com.cutsame.ui.R
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.template.play.IVideoPlayer
import com.cutsame.ui.template.play.PlayCacheServer
import com.cutsame.ui.template.play.VideoPlayer
import com.cutsame.ui.template.play.VideoPlayerListener
import com.cutsame.ui.template.viewmodel.TemplatePreviewModel
import com.danikula.videocache.HttpProxyCacheServer
import com.ss.android.ugc.cut_log.LogUtil
import kotlinx.android.synthetic.main.fragment_template_preview.*

class TemplatePreviewFragment : Fragment() {
    private val TAG = "TemplatePreviewFragment"
    private val ARG_KEY_TEMPLATE_ITEM = "ARG_KEY_TEMPLATE_ITEM"
    private var templateItem: TemplateItem = TemplateItem()
    private var videoPlayer: IVideoPlayer = VideoPlayer()
    private var templatePreviewModel: TemplatePreviewModel? = null
    private var httpProxyCacheServer: HttpProxyCacheServer? = null
    private var videoSurface: Surface? = null
    private var renderStart: Boolean = false
    private val uiHandler = Handler(Looper.getMainLooper())
    private val playRunnable = Runnable {
        LogUtil.d(TAG, "playRunnable")
        videoPlayer.run {
            if (!isPlaying()) {
                if (!videoPlayer.isPrepared) {
                    loadingView.show()
                }
                play()
            }
        }
    }

    companion object {
        fun newInstance(templateItem: TemplateItem? = null) =
            TemplatePreviewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_KEY_TEMPLATE_ITEM, templateItem)
                }
            }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentActivity) {
            httpProxyCacheServer = PlayCacheServer.getProxy(context)
            templatePreviewModel =
                ViewModelProviders.of(context).get(TemplatePreviewModel::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            templateItem = (it.getParcelable(ARG_KEY_TEMPLATE_ITEM) as? TemplateItem)
                ?: TemplateItem()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_template_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initComponent()
    }

    override fun onStart() {
        super.onStart()
        initListener()
    }

    override fun onResume() {
        super.onResume()
        LogUtil.d(TAG, "onResume")
        if (templateItem == templatePreviewModel?.selectedPage?.value) {
            playVideoActual()
        } else {
            pauseVideoActual()
        }
    }

    override fun onPause() {
        super.onPause()
        LogUtil.d(TAG, "onPause")
        pauseVideoActual()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releaseVideo()
    }

    private fun initView() {
        if (context == null) {
            LogUtil.d(TAG, "initView failed")
            return
        }
        loadingView.setMessage(resources.getString(R.string.cutsame_common_loading))
        VideoTitleTv.text = getTemplateDesc(templateItem)
        segmentNumTv.text = String.format(
            resources.getString(R.string.cutsame_feed_fragment_num),
            templateItem.fragmentCount
        )
        showThumbnail()

        val proxyCacheServer = httpProxyCacheServer
        val url = if (proxyCacheServer != null) {
            proxyCacheServer.getProxyUrl(templateItem.videoInfo?.url!!)
        } else {
            templateItem.videoInfo?.url!!
        }

        LogUtil.d(TAG, "proxyUrl = ${url}, realRrl= ${templateItem.videoInfo?.url!!}")
        initPlayer(url)

        previewTexture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                texture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                videoPlayer.also { player ->
                    val surface = Surface(texture)
                    videoSurface = surface
                    player.setSurface(surface)
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) = Unit

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        }
    }

    private fun initComponent() {
        if (context == null) {
            LogUtil.e(TAG, "initComponent failed")
            return
        }

        templatePreviewModel?.apply {
            pageStateIdle.observe(viewLifecycleOwner, Observer<Boolean> { t ->
                t?.let {
                    handlePageStateIdle(t)
                }
            })
            selectedPage.observe(viewLifecycleOwner, Observer<TemplateItem> { t ->
                t?.let {
                    handlePageSelect(t)
                }
            })
        }
    }

    private fun initPlayer(path: String) {
        videoPlayer.run {
            isLooping = true
            setDataSource(path)
            setVideoListener(object : VideoPlayerListener.Default() {
                override fun onPrepared(player: IVideoPlayer) {
                    LogUtil.d(TAG, "onPrepared $isResumed, url=$path")
                    if (templateItem != templatePreviewModel?.selectedPage?.value || !isResumed) {
                        player.pause()
                    }
                    previewTexture?.let {
                        resizeDisplayView(
                            previewTexture,
                            player.videoWidth,
                            player.videoHeight
                        )
                    }
                }

                override fun onRenderStart() {
                    LogUtil.d(TAG, "onRenderStart $isResumed, url=$path")
                    if (!isResumed) {
                        pause()
                    }
                    renderStart = true
                    if (loadingView.isShowing()) {
                        loadingView.dismiss()
                        infoLayout.visibility = View.VISIBLE
                    }
                    videoPauseIv?.visibility = View.GONE
                    videoCoverIv?.visibility = View.GONE
                }

                override fun onError() {
                    super.onError()
                    LogUtil.e(TAG, "play error ${templateItem.videoInfo?.url}")
                }

                override fun onCompletion() {
                    super.onCompletion()
                    if (templateItem == templatePreviewModel?.selectedPage?.value && isResumed) {
                        playVideoActual()
                    }
                }
            })

        }
    }

    private fun initListener() {
        rootView.setGlobalDebounceOnClickListener {
            if (!renderStart) {
                return@setGlobalDebounceOnClickListener
            }
            if (videoPlayer.isPlaying()) {
                pauseVideoActual()
                showPauseIcon()
            } else {
                playVideoActual()
                hidePauseIcon()
            }
        }

        /**
         * 跳转剪同款页面，在剪同款页面里面会先跳转到图片选择页
         */
        jumpToCutSameTv.setGlobalDebounceOnClickListener {
            val videoCache = httpProxyCacheServer?.getProxyUrl(templateItem.videoInfo?.url!!)
            CutSameUiIF.createCutUIIntent(requireContext(), templateItem, videoCache!!)
                ?.apply {
                    putExtras(requireActivity().intent)
                    this.setPackage(context?.packageName)
                    startActivity(this)
                }
        }
    }

    private fun showPauseIcon() {
        videoPauseIv.apply {
            visibility = View.VISIBLE
            scaleX = 2.5f
            scaleY = 2.5f
            animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            isSelected = false
        }
    }

    private fun hidePauseIcon() {
        videoPauseIv.apply {
            visibility = View.VISIBLE
            animate().alpha(0.0f).setDuration(100).withEndAction {
                visibility = View.GONE
            }.start()
        }
    }

    private fun releaseVideo() {
        videoPlayer.stop()
        videoPlayer.release()
    }

    private fun resizeDisplayView(displayView: View, videoW: Int, videoH: Int) {
        if (videoW <= 0 || videoH <= 0) {
            return
        }

        val displayOriWidth = displayView.width
        val displayOriHeight = displayView.height
        val videoRatio = videoW.toFloat() / videoH
        val displayOriRatio = displayOriWidth.toFloat() / displayOriHeight
        val params = displayView.layoutParams
        if (videoRatio < displayOriRatio) {
            params.height = displayOriHeight
            params.width = (displayOriHeight * videoRatio).toInt()
        } else {
            params.width = displayOriWidth
            params.height = (displayOriWidth / videoRatio).toInt()
        }
        displayView.layoutParams = params
    }

    /**
     * 对模版描述文字进行截取处理
     */
    private fun getTemplateDesc(templateItem: TemplateItem): String {
        var titleDesc = if (TextUtils.isEmpty(templateItem.shortTitle)) {
            templateItem.title
        } else {
            templateItem.shortTitle
        }

//        if (titleDesc.length > 10) {
//            titleDesc = titleDesc.substring(0, 10) + "..."
//        }
        return titleDesc
    }

    private fun showThumbnail() {
        videoCoverIv.visibility = View.VISIBLE
        val requestOptions =
            RequestOptions().override(videoCoverIv.measuredWidth, videoCoverIv.measuredHeight)
        Glide.with(videoCoverIv)
            .load(Uri.parse(templateItem.cover?.url!!))
            .apply(requestOptions)
            .into(videoCoverIv)
    }

    private fun handlePageStateIdle(idle: Boolean) {
        if (idle && templateItem == templatePreviewModel?.selectedPage?.value) {
            playVideoActual()
        } else {
            pauseVideoActual()
        }
    }

    private fun handlePageSelect(mediaItem: TemplateItem) {
        if (mediaItem == this.templateItem) {
            playVideoActual()
        } else {
            pauseVideoActual(true)
        }
    }

    private fun playVideoActual() {
        LogUtil.d(TAG, "playVideoActual")
        uiHandler.postDelayed(playRunnable, 50)
    }

    private fun pauseVideoActual(reset: Boolean = false) {
        LogUtil.d(TAG, "pauseVideoActual")
        uiHandler.removeCallbacks(playRunnable)
        videoPlayer.run {
            if (isPlaying()) pause()
            if (isPrepared && reset) seekTo(0)
        }
    }
}
