package com.cutsame.ui.template.view

import android.net.Uri
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.cutsame.solution.template.model.Cover
import com.cutsame.solution.template.model.TemplateItem
import com.cutsame.ui.R
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.template.viewmodel.TemplateNetPageModel
import com.cutsame.ui.utils.SizeUtil
import java.lang.ref.WeakReference
import kotlin.math.min

class TemplateRecyclerAdapter(
    private val viewModel: TemplateNetPageModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var itemClickListener: ItemClickListener? = null
    private val itemList = ArrayList<TemplateItem>()
    private var recyclerViewRef: WeakReference<RecyclerView>? = null
    var hasMore = true

    companion object {
        const val TYPE_ITEM = 1
        const val TYPE_FOOTER = 2
    }

    fun updateItems(items: List<TemplateItem>) {
        itemList.clear()
        val list = removeDuplicateItem(items)
        itemList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        return if (type == TYPE_ITEM) {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_template, parent, false)
            TemplateItemViewHolder(view)
        } else {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.rv_footer, parent, false)
            TemplateFooterViewHolder(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (!hasMore && position == itemCount - 1) {
            TYPE_FOOTER
        } else {
            TYPE_ITEM
        }
    }

    override fun getItemCount(): Int {
        return if (hasMore) {
            itemList.size
        } else {
            itemList.size + 1
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TemplateFooterViewHolder || holder !is TemplateItemViewHolder) {
            return
        }
        val item = itemList[position]
        item.apply {
            cover?.run {
                if (url.isEmpty()) {
                    holder.templateCoverIv.setImageBitmap(null)
                } else {
                    val requestOptions = RequestOptions().override(
                        holder.templateCoverIv.measuredWidth,
                        holder.templateCoverIv.measuredHeight
                    )
                    Glide.with(holder.templateCoverIv)
                        .load(Uri.parse(url))
                        .apply(requestOptions)
                        .into(holder.templateCoverIv)
                }
                resizeCoverDisplay(this, holder.templateCoverIv)
            }
            holder.templateTitleTv.text = getTemplateTitle(this)
            holder.segmentNumTv.text =
                holder.itemView.context.getString(R.string.cutsame_feed_fragment_num, fragmentCount)
        }

        holder.itemView.setGlobalDebounceOnClickListener {
            itemClickListener?.onItemClick(it, position)
        }

        if (position == itemCount - 1) {
            viewModel.loadFeedList()
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerViewRef = WeakReference(recyclerView)
    }

    override fun onViewAttachedToWindow(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (!hasMore && holder.layoutPosition == itemCount - 1) {
            val p =
                holder.itemView.layoutParams as androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams
            p.isFullSpan = true
        }
    }

    //去重逻辑
    private fun removeDuplicateItem(list: List<TemplateItem>): List<TemplateItem> {
        val compareCount = min(itemList.size, 5)
        val start = itemList.size - compareCount
        val result = ArrayList<TemplateItem>()
        for (item in list) {
            var duplicate = false
            for (index in start until itemList.size) {
                if (item.id == itemList[index].id) {
                    duplicate = true
                    break
                }
            }
            if (!duplicate) {
                result.add(item)
            }
        }
        return result
    }

    private var itemWidth = 0
    private fun resizeCoverDisplay(cover: Cover, coverView: View) {
        if (cover.width == 0 || cover.height == 0) {
            return
        }
        if (itemWidth <= 0) {
            val screenW = SizeUtil.getScreenWidth(coverView.context)
            itemWidth =
                (screenW / 2 - coverView.context.resources.getDimension(R.dimen.template_feed_margin) * 4).toInt()
        }
        val lp = coverView.layoutParams
        lp.width = itemWidth
        lp.height = (lp.width.toFloat() * cover.height / cover.width).toInt()
        coverView.layoutParams = lp
    }

    fun setItemClickListener(listener: ItemClickListener) {
        itemClickListener = listener
    }

    private fun getTemplateTitle(templateItem: TemplateItem): String {
        var titleDesc = if (TextUtils.isEmpty(templateItem.shortTitle)) {
            templateItem.title
        } else {
            templateItem.shortTitle
        }
        return titleDesc
    }

    interface ItemClickListener {
        fun onItemClick(view: View, position: Int)
    }
}

class TemplateItemViewHolder(itemView: View) :
    androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
    val templateCoverIv: ImageView = itemView.findViewById(R.id.templateCoverIv)
    val templateTitleTv: TextView = itemView.findViewById(R.id.templateTitleTv)
    val segmentNumTv: TextView = itemView.findViewById(R.id.segmentNumTv)
}

class TemplateFooterViewHolder(itemView: View) :
    androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView)
