package com.cutsame.ui.cut.textedit.controller

import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.cutsame.ui.cut.textedit.PlayerAnimateHelper
import com.cutsame.ui.cut.textedit.PlayerTextBoxData
import com.cutsame.ui.cut.textedit.PlayerTextEditItemData
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditListenerAdapter
import com.cutsame.ui.cut.textedit.listener.PlayerTextViewExtraListener
import com.cutsame.ui.cut.textedit.view.PlayerTextEditExtraView


class PlayerTextExtraController {

    private lateinit var playerTextEditExtraView: PlayerTextEditExtraView

    private var mIMManager: InputMethodManager? = null

    private var isShowEditView: Boolean = false
    private var keyboardHeight: Int = 0
    private var curEditItemData: PlayerTextEditItemData? = null

    private var playerTextEditListenerAdapter: PlayerTextEditListenerAdapter? = null

    fun init(playerTextEditExtraView: PlayerTextEditExtraView) {
        this.playerTextEditExtraView = playerTextEditExtraView
        playerTextEditExtraView.setPlayerExtraListener(object : PlayerTextViewExtraListener {
            override fun clickFinishEditTextView(text: String?) {
                playerTextEditExtraView.showOrHideEditView(false, mIMManager)
                playerTextEditListenerAdapter?.finishClickEditText(curEditItemData)
            }

            override fun editTextChange(text: String?) {
                playerTextEditListenerAdapter?.inputTextChange(curEditItemData, text)
            }
        })

        mIMManager = playerTextEditExtraView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    fun setPlayerTextEditListenerAdapter(adapter: PlayerTextEditListenerAdapter) {
        this.playerTextEditListenerAdapter = adapter
    }

    fun showOrHideView(isShow: Boolean) {
        if (this::playerTextEditExtraView.isInitialized) {
            playerTextEditExtraView.visibility = if (isShow) View.VISIBLE else View.GONE
        }
    }

    fun showOrHideTextBoxView(isShow: Boolean) {
        if (!isShow) {
            playerTextEditExtraView.showOrHideTextBoxView(false)
            return
        }

        if (curEditItemData == null) {
            playerTextEditExtraView.showOrHideTextBoxView(false)
            return
        }

        //文字时间区域是否在当前播放区域
        val playPosition = playerTextEditListenerAdapter?.getCurPlayPosition() ?: return
        if (!curEditItemData!!.isContainerTimeRange(playPosition)) {
            playerTextEditExtraView.showOrHideTextBoxView(false)
            return
        }

        playerTextEditExtraView.showOrHideTextBoxView(true)
    }

    fun showOrHideEditView(isShow: Boolean) {
        playerTextEditExtraView.showOrHideEditView(isShow, mIMManager)
    }

    /**
     * 更新键盘高度
     * @param isShow
     * @param height
     */
    fun updateEditInputHeight(isShow: Boolean, height: Int) {
        Log.d("PlayerTextExtra","isShow ${isShow} height${height}")
        if (isShow && height > 0) {
            isShowEditView = true
            keyboardHeight = height
            PlayerAnimateHelper.animateTransHeightShowOrHide(
                    playerTextEditExtraView.getEditTextLayout(), height, isShow = true, isDelay = false)
            playerTextEditListenerAdapter?.showOrHideKeyboardView(true)
        } else if (isShowEditView && height == 0) {
            PlayerAnimateHelper.animateTransHeightShowOrHide(
                    playerTextEditExtraView.getEditTextLayout(), keyboardHeight, isShow = false, isDelay = false)
            isShowEditView = false
            keyboardHeight = 0
            playerTextEditListenerAdapter?.showOrHideKeyboardView(false)
        }
    }

    fun updateTextBoxViewLocation(data: PlayerTextBoxData?) {
        playerTextEditExtraView.updateTextBoxViewLocation(data)
    }

    /**
     * 更新文字内容
     */
    fun updateEditText() {
        if (curEditItemData == null) {
            return
        }
        if (curEditItemData!!.getEditText() == null) {
            return
        }
        playerTextEditExtraView.updateEditText(curEditItemData!!.getEditText()!!)
    }

    fun setCurEditItemData(data: PlayerTextEditItemData?) {
        curEditItemData = data
    }

    fun getEditTextContent(): String? {
        return playerTextEditExtraView.getEditTextContent()
    }

}
