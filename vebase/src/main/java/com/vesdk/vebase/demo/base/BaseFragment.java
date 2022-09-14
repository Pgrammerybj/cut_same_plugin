package com.vesdk.vebase.demo.base;

import android.os.Bundle;
import androidx.annotation.Nullable;

/**
 *  on 2020-11-17
 */
abstract public class BaseFragment<T extends IPresenter> extends com.vesdk.vebase.fragment.BaseFragment implements IView {
    protected T mPresenter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setPresenter(T presenter) {
        assert presenter != null;
        mPresenter = presenter;
        mPresenter.attachView(this);
    }

    @Override
    public void onDestroy() {
        if (mPresenter != null) {
            mPresenter.detachView();
        }
        super.onDestroy();
    }
}
