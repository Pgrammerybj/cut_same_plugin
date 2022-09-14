package com.cutsame.ui.cut.textedit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bytedance.ies.cutsame.util.SizeUtil
import com.cutsame.ui.R
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.cut.CutSameDesignDrawableFactory
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditItemListener
import java.util.*

class PlayerTextEditAdapter(context: Context, private val itemListener: PlayerTextEditItemListener?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var dataList: List<PlayerTextEditItemData> = ArrayList()

    private var curSelectPos = 0

    //封面图map，退出会做释放操作
    private var thumbBitmapMap = HashMap<String, Bitmap>()

    val curSelectItemData: PlayerTextEditItemData?
        get() = if (curSelectPos < 0 || curSelectPos >= itemCount) {
            null
        } else dataList[curSelectPos]

    val curPos: Int
        get() = curSelectPos

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(
            R.layout.layout_textedit_item,
                viewGroup, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {
        (viewHolder as ItemViewHolder).bindView(dataList[i], i)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun setDataList(dataList: List<PlayerTextEditItemData>?) {
        if (dataList == null || dataList.isEmpty()) {
            return
        }
        this.dataList = dataList!!
        notifyDataSetChanged()
    }

    fun updateCurEditItemText(text: String?) {
        if (curSelectPos < 0 || curSelectPos >= itemCount || text == null) {
            return
        }
        val data = dataList[curSelectPos]
        data.setEditText(text)
        notifyItemChanged(curSelectPos)
    }

    fun addThumbBitmap(thumbBitmapMap: HashMap<String, Bitmap>?) {
        if (thumbBitmapMap == null || thumbBitmapMap.size == 0) {
            return
        }
        this.thumbBitmapMap = thumbBitmapMap
        notifyDataSetChanged()
    }

    fun updateCurSelectStatusView(pos: Int) {
        if (curSelectPos == pos) {
            return
        }
        val preSelectPos = curSelectPos
        curSelectPos = pos
        if (curSelectPos in 0 until itemCount) {
            notifyItemChanged(curSelectPos)
        }
        if (preSelectPos in 0 until itemCount) {
            notifyItemChanged(preSelectPos)
        }
    }
    fun zoomBitmap(bitmap: Bitmap, w: Int, h: Int): Bitmap? {
        val width = bitmap.width
        val height = bitmap.height
        val matrix = Matrix()
        val scaleWidth = w.toFloat() / width
        val scaleHeight = h.toFloat() / height
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(
            bitmap, 0, 0, width, height,
            matrix, true
        )
    }

    internal inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val thumbImageView: ImageView = itemView.findViewById(R.id.thumbView)
        private val textContentView: TextView = itemView.findViewById(R.id.textContentView)
        private val selectedView: TextView = itemView.findViewById(R.id.selectedView)
        private val numberTextView: TextView = itemView.findViewById(R.id.numberTextView)

        fun bindView(itemData: PlayerTextEditItemData?, pos: Int) {
            if (itemData == null || TextUtils.isEmpty(itemData.saltId) || itemData.getEditText() == null) {
                return
            }

            val bitmap = thumbBitmapMap[itemData.getFrameTime().toString() + ""]
            if (bitmap != null && !bitmap.isRecycled) {
                thumbImageView.setImageBitmap(bitmap)
            } else {
                thumbImageView.background = CutSameDesignDrawableFactory.createRectNormalDrawable(
                        Color.TRANSPARENT,
                        0x26FFFFFF,
                        0, SizeUtil.dp2px(2f))
            }

            changeViewStatus(itemData, pos)

            itemView.setGlobalDebounceOnClickListener {
                if (curSelectPos == adapterPosition) { //如果已经是选中态就点击编辑
                    itemListener?.clickEditItem(itemData, adapterPosition)
                } else {
                    itemListener?.selectItem(itemData, adapterPosition)
                }
            }

            numberTextView.text = "${pos+1}"
        }

        /**
         * view 状态变化
         * @param itemData
         * @param pos
         */
        private fun changeViewStatus(itemData: PlayerTextEditItemData, pos: Int) {
            if (curSelectPos == pos) {
                selectedView.visibility = View.VISIBLE
            } else {
                selectedView.visibility = View.GONE
            }

            if (!itemData.isValid) {
                textContentView.text = textContentView.context.resources.getString(R.string.cutsame_edit_tip_no_content)
            } else {
                textContentView.text = itemData.getEditText()
            }
        }

    }

}
