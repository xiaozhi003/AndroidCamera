package com.android.xz.camerademo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.android.xz.view.Camera2GLSurfaceView;
import com.android.xz.view.CameraGLSurfaceView;

public class GLSurfaceCamera2Activity extends AppCompatActivity {

    private Camera2GLSurfaceView mCameraGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glsurface_camera2);

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