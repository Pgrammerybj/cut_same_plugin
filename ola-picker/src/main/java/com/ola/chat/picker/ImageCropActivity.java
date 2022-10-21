package com.ola.chat.picker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ola.chat.picker.album.model.MediaData;
import com.ola.chat.picker.customview.CropImageView;
import com.ola.chat.picker.entry.ImagePickConfig;
import com.ola.chat.picker.utils.BitmapUtil;

import java.io.File;

import static com.ola.chat.picker.utils.PickerConstant.ARG_CLIP_MEDIA_ITEM;
import static com.ola.chat.picker.utils.PickerConstant.ARG_CLIP_PICKER_CONFIG;

/**
 * ================================================
 * 作    者：jeasonlzy（廖子尧 Github地址：https://github.com/jeasonlzy0216
 * 版    本：1.0
 * 创建日期：2016/5/19
 * 描    述：
 * 修订历史：
 * ================================================
 */
public class ImageCropActivity extends AppCompatActivity implements View.OnClickListener, CropImageView.OnBitmapSaveCompleteListener {

    private CropImageView mCropImageView;
    private Bitmap mBitmap;
    private boolean mIsSaveRectangle = false;  //裁剪后的图片是否是矩形，否者跟随裁剪框的形状
    private int mOutputX = 800;//裁剪保存宽度
    private int mOutputY = 800;//裁剪保存宽度
    File cropCacheFolder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);

        MediaData mediaData = getIntent().getParcelableExtra(ARG_CLIP_MEDIA_ITEM);
        ImagePickConfig imagePickConfig = getIntent().getParcelableExtra(ARG_CLIP_PICKER_CONFIG);



        //初始化View
        findViewById(R.id.btn_back).setOnClickListener(this);
        Button btn_ok = findViewById(R.id.btn_clip_ok);
        btn_ok.setText(getString(R.string.cutsame_common_finish));
        btn_ok.setOnClickListener(this);
        TextView tv_des = findViewById(R.id.tv_des);
        tv_des.setText(getString(R.string.ip_photo_crop));
        mCropImageView = findViewById(R.id.cv_crop_image);
        mCropImageView.setOnBitmapSaveCompleteListener(this);

        //获取需要的参数
        String imagePath = mediaData.getPath();
        mCropImageView.setFocusStyle(CropImageView.Style.CIRCLE);
        mCropImageView.setFocusWidth(imagePickConfig.getFocusWidth());
        mCropImageView.setFocusHeight(imagePickConfig.getFocusHeight());


        mOutputX = imagePickConfig.getCropWidth();
        mOutputY = imagePickConfig.getCropHeight();

        //缩放图片
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        options.inSampleSize = calculateInSampleSize(options, displayMetrics.widthPixels, displayMetrics.heightPixels);
        options.inJustDecodeBounds = false;
        mBitmap = BitmapFactory.decodeFile(imagePath, options);
        //设置默认旋转角度
        mCropImageView.setImageBitmap(mCropImageView.rotate(mBitmap, BitmapUtil.getBitmapDegree(imagePath)));
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = width / reqWidth;
            } else {
                inSampleSize = height / reqHeight;
            }
        }
        return inSampleSize;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_back) {
            setResult(RESULT_CANCELED);
            finish();
        } else if (id == R.id.btn_clip_ok) {
            mCropImageView.saveBitmapToFile(getCropCacheFolder(this), mOutputX, mOutputY, mIsSaveRectangle);
        }
    }

    @Override
    public void onBitmapSaveSuccess(File file) {
        Log.i("Ola-picker-clip", "裁剪成功getPath:" + file.getPath());
        //裁剪后替换掉返回数据的内容，但是不要改变全局中的选中数据
        Intent intent = new Intent();
        intent.putExtra(ImagePickConfig.EXTRA_RESULT_IMAGE_FILE, file.getAbsolutePath());
        setResult(ImagePickConfig.REQUEST_CODE_IMAGE_CROP, intent);   //单选不需要裁剪，返回数据
        finish();
    }

    public File getCropCacheFolder(Context context) {
        if (cropCacheFolder == null) {
            cropCacheFolder = new File(context.getCacheDir() + "/ImagePicker/cropTemp/");
        }
        return cropCacheFolder;
    }

    @Override
    public void onBitmapSaveError(File file) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCropImageView.setOnBitmapSaveCompleteListener(null);
        if (null != mBitmap && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
}
