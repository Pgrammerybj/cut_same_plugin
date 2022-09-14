package com.cutsame.ui.cut.videoedit.customview

import android.app.Dialog
import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.cutsame.ui.R
import com.cutsame.ui.utils.SizeUtil

class TipDialog(context: Context, theme: Int) : Dialog(context, theme) {

    class Builder(private var context: Context) {
        private var title: String = ""
        private var sure: String = context.getString(R.string.cutsame_common_abandon)
        private var cancel: String = context.getString(R.string.cutsame_common_cancel)
        private var dialogOperationListener: DialogOperationListener? = null

        fun setTitleText(title: String): Builder {
            this.title = title
            return this
        }

        fun setSureText(sure: String): Builder {
            this.sure = sure
            return this
        }

        fun setCancelText(cancel: String): Builder {
            this.cancel = cancel
            return this
        }

        fun setDialogOperationListener(dialogOperationListener: DialogOperationListener): Builder {
            this.dialogOperationListener = dialogOperationListener
            return this
        }

        fun create(): TipDialog {
            val dialog = TipDialog(context, R.style.Dialog)
            val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_quit, null)
            initTitle(view)
            initListener(dialog, view)
            dialog.setCanceledOnTouchOutside(false)
            dialog.setContentView(view)
            val window = dialog.window
            val attributes = window?.attributes
            attributes?.width = SizeUtil.dp2px(275f)
            attributes?.height = SizeUtil.dp2px(158f)
            window?.attributes = attributes
            return dialog
        }

        private fun initListener(dialog: Dialog, view: View) {
            view.findViewById<TextView>(R.id.cancelBtn).apply {
                setOnClickListener {
                    dialogOperationListener?.onClickCancel()
                    dialog.dismiss()
                }
                text = cancel
            }


            view.findViewById<TextView>(R.id.sureBtn).apply {
                setOnClickListener {
                    dialogOperationListener?.onClickSure()
                    dialog.dismiss()
                }
                text = sure
            }
        }

        private fun initTitle(view: View) {
            view.findViewById<TextView>(R.id.titleView).text = title
        }
    }

    interface DialogOperationListener {
        fun onClickSure()
        fun onClickCancel()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            dismiss()
        }
        return super.onKeyDown(keyCode, event)
    }
}