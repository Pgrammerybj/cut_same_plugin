package com.cutsame.ui.utils;

import android.content.Context;
import android.content.SharedPreferences;

/*
 * sharedpreference工具类
 */
public class PrefUtils {
	public static SharedPreferences sp;

	public static final String key = "isDebug";

	public static SharedPreferences getSharedPre(Context ctx) {
		if (sp == null) {
			sp = ctx.getSharedPreferences("config_ve",
					Context.MODE_PRIVATE);
		}
		return sp;
	}

	public static void putBoolean(Context ctx, String key,
                                  boolean value) {
		SharedPreferences sp = getSharedPre(ctx);
		sp.edit().putBoolean(key, value).commit();
	}

	public static boolean getBoolean(Context ctx, String key,
                                     boolean defValue) {
		SharedPreferences sp = getSharedPre(ctx);
		return sp.getBoolean(key, defValue);
	}

	public static void putString(Context ctx, String key, String value) {
		SharedPreferences sp = getSharedPre(ctx);
		sp.edit().putString(key, value).commit();
	}

	public static String getString(Context ctx, String key,
                                   String defValue) {
		SharedPreferences sp = getSharedPre(ctx);
		return sp.getString(key, defValue);
	}

	public static void putInt(Context ctx, String key, int value) {
		SharedPreferences sp = getSharedPre(ctx);
		sp.edit().putInt(key, value).commit();
	}

	public static int getInt(Context ctx, String key, int defValue) {
		SharedPreferences sp = getSharedPre(ctx);
		return sp.getInt(key, defValue);
	}
	
	public static void removeString(Context ctx, String key) {
	    getSharedPre(ctx).edit().remove(key).commit();
	}


	public static void putFloat(Context ctx, String key, float value) {
		SharedPreferences sp = getSharedPre(ctx);
		sp.edit().putFloat(key, value).commit();

	}

	public static float getFloat(Context ctx, String key, float defValue) {
		SharedPreferences sp = getSharedPre(ctx);
		return sp.getFloat(key, defValue);

	}
}
