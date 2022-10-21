package com.ola.chat.picker.entry;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/18 17:30
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 图像选择器的实体类对象
 */

public class ImagePickConfig implements Parcelable {


    public static final int REQUEST_CODE_IMAGE_CROP = 1004;
    public static final String EXTRA_RESULT_IMAGE_FILE = "extra_result_image_path";

    //先定义 常量
    public static final int PICKER_SINGLE = 0; //单选模式
    public static final int PICKER_MIX = 1;//多选模式【九宫格】
    public static final int PICKER_CUT_SAME = 2;//剪同款模式

    public static final String RECTANGLE = "RECTANGLE";
    public static final String CIRCLE = "CIRCLE";

    public static final String SELECT_ALL = "ALL";//混合展示
    public static final String SELECT_IMAGE = "IMAGE";//展示图片
    public static final String SELECT_VIDEO = "VIDEO";//展示视频

    public ImagePickConfig() {

    }

    protected ImagePickConfig(Parcel in) {
        sceneType = in.readInt();
        crop = in.readByte() != 0;
        cropStyle = in.readString();
        defaultResourceType = in.readString();
        focusWidth = in.readInt();
        focusHeight = in.readInt();
        maxCount = in.readInt();
        cropWidth = in.readInt();
        cropHeight = in.readInt();
    }

    public static final Creator<ImagePickConfig> CREATOR = new Creator<ImagePickConfig>() {
        @Override
        public ImagePickConfig createFromParcel(Parcel in) {
            return new ImagePickConfig(in);
        }

        @Override
        public ImagePickConfig[] newArray(int size) {
            return new ImagePickConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(sceneType);
        dest.writeByte((byte) (crop ? 1 : 0));
        dest.writeString(cropStyle);
        dest.writeString(defaultResourceType);
        dest.writeInt(focusWidth);
        dest.writeInt(focusHeight);
        dest.writeInt(maxCount);
        dest.writeInt(cropWidth);
        dest.writeInt(cropHeight);
    }


    //注解枚举
    @IntDef({PICKER_SINGLE, PICKER_MIX, PICKER_CUT_SAME})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SceneType {
    }

    @StringDef({RECTANGLE, CIRCLE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CropStyle {
    }

    //注解枚举
    @StringDef({SELECT_ALL, SELECT_IMAGE, SELECT_VIDEO})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DefaultSelectType {
    }

    //图片选择器使用场景（图像替换、九宫格、剪同款）
    @SceneType
    private int sceneType = PICKER_SINGLE;

    //是否允许裁剪（单选有效
    private boolean crop = true;

    @CropStyle
    private String cropStyle = CIRCLE;

    //进入相册指示器默认选中的页面
    @DefaultSelectType
    private String defaultResourceType = SELECT_IMAGE;

    //矩形裁剪框宽度（圆形自动取宽高最小值）
    private int focusWidth = 300;
    private int focusHeight = cropStyle.equals(CIRCLE) ? 300 : 400;

    //最大可以选择多少张图片
    private int maxCount = 1;

    //图片裁剪完成后保存的的宽高
    private int cropWidth = 800;
    private int cropHeight = 800;


    public @SceneType
    int getSceneType() {
        return sceneType;
    }

    public void setSceneType(@SceneType int sceneType) {
        this.sceneType = sceneType;
    }

    public boolean getCrop() {
        return crop;
    }

    public void setCrop(boolean crop) {
        this.crop = crop;
    }

    public @DefaultSelectType
    String getDefaultResourceType() {
        return defaultResourceType;
    }

    public void setDefaultResourceType(@DefaultSelectType String defaultResourceType) {
        this.defaultResourceType = defaultResourceType;
    }

    public int getFocusWidth() {
        return focusWidth;
    }

    public void setFocusWidth(int focusWidth) {
        this.focusWidth = focusWidth;
    }

    public int getFocusHeight() {
        return focusHeight;
    }

    public void setFocusHeight(int focusHeight) {
        this.focusHeight = focusHeight;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public String getCropStyle() {
        return cropStyle;
    }

    public void setCropStyle(String cropStyle) {
        this.cropStyle = cropStyle;
    }

    public int getCropWidth() {
        return cropWidth;
    }

    public void setCropWidth(int cropWidth) {
        this.cropWidth = cropWidth;
    }

    public int getCropHeight() {
        return cropHeight;
    }

    public void setCropHeight(int cropHeight) {
        this.cropHeight = cropHeight;
    }

    @Override
    public String toString() {
        return "ImagePickConfig{" +
                "sceneType=" + sceneType +
                ", crop=" + crop +
                ", cropStyle='" + cropStyle + '\'' +
                ", defaultResourceType='" + defaultResourceType + '\'' +
                ", focusWidth=" + focusWidth +
                ", focusHeight=" + focusHeight +
                ", maxCount=" + maxCount +
                ", cropWidth=" + cropWidth +
                ", cropHeight=" + cropHeight +
                '}';
    }
}
