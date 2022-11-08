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

/**
 * 调音台里面的混响面板
 */
public class MixerReverbAdapter extends RecyclerView.Adapter<MixerReverbAdapter.ViewHolder> {

    private final Context mContext;
    private final List<AudioMixingEntry.ReverbList> mReverbList;
    private int activePosition = 0;

    public MixerReverbAdapter(Context context, List<AudioMixingEntry.ReverbList> reverbList, int activePosition) {
        this.mReverbList = reverbList;
        this.mContext = context;
        this.activePosition = activePosition-1;
    }

    public void setActivePosition(int activePosition) {
        this.activePosition = activePosition;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_reverb_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioMixingEntry.ReverbList itemEntry = mReverbList.get(position);
        RequestOptions options = RequestOptions.bitmapTransform(new RoundedCorners(SizeUtil.INSTANCE.dp2px(8)));
        //用来加载网络
        Glide.with(this.mContext).load(itemEntry.getIcon()).apply(options).into(holder.itemImage);
        holder.itemName.setText(itemEntry.getName());
        holder.itemSelect.setVisibility(position == activePosition ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> {
            setActivePosition(position);
            onItemClickListener.onItemClick(holder.itemView, itemEntry.getIndex(), position);
        });

    }

    @Override
    public int getItemCount() {
        return mReverbList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        ImageView itemSelect;
        TextView itemName;

        public ViewHolder(View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.iv_reverb_item_img);
            itemSelect = itemView.findViewById(R.id.iv_reverb_item_select);
            itemName = itemView.findViewById(R.id.tv_reverb_item_name);
        }
    }


    private OnMixerItemClickListener onItemClickListener;//声明一下这个接口

    //提供setter方法
    public void setOnItemClickListener(OnMixerItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}

