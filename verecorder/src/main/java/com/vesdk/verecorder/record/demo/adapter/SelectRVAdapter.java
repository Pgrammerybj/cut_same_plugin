package com.vesdk.verecorder.record.demo.adapter;

import androidx.recyclerview.widget.RecyclerView;

abstract public class SelectRVAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
    protected int mSelect = -1 ;

    public void setSelect(int select) {
        if (mSelect != select) {
            int oldSelect = mSelect;
            mSelect = select;
            notifyItemChanged(oldSelect);
            notifyItemChanged(select);
        }
    }

    public int getSelect() {
        return mSelect;
    }
}
