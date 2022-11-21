package com.cutsame.ui.cut.lyrics

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import com.cutsame.ui.R
import com.cutsame.ui.customview.SpacesItemDecoration
import com.cutsame.ui.cut.lyrics.colorselect.OnColorSelectedListener
import com.cutsame.ui.cut.preview.CutPlayerActivity
import com.cutsame.ui.utils.FileUtil
import com.cutsame.ui.utils.JsonHelper
import com.cutsame.ui.utils.UniversalHorizontalLayoutManager
import com.ola.chat.picker.utils.SizeUtil
import kotlinx.android.synthetic.main.layout_lyrics_edit_view.view.*
import java.io.File

/**
 * 歌词样式View
 */
@SuppressLint("ViewConstructor")
class PlayerMaterialLyricsView @JvmOverloads constructor(
    private val activity: CutPlayerActivity,
    attrs: AttributeSet? = null
) : FrameLayout(activity, attrs) {

    private val FONT_STYLE_ROOT_DIR =
        "/storage/emulated/0/Android/data/com.starify.ola.android/files/assets/LocalResource/lyricStyle/"
    private val DEFAULT_FONT_STYLE_DIR = FONT_STYLE_ROOT_DIR + "jingdian" //内置的默认歌词效果
    private val SUBTITLE_EFFECT_FILE = FONT_STYLE_ROOT_DIR + "jingdian/jingdian"//内置默认歌词动效
    private var SUBTITLE_FONT_FILE = FONT_STYLE_ROOT_DIR + "jingdian/jingdianfont"//内置默认歌词字体
    private val LYRICS_STYLE_FONT_PATH = FONT_STYLE_ROOT_DIR + "text_font"//后端拉取的用户可配置字体
    private var DRAFT_STYLE_FONT_PATH = FONT_STYLE_ROOT_DIR + "draft/draft_"//用户临时选取的字体


    private var currentColor = Color.parseColor("#FFFFFFFF").toLong()
    private var currentFont = SUBTITLE_FONT_FILE

    init {
        initView(context)
        parseFontData()
        FileUtil.removeFile(FONT_STYLE_ROOT_DIR + "draft")
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
        val colorList = ArrayList<Long>()
        colorList.add(Color.parseColor("#FF03DAC5").toLong())
        colorList.add(Color.parseColor("#ff0000ff").toLong())
        colorList.add(Color.parseColor("#ffff0000").toLong())
        colorList.add(Color.parseColor("#ffffffff").toLong())
        colorList.add(Color.parseColor("#fffff000").toLong())
        colorList.add(Color.parseColor("#ff000fff").toLong())
        colorList.add(Color.parseColor("#FF018786").toLong())
        colorList.add(Color.parseColor("#ffF0BF42").toLong())
        colorList.add(Color.parseColor("#FFBB86FC").toLong())
        colorList.add(Color.parseColor("#ffff8800").toLong())
        colorList.add(Color.parseColor("#ff1AAD19").toLong())
        colorList.add(Color.parseColor("#ff1C2134").toLong())
        colorList.add(Color.parseColor("#CCF44336").toLong())

        materialColorSelectContainer.setColorList(colorList)
        materialColorSelectContainer.setColorSelectedListener(object : OnColorSelectedListener {
            override fun onColorSelected(colorItem: Long) {
                currentColor = colorItem
                updateLyricsSticker()
            }
        })
    }

    private fun parseFontData() {
        val fontString: String =
            FileUtil.readJsonFile(LYRICS_STYLE_FONT_PATH + File.separator + "font.json")
        val fontItem: FontItemEntry? = JsonHelper.fromJson(fontString, FontItemEntry::class.java)

        initData(fontItem!!.resource.list) { ttfPath, position ->
            //由于API的限制，字体的修改，只能通过迂回的方式将选中的字体copy到默认的目录下和config.json&fontPath.json里面的配置保持一致
            moveFontToFakePath(ttfPath)
        }
    }

    private fun moveFontToFakePath(ttfPath: String) {
        val draftDir = DRAFT_STYLE_FONT_PATH + System.currentTimeMillis()
        FileUtil.copyDir(DEFAULT_FONT_STYLE_DIR, draftDir)
        val textFontPath = draftDir + File.separator + "jingdianfont"
        if (FileUtil.copyFileToDraft(ttfPath, textFontPath, "yonghuaiti.ttf")) {
            currentFont = textFontPath
            updateLyricsSticker()
        }
    }

    /**
     * 由于字体文件所在的目录里包含加密的json文件，里面写死了目录里字体文件的名字，
     * 因此如果需要使用自己的字体，需要将字体文件重命名成跟我们提供的字体资源相同的名字才能生效
     */
    private fun updateLyricsSticker() {
        if (null == activity.cutSamePlayer) {
            Toast.makeText(context, "cutSamePlayer获取失败！", Toast.LENGTH_SHORT).show()
            return
        }
        activity.cutSamePlayer?.updateSubtitleSticker(
            textColor = currentColor,
            textFontPath = currentFont,
            subtitleEffectFilePath = SUBTITLE_EFFECT_FILE
        )
    }

    private fun initData(
        fontItemList: MutableList<FontItemEntry.FontResource.FontItem>,
        itemClickListener: OnLyricsItemClickListener
    ) {
        val adapter = LyricsFontRecyclerViewAdapter(context, fontItemList, LYRICS_STYLE_FONT_PATH)
        materialLyricsRecycleView.adapter = adapter
        adapter.setOnItemClickListener(itemClickListener)
    }
}
