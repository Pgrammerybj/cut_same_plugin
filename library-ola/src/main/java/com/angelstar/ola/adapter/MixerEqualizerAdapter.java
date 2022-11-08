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
 * @Author：yangbaojiang
 * @Date: 2022/11/8 14:46
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 调音台里面的调压器列表
 */
public class MixerEqualizerAdapter extends RecyclerView.Adapter<MixerEqualizerAdapter.ViewHolder> {

    private final Context mContext;
    private final List<AudioMixingEntry.EqualizerEffects> mEqualizerList;
    private int activePosition = 0;

    public MixerEqualizerAdapter(Context context, List<AudioMixingEntry.EqualizerEffects> equalizerList, int activePosition) {
        this.mEqualizerList = equalizerList;
        this.mContext = context;
        //均衡器是从index10开始
        this.activePosition = activePosition - 10;
    }

    public void setActivePosition(int activePosition) {
        this.activePosition = activePosition;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MixerEqualizerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_equalizer_item, parent, false);
        return new MixerEqualizerAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MixerEqualizerAdapter.ViewHolder holder, int position) {
        AudioMixingEntry.EqualizerEffects itemEntry = mEqualizerList.get(position);
        RequestOptions options = RequestOptions.bitmapTransform(new RoundedCorners(SizeUtil.INSTANCE.dp2px(8)));
        //用来加载网络
        Glide.with(this.mContext).load(activePosition == position ? itemEntry.getIcon1() : itemEntry.getIcon()).apply(options).into(holder.itemBgImage);
        holder.itemName.setText(itemEntry.getName());
        holder.itemView.setOnClickListener(v -> {
                    setActivePosition(position);
                    onItemClickListener.onItemClick(holder.itemView, itemEntry.getIndex(), position);
                }
        );
    }

    @Override
    public int getItemCount() {
        return mEqualizerList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemBgImage;
        TextView itemName;

        public ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.tv_equalizer_item_name);
            itemBgImage = itemView.findViewById(R.id.iv_equalizer_icon);
        }
    }

    private OnMixerItemClickListener onItemClickListener;//声明一下这个接口

    //提供setter方法
    public void setOnItemClickListener(OnMixerItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
}
