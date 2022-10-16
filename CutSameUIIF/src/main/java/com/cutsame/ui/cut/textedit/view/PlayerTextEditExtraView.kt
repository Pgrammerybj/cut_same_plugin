package com.cutsame.ui.cut.textedit.view

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Editable
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import com.cutsame.ui.R
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.cut.CutSameDesignDrawableFactory
import com.cutsame.ui.cut.textedit.PlayerTextBoxData
import com.cutsame.ui.cut.textedit.listener.PlayerTextViewExtraListener
import com.cutsame.ui.cut.textedit.listener.TextWatcherAdapter
import com.cutsame.ui.utils.SizeUtil
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * 主要包含点击编辑框和播放按钮逻辑，文字框
 */
class PlayerTextEditExtraView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private lateinit var contentRootView: View
    private lateinit var editTextView: EditText
    private lateinit var editTextLayout: View
    private lateinit var editFinishView: View
    private lateinit var textBoxView: View

    private val textBoxLeftRightPadding: Int
    private val textBoxTopBottomPadding: Int
    private var extraListener: PlayerTextViewExtraListener? = null




    init {
        initView(context)
        textBoxLeftRightPadding = context.resources.getDimensionPixelOffset(R.dimen.video_player_text_box_leftright)
        textBoxTopBottomPadding = context.resources.getDimensionPixelOffset(R.dimen.video_player_text_box_topbottom)
    }

    private fun initView(context: Context) {
        contentRootView = LayoutInflater.from(context).inflate(R.layout.layout_textedit_extra_view, this, true)
        editTextView = contentRootView.findViewById(R.id.bottom_edit_text)
        editTextLayout = contentRootView.findViewById(R.id.bottom_edit_text_layout)
        editFinishView = contentRootView.findViewById(R.id.finish_edit_text)
        textBoxView = contentRootView.findViewById(R.id.text_box_view)

        val textBoxDrawable = CutSameDesignDrawableFactory.createRectNormalDrawable(Color.parseColor("#4d000000"), Color.parseColor("#4d000000"),
            SizeUtil.dp2px(1f), SizeUtil.dp2px(8f))
        textBoxView.background = textBoxDrawable

        editFinishView.setGlobalDebounceOnClickListener { extraListener?.clickFinishEditTextView(editTextView.text.toString()) }

        editTextView.addTextChangedListener(object : TextWatcherAdapter() {

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (count - before >= 1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.M )   {
                    val input: CharSequence = s.subSequence(
                        start + before,
                        start + count
                    )
                    if (isEmoji(input.toString())) {
                        Toast.makeText(context, R.string.cutsame_not_support_emoji, Toast.LENGTH_SHORT).show()
                        (s as SpannableStringBuilder).delete(
                            start + before,
                            start + count
                        )
                    }
                }
            }
            override fun afterTextChanged(s: Editable) {
                extraListener?.editTextChange(s.toString())
            }
        })
    }

    private fun isEmoji(input: String): Boolean {
        val p: Pattern = Pattern.compile(
            "[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\ud83e\udc00-\ud83e\udfff]" +
                    "|[\u2100-\u32ff]|[\u0030-\u007f][\u20d0-\u20ff]|[\u0080-\u00ff]"
        )
        val m: Matcher = p.matcher(input)
        return m.find()
    }

    fun setPlayerExtraListener(listener: PlayerTextViewExtraListener?) {
        extraListener = listener
    }

    /**
     * 是否展示文本框
     * @param isShow
     */
    fun showOrHideTextBoxView(isShow: Boolean) {
        textBoxView.visibility = if (isShow) View.VISIBLE else View.GONE
    }

    /**
     * 是否展示文字编辑框
     * @param isShow
     */
    fun showOrHideEditView(isShow: Boolean, methodManager: InputMethodManager?) {
        if (isShow) {
            editTextView.requestFocus()
            showInputMethod(methodManager)
        } else {
            editTextView.clearFocus()
            hideInputMethod(methodManager)
        }
    }

    /**
     * 更新文字内容
     */
    fun updateEditText(text: String) {
        editTextView.setText(text)
        editTextView.setSelection(text.length)
    }

    /**
     * 更新文本框位置
     * @param data
     */
    fun updateTextBoxViewLocation(data: PlayerTextBoxData?) {
        if (data == null) {
            return
        }

        val angle = data.angle
        textBoxView.rotation = angle

        showOrHideTextBoxView(true)

        val params = textBoxView.layoutParams as LayoutParams

        //视频的surface和canvas的差值，surface是展示区域，canvas是可见区域
        val deltaX = data.originSurfaceWidth - data.originCanvasWidth
        val deltaY = data.originSurfaceHeight - data.originCanvasHeight

        var leftMargin = data.leftRightMargin//左右边距
        leftMargin += deltaX / 2 //本身展示区域和可见区域的差值左右各一半

        var topMargin = data.topMargin //顶部边距
        topMargin += deltaY / 2 //本身展示区域和可见区域的差值上下各一半

        val textWidth = (data.originCanvasWidth * data.x).toInt()
        val textHeight = (data.originCanvasHeight * data.y).toInt()

        if (textWidth < 0) {
            //左边超了
            params.leftMargin = leftMargin
        } else {
            params.leftMargin = leftMargin + textWidth - textBoxLeftRightPadding
        }
        if (textHeight < 0) {
            //顶部超了
            params.topMargin =  topMargin
        } else {
            params.topMargin = topMargin + textHeight - textBoxTopBottomPadding
        }
        params.width = (data.originCanvasWidth * data.width).toInt() + 2 * textBoxLeftRightPadding
        params.height = (data.originCanvasHeight * data.height).toInt() + 2 * textBoxTopBottomPadding

        textBoxView.layoutParams = params
    }

    fun getEditTextLayout(): View {
        return editTextLayout
    }

    fun getEditTextContent(): String? {
        return editTextView.editableText?.toString()
    }

    private fun showInputMethod(mIMManager: InputMethodManager?) {
        mIMManager?.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    private fun hideInputMethod(mIMManager: InputMethodManager?) {
        mIMManager?.hideSoftInputFromWindow(editTextView.windowToken, 0)
    }

}
