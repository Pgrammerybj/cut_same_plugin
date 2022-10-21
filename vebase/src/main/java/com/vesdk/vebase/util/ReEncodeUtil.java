package com.vesdk.vebase.util;

import android.app.Activity;
import com.ss.android.medialib.FFMpegManager;
import com.ss.android.vesdk.VEUtils;
import com.vesdk.RecordInitHelper;
import com.vesdk.vebase.LogUtils;

import java.io.File;

/**
 * description :
 */
public class ReEncodeUtil {

    public static final String videoPath = RecordInitHelper.getApplicationContext().getExternalFilesDir(null) + File.separator + "duet_input.mp4";
    public static final String audioPath = RecordInitHelper.getApplicationContext().getExternalFilesDir(null) + File.separator + "duet_input.wav";

    public static void reEncodeVideo(final Activity context, final String path, final ReEncodeInterface callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                LogUtils.d("合拍视频 开始处理....");
                final FFMpegManager.RencodeParams params = new FFMpegManager.RencodeParams();
                //sdk内部对出入点做了判断,若outpoint-inpoint < minDurationInMs会报错，minDurationInMs默认3s，可自行控制，防止处理后的合拍视频为0或者时间很短的情况，没有意义
                params.minDurationInMs= 1000 ; //限制生成的视频不小于1s
                params.readfrom = path;
                params.saveto = videoPath;
                params.outputWav = audioPath;
                params.inpoint = 0;
                params.outpoint = VEUtils.getVideoFileInfo(path).duration ;
                params.screenWidth = 540;
                params.fullScreen = false;
                params.pos = 0;
                params.rotateAngle = 0;
                params.isCPUEncode = true;
                // 对合拍视频进行抽离出视频和音频 并且把音频文件从aac转成pcm，提升性能
                final int ret = FFMpegManager.getInstance().rencodeAndSplitFile(params);
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callBack != null) {
                            callBack.complete(ret, params.saveto, params.outputWav);
                        }
                    }
                });
            }
        }).start();
    }

    public interface ReEncodeInterface {
        void complete(int ret, String videoPath, String AudioPath);
    }
}
