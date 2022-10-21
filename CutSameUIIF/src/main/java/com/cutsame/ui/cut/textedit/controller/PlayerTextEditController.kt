package com.cutsame.ui.cut.textedit.controller

import android.graphics.Bitmap
import android.text.TextUtils
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.bytedance.ies.cutsame.util.SizeUtil
import com.cutsame.ui.R
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.cut.textedit.*
import com.cutsame.ui.cut.textedit.listener.KeyboardHeightProvider
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditListener
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditListenerAdapter
import com.cutsame.ui.cut.textedit.listener.PlayerTextItemThumbBitmapListener
import com.cutsame.ui.cut.textedit.view.PlayerMaterialTextEditView
import com.cutsame.ui.cut.textedit.view.TextEditQuitDialog
import com.cutsame.ui.utils.postOnUiThread
import com.ss.android.ugc.cut_ui.TextItem
import java.util.HashMap

class PlayerTextEditController(var playerTextEditView: PlayerMaterialTextEditView) : PlayerTextEditListenerAdapter() {

    private lateinit var fragmentActivity: FragmentActivity

    private var keyBoardListener: KeyboardHeightProvider? = null
    private var playerTextEditListener: PlayerTextEditListener? = null
    private var scaleListener: PlayerAnimateHelper.PlayerSurfaceScaleListener? = null

    //用于绘制文字白框
    private var scaleW: Float = 0.toFloat()
    private var scaleH: Float = 0.toFloat()
    private var originCanvasWidth: Int = 0
    private var originCanvasHeight: Int = 0
    private var surfaceTransY: Int = 0
    private var dataList: MutableList<PlayerTextEditItemData>? = null

    private lateinit var thumbCreator: PlayerTextThumbCreator
    private var playerTextExtraController = PlayerTextExtraController()

    private var isShowView = false

    var processPlayerTextBoxData: ((PlayerTextBoxData) -> Unit)? = null

    fun init(activity: FragmentActivity, contentView: View, editListener: PlayerTextEditListener?) {
        fragmentActivity = activity

        playerTextEditView.setGlobalDebounceOnClickListener { }

        playerTextExtraController.init(contentView.findViewById(R.id.play_edit_extra_view))
        playerTextExtraController.setPlayerTextEditListenerAdapter(this)

        playerTextEditListener = editListener
        playerTextEditView.initData()
        playerTextEditView.setEditListener(this)

        //获取视频的画布大小
        if (playerTextEditListener?.getCanvasSize() != null) {
            originCanvasWidth = playerTextEditListener?.getCanvasSize()!![0]
            originCanvasHeight = playerTextEditListener?.getCanvasSize()!![1]
        }

        //获取封面图
        thumbCreator = PlayerTextThumbCreator().setPlayerTextEditListenerAdapter(object :
            PlayerTextEditListenerAdapter() {
            override fun getItemFrameBitmap(
                times: IntArray?,
                width: Int,
                height: Int,
                listener: PlayerTextItemThumbBitmapListener?
            ) {
                playerTextEditListener?.getItemFrameBitmap(times, width, height, listener)
            }
        })

        contentView.findViewById<View>(R.id.videoPlayContainer)
            .setGlobalDebounceOnClickListener {
                playerTextExtraController.showOrHideEditView(false)
            }
    }

    fun updateDataList(textItems: List<TextItem>?) {
        if (dataList == null || dataList!!.isEmpty()) {
            dataList = PlayerTextEditHelper.covertItemTextData(textItems)
        }
        // TODO: 2022/10/14 此处为临时添加，记得删除
//        dataList?.get(4)?.let { dataList?.add(it) }
        playerTextEditView.updateTextData(dataList)
    }

    fun setScaleListener(scaleListener: PlayerAnimateHelper.PlayerSurfaceScaleListener?) {
        this.scaleListener = scaleListener
    }

    fun showTextEditView(): Boolean {
        if (!playerTextEditView.hasData()) {
            return false
        }

        //获取文字封面图
        var width = SizeUtil.dp2px(68f)
        var height = SizeUtil.dp2px(68f)
        if (originCanvasWidth > originCanvasHeight) {
            height =
                (width.toFloat() / (originCanvasWidth.toFloat() / originCanvasHeight.toFloat())).toInt()
        } else {
            width =
                (height.toFloat() / (originCanvasHeight.toFloat() / originCanvasWidth.toFloat())).toInt()
        }

        dataList?.let {
            thumbCreator.getThumb(it, width, height, object :
                PlayerTextThumbCreator.PlayerTextThumbAllBitmapListener {
                override fun thumbAllBitmap(bitmapHashMap: HashMap<String, Bitmap>?) {
                    bitmapHashMap ?: return
                    playerTextEditView.addThumbBitmap(bitmapHashMap)
                }
            })
        }
        isShowView = true

        return true
    }

    fun isShowing(): Boolean {
        return isShowView
    }

    fun release() {
        releaseKeyboardListener()
        //释放封面图
        if (this::thumbCreator.isInitialized) {
            thumbCreator.release()
        }
    }

    private fun isTextChange(): Boolean {
        if (dataList?.isEmpty() == false) {
            dataList!!.forEach {
                if (it.isChangeText()) {
                    return@isTextChange true
                }
            }
        }
        return false
    }

    override fun clickCancel() {
        //文字发生变化了才谈对话框
        if (isTextChange()) {
            showCancelDialog()
        } else {
            cancelWork()
        }
        //开始文字动画
        playerTextEditListener?.controlTextAnimate(
            playerTextEditView.curSelectItemData?.saltId,
            true
        )
    }

    override fun clickSave() {
        finishAnim()
        //开始文字动画
        playerTextEditListener?.controlTextAnimate(
            playerTextEditView.curSelectItemData?.saltId,
            true
        )
    }

    override fun selectTextItem(
        data: PlayerTextEditItemData?,
        pos: Int,
        seekDone: ((ret: Int) -> Unit)?
    ) {
        //禁止文字动画
        playerTextEditListener?.controlTextAnimate(data?.saltId, false)
        playerTextEditListener?.selectTextItem(data, pos) {
            postOnUiThread {
                //选择文字item需要展示白色框
                updateTextBoxViewLocation(!TextUtils.isEmpty(data?.getEditText()))
            }
        }
        playerTextExtraController.setCurEditItemData(data)
    }

    override fun clickEditTextItem(data: PlayerTextEditItemData?, pos: Int) {
        playerTextEditListener?.clickEditTextItem(data, pos)

        if (keyBoardListener == null) {
            keyBoardListener = KeyboardHeightProvider(fragmentActivity)
            keyBoardListener!!.setKeyboardHeightObserver { height, _ ->
                playerTextExtraController.updateEditInputHeight(true, height)
            }
            keyBoardListener!!.start()
        }
        playerTextExtraController.showOrHideEditView(true)
        playerTextExtraController.updateEditText()
    }

    override fun showOrHideKeyboardView(isShow: Boolean) {
        if (!isShow) {
            editTextFinish()
        }
        playerTextEditListener?.showOrHideKeyboardView(isShow)
        playerTextEditView.visibility = if (isShow) View.INVISIBLE else View.VISIBLE
    }

    override fun clickPlay(): Boolean {
        val playStatus = playerTextEditListener?.clickPlay() == true
        if (playStatus) {
            //开始文字动画
            playerTextEditListener?.controlTextAnimate(
                playerTextEditView.curSelectItemData?.saltId,
                true
            )
            playerTextExtraController.showOrHideTextBoxView(false)
            playerTextEditView.updateCurEditItemStatus(-1)
        }
        return playStatus
    }

    override fun getCurPlayPosition(): Long {
        return if (playerTextEditListener?.getCurPlayPosition() == null) {
            0
        } else {
            playerTextEditListener?.getCurPlayPosition()!!
        }
    }

    override fun finishClickEditText(itemData: PlayerTextEditItemData?) {
        playerTextEditListener?.finishClickEditText(itemData)
    }

    override fun inputTextChange(itemData: PlayerTextEditItemData?, text: String?) {
        //文本框输入内容，要试试看到预览效果
        playerTextEditListener?.inputTextChange(itemData, text)
        if (TextUtils.isEmpty(text)) {
            //内容无效就不展示框了
            playerTextExtraController.showOrHideTextBoxView(false)
        } else {
            updateTextBoxViewLocation(true)
        }
    }

    /**
     * 用户取消，还原文字功能
     */
    private fun restoreTextContent() {
        if (dataList?.isEmpty() == false) {
            for (itemData in dataList!!) {
                itemData.restoreData()
                playerTextEditListener?.inputTextChange(itemData, itemData.getEditText())
            }
        }
    }

    private fun updateTextBoxViewLocation(isValidText: Boolean) {

        playerTextExtraController.showOrHideTextBoxView(false)

        val itemData = playerTextEditView.curSelectItemData ?: return

        //无效文字不展示box框
        if (!isValidText) {
            return
        }

        val boxData = playerTextEditListener?.getCurItemTextBoxData(itemData) ?: return

        boxData.originCanvasWidth = originCanvasWidth
        boxData.originCanvasHeight = originCanvasHeight
        boxData.originSurfaceWidth = PlayerAnimateHelper.surfaceViewWidth
        boxData.originSurfaceHeight = PlayerAnimateHelper.surfaceViewHeight
        boxData.topMargin = PlayerAnimateHelper.topTitleViewHeight
        boxData.leftRightMargin = PlayerAnimateHelper.leftRightMargin
        boxData.scaleSizeW = scaleW
        boxData.scaleSizeH = scaleH
        boxData.transY = surfaceTransY.toFloat()
        processPlayerTextBoxData?.invoke(boxData)

        playerTextExtraController.updateTextBoxViewLocation(boxData)

        //暂停视频
        playerTextEditListener?.controlVideoPlaying(false)
    }

    private fun finishAnim() {
        playerTextExtraController.showOrHideView(false)
        playerTextExtraController.showOrHideTextBoxView(false)
        releaseKeyboardListener()
        isShowView = false
    }

    private fun releaseKeyboardListener() {
        if (keyBoardListener != null) {
            keyBoardListener!!.close()
            keyBoardListener = null
        }
    }

    private fun showCancelDialog() {
        TextEditQuitDialog.Builder(fragmentActivity)
            .setDialogOperationListener(object : TextEditQuitDialog.DialogOperationListener{
                override fun onClickSure() {
                    cancelWork()
                }

                override fun onClickCancel() {

                }
            })
            .setTitleText(fragmentActivity.getString(R.string.cutsame_edit_title_abandon_edit))
            .setSubTitleText(fragmentActivity.getString(R.string.cutsame_edit_tip_abandon_text_edit))
            .create()
            .show()
    }

    private fun cancelWork() {
        //恢复原有的文字内容
        restoreTextContent()
        playerTextEditListener?.clickCancel()
        finishAnim()
        playerTextEditView.refreshAll()
    }

    /**
     * 键盘收起就相当于完成文字内容更改,这里不区分是返回键，点击空白区域，点击完成，统统都是算是生效文字效果
     */
    private fun editTextFinish() {
        playerTextEditView.curSelectItemData?.setEditText(playerTextExtraController.getEditTextContent())
        playerTextEditView.updateCurEditItemText(playerTextEditView.curSelectItemData?.getEditText())
        inputTextChange(
            playerTextEditView.curSelectItemData,
            playerTextEditView.curSelectItemData?.getEditText()
        )
    }
}
