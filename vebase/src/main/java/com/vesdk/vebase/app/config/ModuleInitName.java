package com.vesdk.vebase.app.config;

/**
 *  on 2018.6.21 0021.
 * 组件生命周期反射类名管理，在这里注册需要初始化的组件，通过反射动态调用各个组件的初始化方法
 * 注意：以下模块中初始化的Module类不能被混淆
 */

public class ModuleInitName {

    private static final String BaseInit = "com.vesdk.vebase.app.BaseModuleInit";
    // 拍摄模块
    private static final String RecorderInit = "com.vesdk.verecorder.record.RecorderModuleInit";
    // 编辑模块
    private static final String EditorInit = "com.vesdk.veeditor.edit.EditorModuleInit";


    public static String[] initModuleNames = {BaseInit,RecorderInit, EditorInit};
}
