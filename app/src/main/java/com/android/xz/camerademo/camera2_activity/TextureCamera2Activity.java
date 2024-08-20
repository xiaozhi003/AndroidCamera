package com.android.xz.camerademo.camera2_activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.android.xz.camerademo.R;
import com.android.xz.view.Camera2TextureView;
import com.android.xz.view.CameraTextureView;

public class TextureCamera2Activity extends AppCompatActivity {

    private Camera2TextureView mCameraTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_camera2);

        mCameraTextureView = findViewById(R.id.textureView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraTextureView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraTextureView.onPause();
    }
}