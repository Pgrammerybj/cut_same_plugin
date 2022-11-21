package com.cutsame.ui.cut.lyrics

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import com.cutsame.ui.R
import com.cutsame.ui.customview.SpacesItemDecoration
import com.cutsame.ui.cut.lyrics.colorselect.OnColorSelectedListener
import com.cutsame.ui.utils.FileUtil
import com.cutsame.ui.utils.JsonHelper
import com.cutsame.ui.utils.UniversalHorizontalLayoutManager
import com.ola.chat.picker.utils.SizeUtil
import kotlinx.android.synthetic.main.layout_lyrics_edit_view.view.*
import java.io.File

/**
 * 歌词样式View
 */
class PlayerMaterialLyricsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val LYRICS_STYLE_FONT_PATH =
        "/storage/emulated/0/Android/data/com.starify.ola.android/files/assets/LocalResource/default/text_font"

    init {
        initView(context)
        mockFontDataList()
    }

    private fun initView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.layout_lyrics_edit_view, this, true)
        //歌词字体
        materialLyricsRecycleView.layoutManager = UniversalHorizontalLayoutManager(context)
        materialLyricsRecycleView.setHasFixedSize(true)
        materialLyricsRecycleView.addItemDecoration(
            SpacesItemDecoration(
                0,
                SizeUtil.dp2px(16f),
                rowCountLimit = 1
            )
        )

        //歌词颜色
        val colorList = ArrayList<Int>()
        colorList.add(Color.parseColor("#FF03DAC5"))
        colorList.add(Color.parseColor("#ff0000ff"))
        colorList.add(Color.parseColor("#ffff0000"))
        colorList.add(Color.parseColor("#ffffff00"))
        colorList.add(Color.parseColor("#ffffffff"))
        colorList.add(Color.parseColor("#fffff000"))
        colorList.add(Color.parseColor("#ff000fff"))
        colorList.add(Color.parseColor("#FF018786"))
        colorList.add(Color.parseColor("#ffF0BF42"))
        colorList.add(Color.parseColor("#FFBB86FC"))
        colorList.add(Color.parseColor("#ffff8800"))
        colorList.add(Color.parseColor("#ff1AAD19"))
        colorList.add(Color.parseColor("#ff1C2134"))
        colorList.add(Color.parseColor("#CCF44336"))

        materialColorSelectContainer.setColorList(colorList)
        materialColorSelectContainer.setColorSelectedListener(object : OnColorSelectedListener {
            override fun onColorSelected(colorItem: Int) {
                Toast.makeText(context, "点击了颜色:$colorItem", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initData(
        fontItemList: MutableList<FontItemEntry.FontResource.FontItem>,
        itemClickListener: OnLyricsItemClickListener
    ) {
        val adapter = LyricsFontRecyclerViewAdapter(context, fontItemList, LYRICS_STYLE_FONT_PATH)
        materialLyricsRecycleView.adapter = adapter
        adapter.setOnItemClickListener(itemClickListener)
    }


    /**
     * 模拟数据
     */
    private fun mockFontDataList() {

        val fontString: String =
            FileUtil.readJsonFile(LYRICS_STYLE_FONT_PATH + File.separator + "font.json")
        val fontItem: FontItemEntry? = JsonHelper.fromJson(fontString, FontItemEntry::class.java)

        //模拟调用
        initData(fontItem!!.resource.list, OnLyricsItemClickListener { ttfPath, position ->
            Toast.makeText(context, "点击了:$position:$ttfPath ", Toast.LENGTH_SHORT).show()
        })
    }
}
