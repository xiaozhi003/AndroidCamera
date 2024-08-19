package com.android.xz.camerademo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.android.xz.view.CameraTextureView;

public class TextureCameraActivity extends AppCompatActivity {

    private CameraTextureView mCameraTextureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_camera);

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