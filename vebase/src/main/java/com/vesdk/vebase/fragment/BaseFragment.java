package com.vesdk.vebase.fragment;

import static com.vesdk.vebase.fragment.RiggerHelper.getRiggerInstance;
import static com.vesdk.vebase.fragment.RiggerHelper.getRiggerMethod;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import  androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import  androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jkb.fragment.rigger.annotation.Puppet;
import com.jkb.fragment.rigger.rigger.Rigger;

import java.lang.reflect.Method;

/**
 * Base fragment.
 * @since Nov 22,2017
 */
@Puppet
public abstract class BaseFragment extends Fragment {

    protected static final String BUNDLE_KEY = "/bundle/key";
    protected View mContentView;
    protected Context mContext;

    public BaseFragment() {
        try {
            Method onResumeFragments = getRiggerMethod("onPuppetConstructor", Object.class);
            onResumeFragments.invoke(getRiggerInstance(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        try {
            Method onCreate = getRiggerMethod("onCreate", Object.class, Bundle.class);
            onCreate.invoke(getRiggerInstance(), this, savedInstanceState);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        try {
            Method onAttach = getRiggerMethod("onAttach", Object.class, Context.class);
            onAttach.invoke(getRiggerInstance(), this, context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mContentView = inflater.inflate(getContentView(), container, false);
        try {
            Method onCreate = getRiggerMethod("onCreateView", Object.class, LayoutInflater.class, ViewGroup.class,
                    Bundle.class, View.class);
            onCreate.invoke(getRiggerInstance(), this, inflater, container, savedInstanceState, mContentView);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        mContentView.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.white));
        return mContentView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            Method onCreate = getRiggerMethod("onViewCreated", Object.class, View.class, Bundle.class);
            onCreate.invoke(getRiggerInstance(), this, view, savedInstanceState);
        } catch (Exception e) {
            e.printStackTrace();
        }
        init(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            Method onPause = getRiggerMethod("onResume", Object.class);
            onPause.invoke(getRiggerInstance(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            Method onSaveInstanceState = getRiggerMethod("onSaveInstanceState", Object.class, Bundle.class);
            onSaveInstanceState.invoke(getRiggerInstance(), this, outState);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            Method onDestroy = getRiggerMethod("onDestroy", Object.class);
            onDestroy.invoke(getRiggerInstance(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            Method onDetach = getRiggerMethod("onDetach", Object.class);
            onDetach.invoke(getRiggerInstance(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        try {
            Method setUserVisibleHint = getRiggerMethod("setUserVisibleHint", Object.class, boolean.class);
            setUserVisibleHint.invoke(getRiggerInstance(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @LayoutRes
    protected int getContentView() {
        return 0;
    }

    protected void init(Bundle savedInstanceState) {

    }

    protected  <T extends View> T  findViewById(@IdRes int id) {
        return mContentView.findViewById(id);
    }

    protected void pop() {
        Rigger.getRigger(this).close();
    }

    protected Fragment startFragment(Class<? extends Fragment> fragmentClass) {
        return startFragment(fragmentClass, null);
    }

    protected Fragment startFragment(Class<? extends Fragment> fragmentClass, Bundle args) {
        Fragment fragment = Fragment.instantiate(getContext(), fragmentClass.getCanonicalName(), args);
        Rigger.getRigger(this.getActivity()).startFragment(fragment);
        return fragment;
    }

    protected Fragment startFragment(Fragment fragment) {
        Rigger.getRigger(this.getActivity()).startFragment(fragment);
        return fragment;
    }

    public boolean onRiggerBackPressed() {
        //需要拦截，则返回true，否则返回false
        return false;
    }

}
