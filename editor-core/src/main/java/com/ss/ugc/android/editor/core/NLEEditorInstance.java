package com.ss.ugc.android.editor.core;

import java.util.HashMap;
import java.util.Set;

/**
 * @Author：yangbaojiang
 * @Date: 2022/11/29 14:26
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
public class NLEEditorInstance {

    public static String KEY_NLE_EDITOR_CONTEXT = "key_nle_Editor_Context";

    private HashMap<String, Object> singleInstanceMap;

    private volatile static NLEEditorInstance nleEditorInstance;


    public static NLEEditorInstance mInstance() {
        if (null == nleEditorInstance) {
            synchronized (NLEEditorInstance.class) {
                nleEditorInstance = new NLEEditorInstance();
            }
        }
        return nleEditorInstance;
    }


    private NLEEditorInstance() {
        singleInstanceMap = new HashMap<>();
    }

    public void put(String key, NLEEditorContext value) {
        if (null != singleInstanceMap) {
            singleInstanceMap.put(key, value);
        }
    }

    public Object get(String key) {
        if (null != singleInstanceMap) {
            return singleInstanceMap.get(key);
        }
        return null;
    }

    public void removeAll() {
        if (null != singleInstanceMap) {
            Set<String> keys = singleInstanceMap.keySet();
            for (String key : keys) {
                singleInstanceMap.remove(key);
            }
        }
    }
}
