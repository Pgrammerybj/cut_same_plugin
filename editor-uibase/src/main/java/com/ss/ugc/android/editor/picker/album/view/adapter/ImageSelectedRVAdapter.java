package com.ss.ugc.android.editor.picker.album.view.adapter;

import android.content.Context;
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ImageSelectedRVAdapter extends RecyclerView.Adapter<ImageSelectedRVAdapter.ViewHolder> {

    private final Context mContext;
    private List<MediaItem> selectedList;
    String url_test = "https://img2.baidu.com/it/u=552452605,2067380431&fm=253&fmt=auto&app=138&f=JPEG?w=160&h=100";

    public ImageSelectedRVAdapter(Context context, List<MediaItem> selectedList) {
        this.selectedList = selectedList;
        this.mContext = context;
    }

    public void updateSelectedList(List<MediaItem> updateSelectedList) {
        if (updateSelectedList != null) {
            updateSelectedList.size();
            selectedList = updateSelectedList;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_picker_selected_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItem itemEntry = selectedList.get(position);
        RequestOptions options = RequestOptions
                .bitmapTransform(new RoundedCorners(SizeUtil.INSTANCE.dp2px(8F)))
                .placeholder(R.drawable.default_image);

        //用来加载网络
        Glide.with(this.mContext).load(url_test).apply(options).into(holder.ivSelectedCover);
        holder.tvReservedSpaceTime.setText(String.valueOf(itemEntry.getDuration()));
    }

    @Override
    public int getItemCount() {
        return selectedList.size();
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

