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
import com.angelstar.ola.entity.MixerItemEntry;
import com.angelstar.ola.interfaces.OnMixerItemClickListener;
import com.angelstar.ola.utils.SizeUtil;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class MixerRecyclerViewAdapter extends RecyclerView.Adapter<MixerRecyclerViewAdapter.ViewHolder> {

    private Context mContext;
    private List<MixerItemEntry> mixerList;
    public static final int DEFAULT_ITEM = 0;
    public static final int MIXER_ITEM = 1;

    String url_test = "https://img2.baidu.com/it/u=552452605,2067380431&fm=253&fmt=auto&app=138&f=JPEG?w=160&h=100";

    public MixerRecyclerViewAdapter(Context context, List<MixerItemEntry> mixerList) {
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
        MixerItemEntry itemEntry = mixerList.get(position);
        if (position == 0) {
            holder.itemImage.setImageResource(R.mipmap.mixer_item_test);
            holder.itemTitle.setText("调音");
        } else {
//            holder.itemImage.setImageResource(R.mipmap.mixer_item_test);

            RequestOptions options = new RequestOptions()
                    .placeholder(R.mipmap.mixer_item_test)
                    .bitmapTransform(new RoundedCorners(SizeUtil.INSTANCE.dp2px(8)));

            //用来加载网络
            Glide.with(this.mContext).load(url_test).apply(options).into(holder.itemImage);
            holder.itemTitle.setText(itemEntry.getMixerTitle());
        }
        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(holder.itemView, itemEntry, position));
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

