package com.angelstar.cutsame.cut_same_plugin;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.angelstar.ola.OlaTemplateFeedActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * CutSamePlugin
 */
public class CutSamePlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private Activity activity;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "cut_same_plugin");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("startCutSamePage")) {
            //跳转到目标页面
            if (null != activity) {

                ArrayList<String> videoList = new ArrayList<>();
                videoList.add("/storage/emulated/0/DCIM/baiyueguangzhushazhi_7002448424021524488.mp4");

                Intent intent = new Intent(activity, OlaTemplateFeedActivity.class);
                intent.putExtra("extra_key_from_type", 1);
                intent.putStringArrayListExtra("extra_video_paths", videoList);
                intent.putExtra("extra_media_type", 3);//1：图片、3视频
                intent.setPackage(activity.getPackageName());
                activity.startActivity(intent);
            }
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull @NotNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull @NotNull ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }
}
