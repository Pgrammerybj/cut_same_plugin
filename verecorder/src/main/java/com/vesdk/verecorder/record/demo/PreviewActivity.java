package com.vesdk.verecorder.record.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jkb.fragment.rigger.annotation.Puppet;
import com.vesdk.vebase.Constant;
import com.vesdk.verecorder.R;
import com.vesdk.verecorder.record.demo.fragment.PreviewFragment;


/**
 * 录制界面
 */
@Puppet
public class PreviewActivity extends AppCompatActivity {

    private PreviewFragment previewFragment;

    public static void startPreviewActivity(Activity activity, Bundle bundle) {
        Intent intent = new Intent(activity, PreviewActivity.class);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recorder_activity_preview);
        Intent intent = getIntent();
        boolean isDuet = intent.getBooleanExtra(Constant.isDuet, false);
        String duetVideoPath = intent.getStringExtra(Constant.duetVideoPath);
        String duetAudioPath = intent.getStringExtra(Constant.duetAudioPath);
        previewFragment = PreviewFragment.getInstance(isDuet, duetVideoPath, duetAudioPath);
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_recorder_preview_container, previewFragment, "").commitAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        if (previewFragment != null) {
            previewFragment.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }
}
