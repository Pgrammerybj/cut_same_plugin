package com.vesdk.vebase.fragment;

import com.jkb.fragment.rigger.rigger.Rigger;

import java.lang.reflect.Method;

public class RiggerHelper {

    /**
     * Returns the instance of Rigger class by reflect.
     */
   public static Rigger getRiggerInstance() throws Exception {
        Class<?> riggerClazz = Class.forName(Rigger.class.getName());
        Method getInstance = riggerClazz.getDeclaredMethod("getInstance");
        getInstance.setAccessible(true);
        return (Rigger) getInstance.invoke(null);
    }

    /**
     * Returns the method object of Rigger by reflect.
     */
   public static Method getRiggerMethod(String methodName, Class<?>... params) throws Exception {
        Rigger rigger = getRiggerInstance();
        Class<? extends Rigger> clazz = rigger.getClass();
        Method method = clazz.getDeclaredMethod(methodName, params);
        method.setAccessible(true);
        return method;
    }

}
