package com.angelstar.ola.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.angelstar.ola.R;
import com.angelstar.ola.entity.AudioMixingEntry;
import com.angelstar.ola.interfaces.OnMixerItemClickListener;
import com.angelstar.ola.utils.SizeUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class MixerRecyclerViewAdapter extends RecyclerView.Adapter<MixerRecyclerViewAdapter.ViewHolder> {

    private final Context mContext;
    private List<AudioMixingEntry.BoardEffects> mixerList;
    public static final int DEFAULT_ITEM = 0;
    public static final int MIXER_ITEM = 1;

    public MixerRecyclerViewAdapter(Context context, List<AudioMixingEntry.BoardEffects> mixerList) {
        mixerList.add(0,new AudioMixingEntry.BoardEffects());
        this.mixerList = mixerList;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == DEFAULT_ITEM) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_default_item, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_mixer_item, parent, false);
        }
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioMixingEntry.BoardEffects itemEntry = mixerList.get(position);
        if (position == 0) {
            holder.itemImage.setImageResource(R.mipmap.song_edit_tuner_bg);
            holder.itemTitle.setText("调音");
        } else {
            RequestOptions options = RequestOptions.bitmapTransform(new RoundedCorners(SizeUtil.INSTANCE.dp2px(8)));
            //用来加载网络
            Glide.with(this.mContext).load(itemEntry.getIcon()).apply(options).into(holder.itemImage);
            holder.itemTitle.setText(itemEntry.getName());
        }
        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(holder.itemView, itemEntry.getIndex(), position));
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return DEFAULT_ITEM;
        } else {
            return MIXER_ITEM;
        }
    }

    @Override
    public int getItemCount() {
        return mixerList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        ImageView itemLabel;
        TextView itemTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.iv_mixer_item_img);
            itemLabel = itemView.findViewById(R.id.iv_mixer_item_label);
            itemTitle = itemView.findViewById(R.id.tv_mixer_item_title);
        }
    }

    private OnMixerItemClickListener onItemClickListener;//声明一下这个接口

    //提供setter方法
    public void setOnItemClickListener(OnMixerItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}

