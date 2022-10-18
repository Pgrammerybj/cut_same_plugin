package com.cutsame.ui.cut.preview

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import com.bytedance.ies.cutsame.cut_android.TemplateError
import com.bytedance.ies.cutsame.util.MediaUtil
import com.cutsame.solution.CutSameSolution
import com.cutsame.solution.player.BasePlayer
import com.cutsame.solution.player.CutSamePlayer
import com.cutsame.solution.player.PlayerStateListener
import com.cutsame.solution.source.CutSameSource
import com.cutsame.solution.source.SourceInfo
import com.cutsame.solution.template.model.TemplateExtraList
import com.cutsame.solution.template.model.TemplateItem
import com.cutsame.ui.CutSameUiIF
import com.cutsame.ui.CutSameUiIF.ARG_TEMPLATE_ITEM
import com.cutsame.ui.R
import com.cutsame.ui.exten.FastMain
import com.cutsame.ui.utils.CutSameMediaUtils
import com.cutsame.ui.utils.ScreenUtil.isScreenOn
import com.cutsame.ui.utils.SizeUtil
import com.google.gson.Gson
import com.ola.chat.picker.entry.Author
import com.ola.chat.picker.entry.Cover
import com.ola.chat.picker.entry.ImagePickConfig
import com.ola.chat.picker.entry.OriginVideoInfo
import com.ola.chat.picker.utils.PickerConstant
import com.ss.android.ugc.cut_log.LogUtil
import com.ss.android.ugc.cut_ui.ItemCrop
import com.ss.android.ugc.cut_ui.MediaItem
import com.ss.android.ugc.cut_ui.TextItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import java.io.File
import kotlin.coroutines.CoroutineContext

const val REQUEST_CODE_PICKER = 1000  //选择素材
const val REQUEST_CODE_REPLACE = 1001 // 素材替换
const val REQUEST_CODE_CLIP = 1002 //素材裁剪
const val REQUEST_CODE_NEXT = 1003 //导出
const val REQUEST_CODE_SINGLE_CHOOSE = 1004 //其他业务单选

private const val TAG = "cut.CutPlayerActivity"

/**
 * 剪同款成品播放页
 */
abstract class CutPlayerActivity : AppCompatActivity(), CoroutineScope {
    override val coroutineContext: CoroutineContext = FastMain + Job()

    var cutSameSource: CutSameSource? = null
    var cutSamePlayer: CutSamePlayer? = null
    protected var templatePlayerErrorCode = TemplateError.SUCCESS

    private var isPlayingOnPause = false

    private lateinit var templateItem: TemplateItem
    private var hasLaunchClip = false
    private var hasLaunchPicker = false
    private var hasLaunchNext = false
    var isForeground = true // 此页面是否在前台
    protected var mutableMediaItemList: ArrayList<MediaItem> = ArrayList()
    private var mutableTextItemList: ArrayList<TextItem>? = null

    // 用于记录原始数据是否更改过 目前仅用于弹窗判断
    private var originMediaItemList: ArrayList<MediaItem>? = null
    private var compileNextIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        supportActionBar?.hide()

        val templateItem =
            intent.getParcelableExtra<TemplateItem>(ARG_TEMPLATE_ITEM)?.also { templateItem = it }
        if (templateItem == null || templateItem.templateUrl.isEmpty() || templateItem.md5.isEmpty()) {
            LogUtil.e(TAG, "onCreate templateItem == null，return")
            finish()
            return
        }
        val templateExtraList = Gson().fromJson(templateItem.extra, TemplateExtraList::class.java)
        LogUtil.d(TAG, "onCreate templateExtraList $templateExtraList")
        for (template in templateExtraList.list) {
            mutableMediaItemList.add(
                MediaItem(
                    duration = template.duration,
                    materialId = template.material_id,
                    isMutable = true,
                    width = template.video_width,
                    height = template.video_height,
                    alignMode = templateExtraList.align_mode
                )
            )
        }

        cutSameSource = CutSameSolution.createCutSameSource(
            SourceInfo(
                templateItem.templateUrl,
                templateItem.md5,
                templateItem.template_type
            )
        )
        if (savedInstanceState != null) {
            hasLaunchPicker = savedInstanceState.getBoolean("hasLaunchPicker", false)
            hasLaunchNext = savedInstanceState.getBoolean("hasLaunchNext", false)
            hasLaunchClip = savedInstanceState.getBoolean("hasLaunchClip", false)
            LogUtil.d(
                TAG,
                "onCreate restore hasLaunchPicker=$hasLaunchPicker, hasLaunchNext=$hasLaunchNext, hasLaunchClip=$hasLaunchClip"
            )
        }
        checkDataOkOrNot()
    }

    override fun onResume() {
        super.onResume()
        if (!isScreenOn(this)) {
            return
        }
        isForeground = true
        compileNextIntent = null
        Log.d(TAG, "onResume  isPlayingOnPause $isPlayingOnPause")
        if (isPlayingOnPause) {
            cutSamePlayer?.start()
        }
    }

    override fun onPause() {
        isForeground = false
        super.onPause()
        if (cutSamePlayer?.getState() == BasePlayer.PlayState.PLAYING) {
            isPlayingOnPause = true
            cutSamePlayer?.pause()
        } else {
            isPlayingOnPause = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cutSameSource?.release()
        cutSameSource = null

        cutSamePlayer?.release()
        cutSamePlayer = null
    }

    private fun checkDataOkOrNot() {
        val needPickMediaItems = mutableMediaItemList.filter {
            !File(it.source).exists()
        }
        val videoCachePath = CutSameUiIF.getTemplateVideoCacheByIntent(intent)

        if (!needPickMediaItems.isNullOrEmpty()) {

            // TODO: 2022/10/18 模拟其他业务的单选逻辑
            val isSingleChoose = true
            if (isSingleChoose) {
                launchPicker()
            } else {
                launchPicker(ArrayList(needPickMediaItems), videoCachePath!!)
            }


        } else {
            initTemplateWhileDataReady(
                templateItem.templateUrl,
                mutableMediaItemList,
                mutableTextItemList
            )
        }
    }

    fun updateTextItem(materialId: String, text: String) {
        val textItemList = mutableTextItemList
        if (textItemList != null) {
            val index = textItemList.indexOfFirst { it.materialId == materialId }
            LogUtil.d(TAG, "updateTextItem materialId = $materialId, index=$index")
            if (index != -1) {
                val errorCode = cutSamePlayer?.updateText(materialId, text)
                    ?: TemplateError.PLAYER_STATE_ERROR
                if (errorCode == TemplateError.SUCCESS) {
                    textItemList[index] = textItemList[index].copy(
                        text = text
                    )
                    onPlayerTextItemUpdate(textItemList[index])
                } else {
                    LogUtil.e(TAG, "updateTextItem error = $errorCode")
                }
            }
        }
    }

    /**
     * 单个更新素材 -- 唤起裁剪/替换视频
     * @return 是否真的唤起了
     */
    fun launchClip(item: MediaItem): Boolean {
        LogUtil.d(TAG, "launchClip templatePlayerErrorCode $templatePlayerErrorCode")
        if (templatePlayerErrorCode != TemplateError.SUCCESS) {
            return false
        }

        val createClipUIIntent = CutSameUiIF.createClipUIIntent(this, item)
        if (createClipUIIntent != null) {
            createClipUIIntent.putExtras(intent)
            createClipUIIntent.putParcelableArrayListExtra(
                CutSameUiIF.ARG_DATA_PRE_PICK_RESULT_MEDIA_ITEMS,
                cutSamePlayer?.getMediaItems()
            )
            createClipUIIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            hasLaunchClip = true
            startActivityForResult(createClipUIIntent, REQUEST_CODE_CLIP)
            onClipStart(item)
            return true
        }

        LogUtil.d(TAG, "createClipUIIntent == null, can not launchClip")
        return false
    }

    /**
     * 批量更新素材 -- 唤起相册
     * @return 是否真的唤起了
     */
    private fun launchPicker(itemList: ArrayList<MediaItem>, videoCache: String): Boolean {
        LogUtil.d(TAG, "launchPicker")

        val pickerIntent =
            PickerConstant.createGalleryUIIntent(
                this,
                CutSameMediaUtils.cutSameToOlaMediaItemList(itemList),
                parseTemplateItem(templateItem)
            )?.let {
                it.putExtra(CutSameUiIF.ARG_CUT_TEMPLATE_VIDEO_PATH, videoCache)
                it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        if (pickerIntent != null) {
            hasLaunchPicker = true
            startActivityForResult(pickerIntent, REQUEST_CODE_PICKER)
            return true
        }

        LogUtil.d(TAG, "can not launchPicker, pickerIntent==null")
        return false
    }

    /**
     * 单选的逻辑
     */
    private fun launchPicker(): Boolean {

        val imagePickConfig = ImagePickConfig()
        imagePickConfig.crop = true
        imagePickConfig.cropStyle = ImagePickConfig.CIRCLE
        imagePickConfig.defaultResourceType = ImagePickConfig.SELECT_IMAGE
        imagePickConfig.maxCount = 1
        imagePickConfig.focusHeight = SizeUtil.dp2px(300f)
        imagePickConfig.focusWidth = SizeUtil.dp2px(300f)
        imagePickConfig.sceneType = ImagePickConfig.PICKER_SINGLE

        val pickerIntent = PickerConstant.createSingleGalleryUIIntent(this, imagePickConfig)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        if (pickerIntent != null) {
            hasLaunchPicker = true
            startActivityForResult(pickerIntent, REQUEST_CODE_SINGLE_CHOOSE)
            return true
        }
        LogUtil.d(TAG, "can not launchPicker, pickerIntent==null")
        return false
    }

    fun parseTemplateItem(templateItem: TemplateItem): com.ola.chat.picker.entry.TemplateItem {
        val jackYang = Author("https://photo.tuchong.com/250829/f/31548923.jpg", "JackYang", 10086)
        val cover = Cover(
            "http://lf3-ck.bytetos.com/obj/template-bucket/7117128998756794382/baiyueguangzhushazhi_7002448424021524488/cover.png",
            720,
            1280
        )
        val originVideoInfo =
            OriginVideoInfo("http://lf3-ck.bytetos.com/obj/template-bucket/7117128998756794382/baiyueguangzhushazhi_7002448424021524488/baiyueguangzhushazhi_7002448424021524488.mp4")
        return com.ola.chat.picker.entry.TemplateItem(
            jackYang,
            templateItem.title,
            templateItem.md5,
            templateItem.template_type,
            templateItem.provider_media_id,
            originVideoInfo,
            templateItem.extra,
            templateItem.templateUrl,
            "",
            cover,
            templateItem.fragmentCount,
            templateItem.id,
            originVideoInfo,
            templateItem.shortTitle,
            templateItem.templateTags
        )
    }

    fun launchMediaReplace(mediaItem: MediaItem) {
        val items = ArrayList<MediaItem>().apply {
            add(mediaItem)
        }

        val galleryUIIntent =
            CutSameUiIF.createGalleryUIIntent(this, items, templateItem)?.putExtras(intent)
        if (galleryUIIntent == null) {
            Toast.makeText(
                this,
                getString(R.string.cutsame_pick_tip_album_not_found),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        onReplaceStart(mediaItem)
        startActivityForResult(galleryUIIntent, REQUEST_CODE_REPLACE)
    }


    /**
     * 批量更新素材 -- 视频合成
     */
    fun launchCompile() {
        cutSamePlayer?.pause()
        val createExportUIIntent = CutSameUiIF.createExportUIIntent(
            this,
            templateItem.templateUrl,
            templateItem.md5,
            cutSamePlayer!!.getConfigCanvasSize()
        )
        startActivityForResult(createExportUIIntent, REQUEST_CODE_NEXT)
    }

    private fun initTemplateWhileDataReady(
        templateUrl: String,
        mediaItemList: ArrayList<MediaItem>?,
        textItemList: ArrayList<TextItem>?
    ) {
        if (cutSamePlayer == null) {
            onPlayerDataOk()
            initTemplateData(templateUrl, mediaItemList, textItemList, getPlayerSurfaceView())
            onPlayerInitOk()
        }
        window.decorView.setBackgroundColor(Color.BLACK)
    }

    private fun initTemplateData(
        templateUrl: String,
        mediaItemList: ArrayList<MediaItem>?,
        textItemList: ArrayList<TextItem>?,
        videoSurfaceView: SurfaceView
    ) {
        cutSamePlayer = CutSameSolution.createCutSamePlayer(videoSurfaceView, templateUrl)
        cutSamePlayer?.preparePlay(mediaItemList, textItemList, object : PlayerStateListener {
            override fun onFirstFrameRendered() {
                onPlayerFirstFrameOk()
            }

            override fun onChanged(state: Int) {
                when (state) {
                    PlayerStateListener.PLAYER_STATE_PREPARED -> {
                        LogUtil.d(TAG, "PLAYER_STATE_PREPARED")
                        val textItems = cutSamePlayer?.getTextItems()
                        if (textItems != null) {
                            //视频合成之后，获取文本
                            mutableTextItemList = ArrayList(textItems)
                        }
                        onPlayerPrepareOk()
                        if (!isScreenOn(this@CutPlayerActivity)) {
                            return
                        }
                        cutSamePlayer?.start()
                    }
                    PlayerStateListener.PLAYER_STATE_PLAYING -> {
                        onPlayerPlaying(true)
                    }
                    PlayerStateListener.PLAYER_STATE_ERROR -> {
                        onPlayerPlaying(false)
                    }
                    PlayerStateListener.PLAYER_STATE_IDLE -> {
                        onPlayerPlaying(false)
                    }
                    PlayerStateListener.PLAYER_STATE_PAUSED -> {
                        onPlayerPlaying(false)
                    }
                    PlayerStateListener.PLAYER_STATE_DESTROYED -> {
                        Log.d(TAG, "PLAYER_STATE_DESTROYED")
                        onPlayerDestroy()
                    }
                }
            }

            override fun onPlayEof() {
                onPlayerPlaying(false)
            }

            override fun onPlayError(what: Int, extra: String) {
            }

            override fun onPlayProgress(process: Long) {
                onPlayerProgress(process)
            }
        })
    }

    private fun mergeMediaItemList(
        target: MutableList<MediaItem>, // 来自 templateSource 的数据
        source: List<MediaItem>         // 来自 相册选择页 的数据
    ) {
        target.forEachIndexed { index, ta ->
            if (!ta.isMutable) {
                return@forEachIndexed
            }
            val item = source.find { it.materialId == ta.materialId }
            if (item != null) {
                target[index] = target[index].copy(
                    source = item.source,
                    mediaSrcPath = item.mediaSrcPath,
                    sourceStartTime = item.sourceStartTime,
                    targetStartTime = item.targetStartTime,
                    crop = item.crop,
                    cropScale = item.cropScale,
                    oriDuration = item.oriDuration,
                    volume = item.volume,
                    type = item.type,
                    alignMode = item.alignMode,
                )
                return@forEachIndexed
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_NEXT -> {
                LogUtil.d(TAG, "REQUEST_CODE_NEXT resultCode=$resultCode")
                //导出页面返回到该页面
                if (resultCode == Activity.RESULT_OK) {
                    finish()
                }
            }

            REQUEST_CODE_PICKER -> {
                //选择素材返回到该页面
                hasLaunchPicker = false
                LogUtil.d(TAG, "REQUEST_CODE_PICKER resultCode=$resultCode, data=$data")
                if (resultCode == RESULT_OK && data != null) {
                    val items = CutSameUiIF.getGalleryPickResultData(data)
                    LogUtil.d(TAG, "REQUEST_CODE_PICKER items size ${items?.size}")
                    val mediaItemListFromPrepare = mutableMediaItemList

                    if (items != null) {
                        mergeMediaItemList(mediaItemListFromPrepare, items)
                        if (originMediaItemList.isNullOrEmpty()) {
                            originMediaItemList = ArrayList(mediaItemListFromPrepare)
                        }

                        checkDataOkOrNot()
                    } else {
                        LogUtil.d(TAG, "REQUEST_CODE_PICKER items == null, finish")
                        overridePendingTransition(0, 0)
                        finish()
                    }
                } else {
                    LogUtil.d(TAG, "REQUEST_CODE_PICKER resultCode != ok,or data==null, finish")
                    overridePendingTransition(0, R.anim.abc_slide_out_bottom)
                    finish()
                }
            }

            REQUEST_CODE_CLIP -> {
                //替换素材返回到该页面
                hasLaunchClip = false
                var processItem: MediaItem? = null
                LogUtil.d(TAG, "REQUEST_CODE_CLIP resultCode $resultCode")
                if (resultCode == Activity.RESULT_OK) {
                    processItem = data?.getParcelableExtra(CutSameUiIF.ARG_DATA_CLIP_MEDIA_ITEM)
                    if (processItem != null) {
                        val currentList = mutableMediaItemList
                        val index =
                            currentList.indexOfFirst { it.materialId == processItem.materialId }
                        if (index != -1) {
                            currentList[index] = currentList[index].copy(
                                source = processItem.source,
                                sourceStartTime = processItem.sourceStartTime,
                                crop = processItem.crop,
                                type = processItem.type,
                                mediaSrcPath = processItem.mediaSrcPath
                            )
                            // update player
                            cutSamePlayer?.updateMedia(processItem.materialId, processItem)
                            onPlayerMediaItemUpdate(currentList[index])
                            // 再调用一下，与 Picker 对齐
                            checkDataOkOrNot()
                            LogUtil.d(TAG, "REQUEST_CODE_CLIP result ${currentList[index]}")

                        } else {
                            LogUtil.e(TAG, "REQUEST_CODE_CLIP invalid processItem")
                        }
                    } else {
                        LogUtil.e(TAG, "REQUEST_CODE_CLIP processItem is null")
                    }
                } else {
                    LogUtil.e(TAG, "REQUEST_CODE_CLIP resultCode!=ok")
                }

                cutSamePlayer?.seekTo(processItem?.targetStartTime?.toInt() ?: 0, true)
                onClipFinish(processItem)
            }

            REQUEST_CODE_REPLACE -> {
                LogUtil.d(TAG, "REQUEST_CODE_REPLACE resultCode $resultCode")
                if (resultCode == RESULT_OK && data != null) {
                    val items = CutSameUiIF.getGalleryPickResultData(data)
                    if (items != null && items.size > 0) {
                        val processItem = items[0]
                        processItem.crop = calMaterialChangedCrop(processItem)
                        val currentList = mutableMediaItemList
                        val index =
                            currentList.indexOfFirst { it.materialId == processItem.materialId }
                        if (index != -1) {
                            currentList[index] = currentList[index].copy(
                                source = processItem.source,
                                sourceStartTime = 0,
                                crop = processItem.crop,
                                type = processItem.type,
                                mediaSrcPath = processItem.mediaSrcPath
                            )
                            LogUtil.d(
                                TAG,
                                "REQUEST_CODE_CLIP  source = ${processItem.source}, sourceStartTime = ${processItem.sourceStartTime}, crop=${processItem.crop}"
                            )
                            cutSamePlayer?.updateMedia(processItem.materialId, processItem)
                            onPlayerMediaItemUpdate(currentList[index])
                            checkDataOkOrNot()
                            LogUtil.d(TAG, "REQUEST_CODE_CLIP clip : ${currentList[index]}")
                            cutSamePlayer?.seekTo(processItem.targetStartTime.toInt(), true)
                            onReplaceFinish(processItem)
                        } else {
                            LogUtil.e(TAG, "REQUEST_CODE_CLIP invalid items")
                        }
                    } else {
                        LogUtil.e(TAG, "REQUEST_CODE_CLIP items is null")
                    }
                } else {
                    LogUtil.e(TAG, "REQUEST_CODE_CLIP resultCode!=ok")
                }
            }

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    abstract fun getPlayerSurfaceView(): SurfaceView

    /**
     * 播放数据准备完成 [mutableMediaItemList], [mutableTextItemList]，准备开始初始化播放器实例；
     * 请初始化你的界面 [AppCompatActivity.setContentView]
     * 请准备好 SurfaceView 实例 [getPlayerSurfaceView]
     */
    @MainThread
    open fun onPlayerDataOk() {
        LogUtil.d(TAG, "onPlayerDataOk")
    }

    /**
     * 播放器初始化完成，正在等待它 prepare 成功
     */
    open fun onPlayerInitOk() {
        LogUtil.d(TAG, "onPlayerInitOk")
    }

    /**
     * 视频 变更
     */
    @MainThread
    open fun onPlayerMediaItemUpdate(item: MediaItem) {
        LogUtil.d(TAG, "onPlayerMediaItemUpdate : $item")
    }

    /**
     * 文字 变更
     */
    @MainThread
    open fun onPlayerTextItemUpdate(item: TextItem) {
        LogUtil.d(TAG, "onPlayerTextItemUpdate : $item")
    }


    /**
     * 播放器prepare完成，即将触发 start()
     */
    open fun onPlayerPrepareOk() {
        LogUtil.d(TAG, "onPlayerPrepareOk")
    }

    /**
     * 播放器prepare失败了
     */
    open fun onPlayerError(errorCode: Int, errorMessage: String?) {
        LogUtil.e(TAG, "initPlayer onError, code=$errorCode, message=$errorMessage")
        templatePlayerErrorCode = errorCode
    }

    /**
     * 播放器已经开始播放了，第一帧渲染完成
     */
    open fun onPlayerFirstFrameOk() {
        LogUtil.d(TAG, "onPlayerFirstFrameOk")
    }

    /**
     * 播放器进度回调
     */
    open fun onPlayerProgress(progress: Long) {
    }

    /**
     * 播放器是否播放中
     */
    open fun onPlayerPlaying(isPlaying: Boolean) {
        LogUtil.d(TAG, "onPlayerPlaying : $isPlaying")
    }

    /**
     * 播放器销毁
     */
    open fun onPlayerDestroy() {
        LogUtil.d(TAG, "onPlayerDestroy")
    }

    /**
     * 离开页面去了裁剪页
     */
    @MainThread
    open fun onClipStart(item: MediaItem) {
        LogUtil.d(TAG, "onClipStart : $item")
    }

    /**
     * 离开页面去了裁剪页 回来了
     */
    @MainThread
    open fun onClipFinish(item: MediaItem?) {
        LogUtil.d(TAG, "onClipFinish : $item")
    }

    /**
     * 离开页面去了替换页
     */
    @MainThread
    open fun onReplaceStart(item: MediaItem) {
        LogUtil.d(TAG, "onReplaceStart : $item")
    }

    /**
     * 离开页面去了替换页 回来了
     */
    @MainThread
    open fun onReplaceFinish(item: MediaItem?) {
        LogUtil.d(TAG, "onReplaceFinish : $item")
    }

    private fun calMaterialChangedCrop(mediaItem: MediaItem): ItemCrop {
        if (MediaItem.ALIGN_MODE_VIDEO != mediaItem.alignMode) {
            return ItemCrop(0F, 0F, 1F, 1F)
        }

        val videoInfo = MediaUtil.getRealVideoMetaDataInfo(this@CutPlayerActivity, mediaItem.source)
        var width = videoInfo.width
        var height = videoInfo.height
        if (videoInfo.rotation == 90 || videoInfo.rotation == 270) {
            width = videoInfo.height
            height = videoInfo.width
        }
        var scale = 1.0f
        if (width > 0 && height > 0) { // if file not exist，width=height=0
            scale = getSaleFactorMax(
                width.toFloat(), height.toFloat(),
                mediaItem.width.toFloat(), mediaItem.height.toFloat()
            )
        }
        LogUtil.d(
            TAG, "checkScale: id=" + mediaItem.materialId
                    + ", videoSegment w/h=" + mediaItem.width + "/" + mediaItem.height
                    + ", video w/h=" + width + "/" + height
                    + ", scale=" + scale
        )
        val upperLeftX: Float =
            ((width * scale / 2.0f - mediaItem.width / 2.0f) / (width * scale)).run {
                getValidValue(
                    this
                )
            }
        val upperLeftY: Float =
            ((height * scale / 2.0f - mediaItem.height / 2.0f) / (height * scale)).run {
                getValidValue(
                    this
                )
            }
        val lowerRightX: Float =
            ((width * scale / 2.0f + mediaItem.width / 2.0f) / (width * scale)).run {
                getValidValue(
                    this
                )
            }
        val lowerRightY: Float =
            ((height * scale / 2.0f + mediaItem.height / 2.0f) / (height * scale)).run {
                getValidValue(
                    this
                )
            }
        LogUtil.d(
            TAG, "checkScale: id=" + mediaItem.materialId
                    + ", LUX=" + upperLeftX + ", LUY=" + upperLeftY + ", RDX=" + lowerRightX + ", RDY=" + lowerRightY
        )
        return ItemCrop(upperLeftX, upperLeftY, lowerRightX, lowerRightY)
    }

    private fun getValidValue(value: Float): Float {
        if (value < 0) {
            return 0.0f
        }

        if (value > 1) {
            return 1.0f
        }

        return value
    }

    private fun getSaleFactorMax(
        srcX: Float,
        srcY: Float,
        distX: Float,
        distY: Float
    ): Float {
        val sx = distX / srcX
        val sy = distY / srcY
        return sx.coerceAtLeast(sy)
    }
}
