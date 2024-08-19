package com.android.xz.camerademo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.android.xz.view.CameraGLSurfaceView;

public class GLSurfaceCameraActivity extends AppCompatActivity {

    private CameraGLSurfaceView mCameraGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glsurface_camera);

        mCameraGLSurfaceView = findViewById(R.id.glSurfaceView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraGLSurfaceView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraGLSurfaceView.onDestroy();
    }
}