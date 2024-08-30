package com.android.xz.camerademo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.android.xz.camerademo.base.BaseCameraActivity;

public class CameraActivity extends BaseCameraActivity {

    public static final String EXTRA_LAYOUT_ID = "com.android.xz.camera.EXTRA_LAYOUT_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public int getLayoutId() {
        Intent intent = getIntent();
        return intent.getIntExtra(EXTRA_LAYOUT_ID, R.layout.activity_surface_camera);
    }
}