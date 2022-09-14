package com.vesdk.verecorder.record.demo;

import android.text.TextUtils;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.ss.android.vesdk.VEAudioEncodeSettings;
import com.ss.android.vesdk.VECameraCapture;
import com.ss.android.vesdk.VECameraSettings;
import com.ss.android.vesdk.VEConfigCenter;
import com.ss.android.vesdk.VEConfigKeys;
import com.ss.android.vesdk.VEDuetSettings;
import com.ss.android.vesdk.VEInfo;
import com.ss.android.vesdk.VEListener;
import com.ss.android.vesdk.VEPreviewSettings;
import com.ss.android.vesdk.VERecorder;
import com.ss.android.vesdk.VEResult;
import com.ss.android.vesdk.VESDK;
import com.ss.android.vesdk.VESize;
import com.ss.android.vesdk.VEVideoEncodeSettings;
import com.vesdk.vebase.LogUtils;
import com.vesdk.vebase.ToastUtils;
import com.vesdk.vebase.demo.model.ComposerNode;
import com.vesdk.vebase.demo.present.contract.PreviewContract;
import com.vesdk.vebase.resource.ResourceHelper;
import com.vesdk.vebase.resource.ResourceItem;
import com.vesdk.verecorder.record.demo.fragment.PreviewFragment;
import com.vesdk.verecorder.record.preview.model.PreviewConfig;
import com.vesdk.verecorder.record.preview.viewmodel.PreviewModel;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 *  on 2019-07-20 17:30
 */
public class PreviewPresenter extends PreviewContract.Presenter {

    private String currentFilter;
    private float filterValue = 0f ;

    private String[] stickerPaths = new String[1];
    private boolean stickerSelected = false ;

    private VECameraCapture capture;
    private VERecorder recorder;
    private Surface surface;
    int surfaceWidth;
    int surfaceHeight;

    private boolean isDuet ;
    private String duetVideoPath ;
    private String duetAudioPath ;

    private PreviewModel previewModel;

    public PreviewPresenter(boolean isDuet, String videoPath, String audioPath) {
        this.isDuet = isDuet ;
        this.duetVideoPath = videoPath ;
        this.duetAudioPath = audioPath ;
//        previewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getView().getContext().ap).get(PreviewModel.class);
    }


    @Override
    public VERecorder getRecorder() {
        return recorder;
    }

    @Override
    public VECameraCapture getCapture() {
        return capture;
    }


    /**
     * recorder初始化
     *
     * @param surfaceView
     */
    @Override
    public void initRecorder(final SurfaceView surfaceView) {
        final PreviewConfig previewConfig = getPreviewModel().getPreviewConfig();
        //开启新的拍摄接口
        if (previewConfig != null) {
            VESDK.enableGLES3(previewConfig.getEnableGl3());
        }
        // record 相关 拍摄相关的还是在java层调用
        VESDK.enableNewRecorder(true);
        if (previewConfig != null) {
            VESDK.enableRefactorRecorder(previewConfig.getEnableRefactorRecorder());
            if (previewConfig.getEnableRefactorRecorder()) {
                VESDK.enableAudioCapture(true);
            }
        }
        int[] size = {720, 1280};
        //Camera的配置
        VECameraSettings cameraSettings = new VECameraSettings.Builder()
                .setCameraType(VECameraSettings.CAMERA_TYPE.TYPE1)  //使用Camera1
                .setCameraFacing(VECameraSettings.CAMERA_FACING_ID.FACING_BACK) //设置Camera后置
                .setPreviewSize(size[0], size[1]) //Camera采集的预设分辨率，实际打开过程会根据机型找到最合适的分辨率打开预览
                .build();

        //录制视频的配置
        VEVideoEncodeSettings videoEncodeSettings = new VEVideoEncodeSettings.Builder(VEVideoEncodeSettings.USAGE_RECORD)
                .setVideoRes(size[0], size[1]) //录制的视频分辨率
                .setSupportHwEnc(true) //使用硬编码
                .enableRecordingMp4(true)
                .build();
        //开启 enableRecordingMp4时，需要添加一下代码修复ve 蒙版无法生效的问题
        VEConfigCenter.getInstance().updateValue(VEConfigKeys.KEY_ENABLE_AMAZING_UNIT_UNIFY, true);
        VEConfigCenter.getInstance().syncConfigToNative();
        //录制音频的配置，暂时用不上，sdk默认生成44100双声道的音频文件
        VEAudioEncodeSettings audioEncodeSettings = new VEAudioEncodeSettings.Builder()
                .Build();


        //预览及其策略的配置
        VEPreviewSettings.Builder builder = new VEPreviewSettings.Builder()
                .setRenderSize(new VESize(size[0], size[1])) //设置预览分辨率即sdk渲染的纹理大小
                .blockRenderExit(true);
        if (previewConfig != null) {
            builder.setRecordContentType(previewConfig.getVERecordContentType());
            builder.enable3buffer(previewConfig.getEnable3buffer());
            builder.enableEffectRT(previewConfig.getEnableEffectRT());
            builder.enableFollowShotIndependentThread(previewConfig.getEnableFollowShotIndependentThread());
        }
        VEPreviewSettings previewSettings = builder
                .build();

        //创建Camera Capture
        capture = new VECameraCapture();

        //初始化Camera capture
        capture.init(getView().getContext(), cameraSettings);

        //开启相机
        capture.open();


        File vesdk = getView().getContext().getExternalFilesDir("vesdk");
        if (!vesdk.exists() || !vesdk.isDirectory()) {
            if (!vesdk.mkdirs()) {
                ToastUtils.show("创建工作目录失败");
            }
        }
        String workSpace = vesdk.getAbsolutePath();
        if (previewConfig != null && previewConfig.getEnableRefactorRecorder()) {
            //EnableRefactorRecorder,需要手动管理surfaceView
            recorder = new VERecorder(workSpace, getView().getContext());
            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    surface = holder.getSurface();
                    surfaceWidth = surfaceView.getWidth();
                    surfaceHeight = surfaceView.getHeight();
                    //异步开启预览
                    recorder.startPreview(surface);
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    boolean surfaceChanged = holder.getSurface() != surface
                            || width != surfaceWidth
                            || height != surfaceHeight;
                    if (surfaceChanged) {
                        recorder.changeSurface(holder.getSurface());
                        surface = holder.getSurface();
                        surfaceWidth = width;
                        surfaceHeight = height;
                    }
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    //异步结束录制
                    capture.close();
                    recorder.stopPreview(true);
                }
            });
        } else {
            //创建VERecorder,配置工作目录或者自定义ResManager
            //Context建议使用ApplicationContext防止泄漏
            //配置VERenderView,则sdk会注册回调，会在相应的回调周期内自动绑定Recorder的生命周期，否则生命周期自己控制
            recorder = new VERecorder(workSpace, getView().getContext(), surfaceView);
        }
        //开启新引擎
        if (previewConfig != null) {
            recorder.enableEffectAmazing(previewConfig.getEnableEffectAmazing());
        }
        //设置Recorder状态监听,init前调用
        recorder.setRecorderStateListener(new VEListener.VERecorderStateExtListener() {
            @Override
            public void onInfo(int infoType, int ext, String msg) {
                if (infoType == VEInfo.TET_RENDER_CREATED) { //Recorder渲染环境创建成功

                    LogUtils.d("onInfo  create------------");
                    recorder.startCameraPreview(capture); //开启Camera的预览

                } else if (infoType == VEInfo.TET_RENDER_DESTROYED) { //Recorder 渲染环境销毁

                    capture.stopPreview();  // 关闭Camera预览

                    LogUtils.d("onInfo  destroyed------------");
                } else if (infoType == VEInfo.TET_INIT) { //Recorder 渲染模块初始化成功
                    //建议在此之后开启，但不要再此回调中配置，以防止block渲染线程
                    //1）特效效果运行时参数，比如各种传感器参数
                    //2）录制，渲染模块初始化成功之后，才能正常吐帧给编码模块
                }

            }

            @Override
            public void onError(int ret, String msg) {

            }

            @Override
            public void onNativeInit(int ret, String msg) {
                LogUtils.d("onNativeInit...ret:" + ret + "  msg:" + msg );
                // 更改画幅比列 放在onNativeInit之中
                if (getPreviewModel().autoChangeDisplay()) {
                    getPreviewModel().changePic(-1); //
                }
                getView().onRecorderNativeInit(ret, msg);
                //mode : 传1打开下沉（使用composer,现在就传1）  order：目前只能传0
                //这里要先设置，否则后续api 不会生效
                recorder.setComposerMode(1, 0);
                restoreComposer(); //恢复滤镜、美颜效果 如退到后台再重新进入 需重新加载保存的滤镜、美颜效果
            }

            @Override
            public void onHardEncoderInit(boolean success) {

            }
        });

        //如果是合拍模式 需要初始化以下
        if ( isDuet ){
            List<ResourceItem> resourceItems =  ResourceHelper.getInstance().getDuetList();
            DUET_FIRST = resourceItems.size() > 0 ? resourceItems.get(0).getPath() : "";
            DUET_FIRST_IMG_PATH = resourceItems.size() > 0 ? resourceItems.get(0).getIcon() : "";

            DUET_SECOND = resourceItems.size() > 1 ? resourceItems.get(1).getPath() : "";
            DUET_SECOND_IMG_PATH = resourceItems.size() > 1 ? resourceItems.get(1).getIcon() : "";
            // 需要保证对导入的合拍视频进行了预处理，参考ReEncodeUtil类中的reEncodeVideo方法
            initDuet(duetVideoPath,duetAudioPath,true);
            getView().changeDuetImage(DUET_FIRST_IMG_PATH);
        }

        // ve960之后 需要开启 enableEffectAmazing(true)
        recorder.enableEffectAmazing(true);

        //初始化Recorder，其中，cameraCapture为null表示Camera与Recorder分开控制，由业务端保证Camera的生命周期
        //否则，Recorder会绑定Camera的生命周期，两者会耦合，不建议。
        int ret = recorder.init((VECameraCapture) null, videoEncodeSettings, audioEncodeSettings, previewSettings);

        if (ret != VEResult.TER_OK) {
            ToastUtils.show("recorder 初始化失败");
            //初始化失败，走错误逻辑
            return;
        }

    }

    public PreviewModel getPreviewModel(){
        return ((PreviewFragment)getView()).getPreviewModel();
    }

    /**
     * VEDuet配置的构造函数
     * @since 2.0.1
     */
    private void initDuet(String duetUrl, String audioPath,boolean enableV2) {
        //合拍时，画面是一整个，不是分开的。x,y是指整个画面距离左边、上边的归一化距离（这里左边无边距，上边留白0.25）
        recorder.enableAudio(enableV2);
        VEDuetSettings settings = new VEDuetSettings(duetUrl, audioPath,
                0f, 0.25f, 1.0f, true);
        settings.setEnableV2(true); // 开启合拍多布局模式
        recorder.initDuet(settings);
    }


    /**
     * 设置贴纸
     * @param file
     */
    @Override
    public void setSticker(File file) { // /stickers/landiaoxueying

        if (file == null) {
            if ( stickerSelected ){
                recorder.removeComposerNodes(stickerPaths, 1);
                stickerSelected = false ;
            }
            return;
        }

        if ( stickerSelected ){
            recorder.removeComposerNodes(stickerPaths, stickerPaths.length);
        }
        stickerSelected = true ;
        stickerPaths[0] = ResourceHelper.getInstance().getStickerPath(file.getAbsolutePath());
        int ret = recorder.appendComposerNodes(stickerPaths, stickerPaths.length);

        LogUtils.d("setStickerComposerNodes：" + ret);  // "/stickers/weilandongrizhuang"

    }
    private String[] mComposeNodes = new String[0];
    private Set<ComposerNode> mSavedComposerNodes = new HashSet<>();

    @Override
    public void setComposerNodes(String[] nodes) {

        if (nodes.length == 0) {
            mSavedComposerNodes.clear();
        }

        mComposeNodes = nodes ;

        String[] path = new String[nodes.length];

        if ( stickerSelected ){
            path = new String[nodes.length + 1 ];
        }

        // /storage/emulated/0/Android/data/com.ss.android.vesdk.vedemo/files/assets/resource/ComposeMakeup.bundle/ComposeMakeup/body/allslim
        for (int i = 0; i < nodes.length; i++) {
            path[i] = ResourceHelper.getInstance().getComposePath() + nodes[i];
        }
        if (stickerSelected){
            path[path.length - 1] = stickerPaths[0] ;
        }

        recorder.setComposerNodesWithTag(path,path.length,path);

        if (isDuet){
            recorder.appendComposerNodesWithTag(new String[]{
                    isFirst ? DUET_FIRST : DUET_SECOND
            }, 1, new String[]{""});
            getView().changeDuetImage(isFirst ? DUET_FIRST_IMG_PATH : DUET_SECOND_IMG_PATH);
        }

    }

    @Override
    public void updateComposerNode(ComposerNode node, boolean update) {
        if (update) {
            mSavedComposerNodes.remove(node);
            mSavedComposerNodes.add(node);
        }
        // 美颜 美体 x2  业务逻辑
        float value = node.getValue() ;
        // node.getNode():  "beauty_Android_camera" / "reshape_camera" /  "body/allslim"
        recorder.updateComposerNode(ResourceHelper.getInstance().getComposePath() + node.getNode(),node.getKey(),value );
    }
    /**
     * 恢复贴纸 滤镜 美妆等特效
     */
    @Override
    public void restoreComposer() {
        LogUtils.d("restoreComposer======");

        // 滤镜
        if (!TextUtils.isEmpty(currentFilter) ) {
            recorder.setFilterNew(currentFilter, filterValue);
        }

        if (mComposeNodes.length > 0) {
            setComposerNodes(mComposeNodes);

            for (ComposerNode node : mSavedComposerNodes) {
                updateComposerNode(node, false);
            }
        }else { //贴纸的还得再加载
            if ( stickerSelected ){
                recorder.removeComposerNodes(stickerPaths, stickerPaths.length);
                recorder.appendComposerNodes(stickerPaths, stickerPaths.length);
            }
        }

        // 如果是合拍
        if ( isDuet ){
//            recorder.enableEffect(true);
            recorder.appendComposerNodesWithTag(new String[]{
                    isFirst ? DUET_FIRST : DUET_SECOND
            }, 1, new String[]{""});
            getView().changeDuetImage(isFirst ? DUET_FIRST_IMG_PATH : DUET_SECOND_IMG_PATH);
        }
    }

    /**
     * 对比按钮按下
     */
    @Override
    public void onNormalDown() {

        for (ComposerNode node : mSavedComposerNodes) {
//            LogUtils.d("composer update: " + node.getNode() );
            recorder.updateComposerNode(ResourceHelper.getInstance().getComposePath() + node.getNode(),node.getKey(),0f );
        }
    }

    /**
     * 对比按钮松开
     */
    @Override
    public void onNormalUp() {
        restoreComposer();
    }


    @Override
    public void onFilterSelected(File file) {
        currentFilter = file != null ? ResourceHelper.getInstance().getFilterPath(file.getAbsolutePath()) :  "" ;
    }

    @Override
    public void onFilterValueChanged(float cur) {
        if (TextUtils.isEmpty(currentFilter) ) {
            recorder.setFilterNew(null,0);
            filterValue = 0f ;
        } else {
            recorder.setFilterNew(currentFilter, cur);
            filterValue = cur ;
        }
    }


    private boolean isFirst = true ;
    private String DUET_FIRST = "duet_leftRight" ;
    private String DUET_SECOND = "duet_upDown";
    private String DUET_SECOND_IMG_PATH = "";
    private String DUET_FIRST_IMG_PATH = "";

    @Override
    public void onSwitchDuet() {
        String duetRes; // duet_updown  duet_leftright
        if (isFirst) {
            recorder.removeComposerNodes(new String[]{DUET_FIRST},1);
            duetRes = DUET_SECOND;
            isFirst = false ;
        } else {
            recorder.removeComposerNodes(new String[]{DUET_SECOND},1);
            duetRes = DUET_FIRST;
            isFirst = true ;
        }
        getView().changeDuetImage(isFirst ? DUET_FIRST_IMG_PATH : DUET_SECOND_IMG_PATH);
        recorder.appendComposerNodesWithTag(new String[]{duetRes}, 1, new String[]{""});
    }


}
