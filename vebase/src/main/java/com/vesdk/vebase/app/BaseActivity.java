package com.vesdk.vebase.app;

import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import  androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import com.jkb.fragment.rigger.rigger.Rigger;
import com.vesdk.vebase.demo.base.IPresenter;
import com.vesdk.vebase.demo.base.IView;

import java.lang.reflect.Method;

import static com.vesdk.vebase.fragment.RiggerHelper.getRiggerInstance;
import static com.vesdk.vebase.fragment.RiggerHelper.getRiggerMethod;

/**
 * time : 2020/12/20
 *
 * description :
 */
abstract public class BaseActivity<T extends IPresenter> extends AppCompatActivity implements IView {
    protected T mPresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Method onPuppetConstructor = getRiggerMethod("onPuppetConstructor", Object.class);
            onPuppetConstructor.invoke(getRiggerInstance(), this);
            Method onCreate = getRiggerMethod("onCreate", Object.class, Bundle.class);
            onCreate.invoke(getRiggerInstance(), this, savedInstanceState);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        try {
            Method onResumeFragments = getRiggerMethod("onResumeFragments", Object.class);
            onResumeFragments.invoke(getRiggerInstance(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            Method onPause = getRiggerMethod("onPause", Object.class);
            onPause.invoke(getRiggerInstance(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        try {
            Method onSaveInstanceState = getRiggerMethod("onSaveInstanceState", Object.class, Bundle.class);
            onSaveInstanceState.invoke(getRiggerInstance(), this, outState);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected void setPresenter(T presenter) {
        assert presenter != null;
        mPresenter = presenter;
        mPresenter.attachView(this);
    }


    @Override
    protected void onDestroy() {
        if (mPresenter != null) {
            mPresenter.detachView();
        }
        super.onDestroy();
        try {
            Method onDestroy = getRiggerMethod("onDestroy", Object.class);
            onDestroy.invoke(getRiggerInstance(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            Method onBackPressed = getRiggerMethod("onBackPressed", Object.class);
            onBackPressed.invoke(getRiggerInstance(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Fragment startFragment(Class<? extends Fragment> fragmentClass) {
        return startFragment(fragmentClass, null);
    }

    protected Fragment startFragment(Class<? extends Fragment> fragmentClass, Bundle args) {
        Fragment fragment = Fragment.instantiate(this, fragmentClass.getCanonicalName(), args);
        try {
            Method  onResumeFragments = getRiggerMethod("onPuppetConstructor", Object.class);
            onResumeFragments.invoke(getRiggerInstance(), fragment);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Rigger.getRigger(this).startFragment(fragment);
        return fragment;
    }

    protected Fragment startFragment(Fragment fragment) {
        try {
            Method  onResumeFragments = getRiggerMethod("onPuppetConstructor", Object.class);
            onResumeFragments.invoke(getRiggerInstance(), fragment);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Rigger.getRigger(this).startFragment(fragment);
        return fragment;
    }

    @Override
    public Context getContext() {
        return this.getContext();
    }
}