package com.ola.editor.kit.cutsame.cut.textedit.view

import android.app.Dialog
import android.content.Context
import android.view.*
import android.widget.TextView
import com.ola.editor.kit.R
import com.ola.editor.kit.cutsame.utils.SizeUtil

class TextEditQuitDialog(context: Context, theme: Int) : Dialog(context, theme) {

    class Builder(private var context: Context) {
        private var title: String = ""
        private var subTitle: String = ""
        private var dialogOperationListener: DialogOperationListener? = null

        fun setTitleText(title: String): Builder {
            this.title = title
            return this
        }

        fun setSubTitleText(subTitle: String): Builder {
            this.subTitle = subTitle
            return this
        }

        fun setDialogOperationListener(dialogOperationListener: DialogOperationListener): Builder {
            this.dialogOperationListener = dialogOperationListener
            return this
        }

        fun create(): TextEditQuitDialog {
            val dialog = TextEditQuitDialog(context, R.style.Dialog)
            val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_text_edit_quit, null)
            initTitle(view)
            initSubTitle(view)
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
            view.findViewById<TextView>(R.id.cancelBtn).setOnClickListener {
                dialogOperationListener?.onClickCancel()
                dialog.dismiss()
            }

            view.findViewById<TextView>(R.id.sureBtn).setOnClickListener {
                dialogOperationListener?.onClickSure()
                dialog.dismiss()
            }
        }

        private fun initTitle(view: View) {
            view.findViewById<TextView>(R.id.titleView).text = title
        }

        private fun initSubTitle(view: View) {
            view.findViewById<TextView>(R.id.subTitleView).text = subTitle
        }

    }

    interface DialogOperationListener{
        fun onClickSure()
        fun onClickCancel()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            dismiss()
        }
        return super.onKeyDown(keyCode, event)
    }
}