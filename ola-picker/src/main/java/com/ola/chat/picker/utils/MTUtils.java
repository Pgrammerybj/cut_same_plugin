package com.ola.chat.picker.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/21 14:21
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
public class MTUtils {

    private static volatile int STATUS_BAR_RESULT = 0;

    /**
     * 复制文字到剪贴板
     *
     * @author mashengchao 2012-5-2 下午8:19:23
     */
    public static void copyText(Context context, CharSequence msg) {
        ClipboardManager cm = (ClipboardManager) context
                .getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(msg);
    }

    /**
     * 获取剪贴板中的文字
     *
     * @author mashengchao 2012-5-2 下午8:21:04
     */
    public static CharSequence getPasteText(Context context) {
        ClipboardManager cm = (ClipboardManager) context
                .getSystemService(Context.CLIPBOARD_SERVICE);
        return cm.getText();
    }

    /**
     * 取得URL的后缀
     *
     * @return 后缀
     */
    public static String getUrlSuffix(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return "";
        }
        String strUri = uri;
        int index = strUri.indexOf("?");
        if (index >= 0) {
            strUri = strUri.substring(0, index);
        }
        int lastIndex = strUri.lastIndexOf(".");
        String suffix = strUri.substring(lastIndex + 1);

        return suffix.toLowerCase();
    }

    /**
     * 判断是否是飞行模式
     */
    public static boolean isAirplaneMode(Context ctx) {
        try {
            return 1 == Settings.System.getInt(ctx.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取Manifest中activity的label
     *
     */
    public static int getActivityLabelRes(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        ActivityInfo info = null;
        try {
            info = pm.getActivityInfo(activity.getComponentName(), PackageManager.GET_META_DATA);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int res = 0;
        if (info != null) {
            res = info.labelRes;
        }
        return res;
    }

    public static boolean isInteger(double value) {
        return value == Math.floor(value) && !Double.isInfinite(value);
    }

    public static String getLatLngStr(double lat, double lng) {
        return String.valueOf(lat) + "," + String.valueOf(lng);
    }

    public static String getLatLngStr(Location location) {
        return getLatLngStr(location.getLatitude(), location.getLongitude());
    }

    /**
     * Number正则表达式
     */
    public static final String PATTERN_NUMERIC = "[0-9]*";

    /**
     * 字符串是否是数字
     *
     * @return boolean
     */
    public static boolean isNumeric(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        boolean result = false;
        Pattern pattern = Pattern.compile(PATTERN_NUMERIC);
        Matcher matcher = pattern.matcher(str);
        result = matcher.matches();
        return result;
    }

    public static final String PATTERN_COLOR = "^#[0-9a-fA-F]{6}$";
    public static final String PATTERN_COLOR_EIGHT = "^#[0-9a-fA-F]{8}$";

    public static boolean isColorValidForSix(String colorStr) {
        if (TextUtils.isEmpty(colorStr)) {
            return false;
        }
        Pattern pattern = Pattern.compile(PATTERN_COLOR);
        Matcher matcher = pattern.matcher(colorStr);
        return matcher.matches();
    }

    public static boolean isColorValidForEight(String colorStr) {
        if (TextUtils.isEmpty(colorStr)) {
            return false;
        }
        Pattern pattern = Pattern.compile(PATTERN_COLOR_EIGHT);
        Matcher matcher = pattern.matcher(colorStr);
        return matcher.matches();
    }

    public static int getStatusBarHeight(Context context) {
        if (STATUS_BAR_RESULT > 0) {
            return STATUS_BAR_RESULT;
        }
        int result = 0;
        try {
            Resources resources = Resources.getSystem();
            int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = resources.getDimensionPixelSize(resourceId);
                STATUS_BAR_RESULT = result;
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public static void makeStatusBarTransparent(Activity activity) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int option = window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            window.getDecorView().setSystemUiVisibility(option);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * 程序是否在前台运行
     */
    public static boolean isAppOnForeground(Context context) {
        // Returns a list of application processes that are running on the
        // device
        // 某些手机,比如华为, getRunningAppProcesses会crash
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(
                    Context.ACTIVITY_SERVICE);
            String packageName = context.getPackageName();

            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                    .getRunningAppProcesses();
            if (appProcesses == null) {
                return false;
            }

            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                // The name of the process that this object is associated with.
                if (appProcess.processName.equals(packageName)
                        && appProcess.importance
                        == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return true;
                }
            }

        } catch (Exception ignored) {

        }

        return false;
    }
}
