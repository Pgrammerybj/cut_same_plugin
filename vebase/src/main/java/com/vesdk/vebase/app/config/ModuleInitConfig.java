package com.vesdk.vebase.app.config;

import android.app.Application;

import androidx.annotation.Nullable;

import com.vesdk.vebase.app.IModuleInit;


/**
 *  on 2018/6/21 0021.
 * 作为组件生命周期初始化的配置类，通过反射机制，动态调用每个组件初始化逻辑
 */

public class ModuleInitConfig {
    //内部类，在装载该内部类时才会去创建单例对象
    private static class SingletonHolder {
        public static ModuleInitConfig instance = new ModuleInitConfig();
    }

    public static ModuleInitConfig getInstance() {
        return SingletonHolder.instance;
    }

    private ModuleInitConfig() {}

    //初始化组件-靠前
    public void initModuleAhead(@Nullable Application application) {
        for (String moduleInitName : ModuleInitName.initModuleNames) {
            try {
                Class<?> clazz = Class.forName(moduleInitName);
                IModuleInit init = (IModuleInit) clazz.newInstance();
                //调用初始化方法
                init.onInitAhead(application);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //初始化组件-靠后
    public void initModuleLow(@Nullable Application application) {
        for (String moduleInitName : ModuleInitName.initModuleNames) {
            try {
                Class<?> clazz = Class.forName(moduleInitName);
                IModuleInit init = (IModuleInit) clazz.newInstance();
                //调用初始化方法
                init.onInitLow(application);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
