package com.cutsame.ui.cut.lyrics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.cutsame.ui.R;

import java.util.List;

public class LyricsFontRecyclerViewAdapter extends RecyclerView.Adapter<LyricsFontRecyclerViewAdapter.ViewHolder> {

    private final Context mContext;
    private final List<FontItemEntry> fontItemList;

    public LyricsFontRecyclerViewAdapter(Context context, List<FontItemEntry> fontItemList) {
        this.fontItemList = fontItemList;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_lyrics_font_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FontItemEntry itemEntry = fontItemList.get(position);
        RequestOptions options = RequestOptions.bitmapTransform(new CircleCrop());
        //用来加载网络
        Glide.with(this.mContext).load(itemEntry.getFontCover()).apply(options).into(holder.ivFontCover);
        holder.tvFontName.setText(itemEntry.getFontName());
        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(holder.tvFontName, position));
        holder.unDownloadMask.setVisibility(itemEntry.isDownload ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return fontItemList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFontCover;
        ImageView ivDownloadFont;
        TextView tvFontName;
        View selectedMask, unDownloadMask;

        public ViewHolder(View itemView) {
            super(itemView);
            ivFontCover = itemView.findViewById(R.id.ivFontCover);
            ivDownloadFont = itemView.findViewById(R.id.ivDownloadFont);
            tvFontName = itemView.findViewById(R.id.tvFontName);
            selectedMask = itemView.findViewById(R.id.selectedMask);
            unDownloadMask = itemView.findViewById(R.id.unDownloadMask);
        }
    }

    private OnLyricsItemClickListener onItemClickListener;//声明一下这个接口

    //提供setter方法
    public void setOnItemClickListener(OnLyricsItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}

