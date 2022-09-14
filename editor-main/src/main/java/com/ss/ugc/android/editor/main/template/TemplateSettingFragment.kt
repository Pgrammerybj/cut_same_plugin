package com.ss.ugc.android.editor.main.template

import android.os.Bundle
import android.os.Debug
import android.view.View
import com.bytedance.ies.nlemedia.SeekMode
import com.ss.ugc.android.editor.base.fragment.BaseFragment
import com.ss.ugc.android.editor.core.Constants
import com.ss.ugc.android.editor.core.NLEEditorContext
import com.ss.ugc.android.editor.core.utils.DLog
import com.ss.ugc.android.editor.core.utils.LiveDataBus
import com.ss.ugc.android.editor.core.vm.EditViewModelFactory
import com.ss.ugc.android.editor.main.EditorActivity
import com.ss.ugc.android.editor.main.OnFloatSliderChangeListener
import com.ss.ugc.android.editor.main.R
import com.ss.ugc.android.editor.preview.subvideo.VideoGestureLayout
import com.ugc.andorid.editor.template.ITemplateStateListener
import kotlinx.android.synthetic.main.fragment_template_setting.*

/**
 * author:zhujunwen
 */
class TemplateSettingFragment:BaseFragment() ,View.OnClickListener{
    private var enterTemplateSettingFragmentShowViewList: ArrayList<View>? = null
    private var quitTemplateSettingFragmentShowViewList: ArrayList<View>? = null
    companion object {
        const val TAG = "TemplateSettingFragment"
    }
    override fun getContentView(): Int {
        return R.layout.fragment_template_setting // fragment 的 layout
    }

    val templateSettingViewModel by lazy {
        EditViewModelFactory.viewModelProvider(this).get(TemplateSettingViewModel::class.java)
    }

    val nleEditorContext by lazy {
        templateSettingViewModel.nleEditorContext
    }
    lateinit var gestureLayout: VideoGestureLayout

    private val stateListener: ITemplateStateListener = object : ITemplateStateListener {
        override fun onPlayTimeChanged(curPlayTime: String, totalPlayTime: String) {
            cur_time.setText(String.format("%s", curPlayTime))
            total_time.setText(String.format("%s", totalPlayTime))
            val position = 100 * nleEditorContext.videoPlayer.curPosition()
                .toFloat() / nleEditorContext.videoPlayer.totalDuration().toFloat()
            template_slider?.currPosition = position
        }

        override fun onPlayStateChanged(isChangeToPlay: Boolean) {
            if (isChangeToPlay){
                onPlayActivate()
            } else {
                onPauseActivate()
            }
        }

        override fun onPlayActivate() {
//            nleEditorContext.videoPlayer.play()
            template_iv_play.setImageResource(R.drawable.ic_pause)
        }

        override fun onPauseActivate() {
            //nleEditorContext.videoPlayer.pause()
            template_iv_play.setImageResource(R.drawable.template_play_icon)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        DLog.e("$TAG -- onCreate")
        super.onCreate(savedInstanceState)
        templateSettingViewModel.stateListener = stateListener
        templateSettingViewModel.checkTemplateID() // 每次打开 模版设置页面 都会检查模版的 id
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        DLog.e("$TAG -- onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        init()
    }

    override fun onDestroyView() {
        DLog.e("$TAG -- onDestroyView")
        super.onDestroyView()

    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.template_iv_play -> {
                if (nleEditorContext.videoPlayer.isPlaying) { // 在 播放 状态点击 icon，表示要暂停
                    nleEditorContext.videoPlayer.pause()
                } else {
                    nleEditorContext.videoPlayer.play() // 在暂停状态点击 icon， 表示要播放
                }
            }
        }
    }


    fun init() {
        banGesture()
        updateMutableItemData()
        template_iv_play.setOnClickListener(this)
        registerEvent()
        initViewPagerAndTabLayout()

    }

    fun updateMutableItemData() = templateSettingViewModel.updateData()


    private fun banGesture() {
        gestureLayout = activity!!.findViewById(R.id.gesture_layout_preview)
        gestureLayout.isInTemplateSettingView = true
    }

    /**
     *  初始化 viewpager 和 tablayout
     */
    private fun initViewPagerAndTabLayout(){
        templateSettingViewModel.viewpagerFragmentAdapter = TemplateViewPagerAdapter(
            childFragmentManager, // this.activity!!.supportFragmentManager
            templateSettingViewModel.fragmentlists!!,
            templateSettingViewModel.tabTitleList!!
        )
        slot_frame_viewpager.adapter = templateSettingViewModel.viewpagerFragmentAdapter //viewpager与fragment内容绑定
        slot_frame_viewpager.currentItem = 0
        tracks_tablayout.setupWithViewPager(slot_frame_viewpager) //绑定tab和viewpager
    }

    /**
     *  初始化一些事件，例如点击事件、livedata动作等
     */
    private fun registerEvent(){

        /**
         * 配置进度条
         */
        template_slider.setOnSliderChangeListener(object : OnFloatSliderChangeListener() {
            override fun onBegin(value: Float) {
                super.onBegin(value)
                if (nleEditorContext.videoPlayer.isPlaying) { //若进入全屏时，正在播放
                    templateSettingViewModel.isVideoPlayingWhenChangeSeekBar = true //防抖，防止进度条抖动，先停止视频
                    nleEditorContext.videoPlayer.pause()
                }
            }

            override fun onChange(value: Float) {
                nleEditorContext.videoPlayer.seekToPosition(
                    (value * nleEditorContext.videoPlayer.totalDuration() / 100).toInt(),
                    SeekMode.EDITOR_SEEK_FLAG_LastSeek,
                    true
                )
                templateSettingViewModel.mHandler.removeCallbacks(templateSettingViewModel.timeChangedRunnable)
                templateSettingViewModel.mHandler.post(templateSettingViewModel.timeChangedRunnable)
            }

            override fun onFreeze(value: Float) {
                if (templateSettingViewModel.isVideoPlayingWhenChangeSeekBar) {
                    templateSettingViewModel.isVideoPlayingWhenChangeSeekBar = false
                    Thread.sleep(350)  //防抖，防止进度条抖动，延迟后重新播放
                    nleEditorContext.videoPlayer.play() //防抖，防止进度条抖动，延迟后重新播放
                }
            }
        })

        /**
         * 处理livedata观察事件
         */
        LiveDataBus.getInstance().with(Constants.KEY_MAIN, Int::class.java)
            .observe(this, { position ->
                when (position) {
                    NLEEditorContext.STATE_PLAY -> {
                        templateSettingViewModel.mHandler.removeCallbacks(templateSettingViewModel.timeChangedRunnable)
                        templateSettingViewModel.mHandler.post(templateSettingViewModel.timeChangedRunnable)
                        stateListener.onPlayStateChanged(true)
                    }
                    NLEEditorContext.STATE_PAUSE -> {
                        templateSettingViewModel.mHandler.removeCallbacks(templateSettingViewModel.timeChangedRunnable)
                        stateListener.onPlayStateChanged(false)
                    }
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        gestureLayout.isInTemplateSettingView = false
        templateSettingViewModel.mHandler.removeCallbacks(templateSettingViewModel.timeChangedRunnable)
        templateSettingViewModel.mainTrackSlotFrameMessageList.clear()
        templateSettingViewModel.PIPTrackSlotFrameMessageList.clear()
        templateSettingViewModel.textTrackSlotMessageList.clear()
        if (templateSettingViewModel.frameMap.size >= TemplateSettingViewModel.MAX_FRAME_CACHE) {
            templateSettingViewModel.frameMap.clear()
        }
        (this.activity as EditorActivity).templateSettingFragment = null
        uiSwtich(false)
    }

    fun setSwitchFragmentActivityView(
        enterTemplateSettingFragmentShowViewList: ArrayList<View>,
        quitTemplateSettingFragmentShowViewList: ArrayList<View>
    ) {
        this.enterTemplateSettingFragmentShowViewList = enterTemplateSettingFragmentShowViewList
        this.quitTemplateSettingFragmentShowViewList = quitTemplateSettingFragmentShowViewList
    }

    fun uiSwtich(enter: Boolean) {
        var visibleViewList = enterTemplateSettingFragmentShowViewList
        var goneViewList = quitTemplateSettingFragmentShowViewList
        if (!enter) {
            visibleViewList = quitTemplateSettingFragmentShowViewList
            goneViewList = enterTemplateSettingFragmentShowViewList
        }
        visibleViewList?.also { viewList ->
            viewList.forEach {
                it.visibility = View.VISIBLE
            }
        }
        goneViewList?.also { viewList ->
            viewList.forEach {
                it.visibility = View.GONE
            }
        }
    }
}