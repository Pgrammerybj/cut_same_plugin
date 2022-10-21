package com.vesdk.vebase.old.assist;

import android.view.View;
import android.widget.TextView;

import  androidx.annotation.NonNull;

import com.bytedance.android.winnow.WinnowHolder;

import com.vesdk.vebase.R;
import com.vesdk.vebase.old.model.DataContainer;

/**
 * time : 2020/5/9
 *
 * description :
 * 功能 item 条目展示
 */
public class FunctionHolder extends WinnowHolder<DataContainer.FunctionItem> {


    private final TextView functionName;

    public FunctionHolder(View itemView) {
        super(itemView);
        functionName = itemView.findViewById(R.id.tv);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.function_item;
    }

    @Override
    protected void onBindData(@NonNull DataContainer.FunctionItem data) {
        functionName.setText(data.name);
    }
}
