package com.ss.ugc.android.editor.picker.album.view.adapter;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.ss.ugc.android.editor.base.R;
import com.ss.ugc.android.editor.base.utils.SizeUtil;
import com.ss.ugc.android.editor.picker.data.model.MediaItem;
import com.ss.ugc.android.editor.picker.data.model.MediaType;
import com.ss.ugc.android.editor.picker.selector.viewmodel.MaterialSelectModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageSelectedRVAdapter extends RecyclerView.Adapter<ImageSelectedRVAdapter.ViewHolder> {

    private final Context mContext;
    private final MaterialSelectModel materialSelectModel;
    private List<MediaItem> anchorList;
    private int lastSelectedListSize = 0;
    private final MediaItem mediaItem = new MediaItem(MediaType.IMAGE, "", "", new Uri.Builder().build(), -1, "", -1, 0, 0, 0, (long) 0.8);

    public ImageSelectedRVAdapter(Context context, int templateAnchor, MaterialSelectModel materialSelectModel) {
        this.mContext = context;
        this.materialSelectModel = materialSelectModel;
        //先根据模版的锚点位数来创建默认的MediaItem个数，用来和selectedList来merge
        createDefaultAnchor(templateAnchor);
    }

    /**
     * 生成默认的锚点MediaItem
     *
     * @param templateAnchor 锚点坑位数
     */
    private void createDefaultAnchor(int templateAnchor) {
        anchorList = new ArrayList<>();
        for (int i = 0; i < templateAnchor; i++) {
            anchorList.add(mediaItem);
        }
    }

    /**
     * TODO:这一处Merge逻辑需要优化
     *
     * @param updateSelectedList 用户选中的素材
     */
    public void updateSelectedList(List<MediaItem> updateSelectedList) {
        if (updateSelectedList != null && updateSelectedList.size() > 0) {
            //将updateSelectedList中的元素替换anchorList
            for (int i = 0; i < Math.max(updateSelectedList.size(), lastSelectedListSize); i++) {
                if (i >= updateSelectedList.size()) {
                    anchorList.set(i, mediaItem);
                } else {
                    anchorList.set(i, updateSelectedList.get(i));
                }
            }
            notifyItemRangeChanged(0, Math.max(updateSelectedList.size(), lastSelectedListSize));
            lastSelectedListSize = updateSelectedList.size();
        } else {
            //恢复默认
            for (int i = 0; i < lastSelectedListSize; i++) {
                anchorList.set(i, mediaItem);
            }
            notifyItemRangeChanged(0, lastSelectedListSize);
            lastSelectedListSize = 0;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_picker_selected_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final MediaItem itemEntry = anchorList.get(position);

        RequestOptions options = RequestOptions
                .bitmapTransform(new RoundedCorners(SizeUtil.INSTANCE.dp2px(8F)))
                .placeholder(R.drawable.default_image);

        if (itemEntry == null) return;

        boolean showDefault = TextUtils.isEmpty(itemEntry.getPath());
        holder.ivSelectedCover.setVisibility(showDefault ? View.GONE : View.VISIBLE);
        holder.ivDeleteSelected.setVisibility(showDefault ? View.GONE : View.VISIBLE);
        holder.tvReservedSpaceTime.setVisibility(showDefault ? View.VISIBLE : View.GONE);
        holder.tvReservedSpaceTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //互斥逻辑继续要优化，当前Item点击高亮的时候，其他的需要隐藏
                holder.tvReservedSpaceTime.setSelected(!holder.tvReservedSpaceTime.isSelected());
            }
        });

        holder.ivDeleteSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                materialSelectModel.changeSelectState(itemEntry);
            }
        });

        if (!showDefault) {
            File file = new File(itemEntry.getPath());
            Glide.with(this.mContext).load(file).apply(options).into(holder.ivSelectedCover);
        } else {
            holder.tvReservedSpaceTime.setText(String.format(mContext.getString(R.string.ck_picker_anchor_time), itemEntry.getDuration()));
        }
    }

    @Override
    public int getItemCount() {
        return anchorList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDeleteSelected, ivSelectedCover;
        TextView tvReservedSpaceTime;

        public ViewHolder(View itemView) {
            super(itemView);
            ivSelectedCover = itemView.findViewById(R.id.iv_selected_cover);
            ivDeleteSelected = itemView.findViewById(R.id.iv_delete_selected);
            tvReservedSpaceTime = itemView.findViewById(R.id.tv_reserved_space_time);
        }
    }
}

