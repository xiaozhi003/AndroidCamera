package com.android.xz.camerademo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.xz.camerademo.mediacodec_activity.MediaCodecBufferActivity;
import com.android.xz.camerademo.mediacodec_activity.MediaCodecSurfaceActivity;
import com.android.xz.permission.IPermissionsResult;
import com.android.xz.permission.PermissionUtils;
import com.android.xz.util.ImageUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        ImageUtils.init(getApplicationContext());

        findViewById(R.id.surfaceCameraBtn).setOnClickListener(this);
        findViewById(R.id.textureCameraBtn).setOnClickListener(this);
        findViewById(R.id.glTextureCameraBtn).setOnClickListener(this);
        findViewById(R.id.glSurfaceCameraBtn).setOnClickListener(this);
        findViewById(R.id.surfaceCamera2Btn).setOnClickListener(this);
        findViewById(R.id.textureCamera2Btn).setOnClickListener(this);
        findViewById(R.id.glSurfaceCamera2Btn).setOnClickListener(this);
        findViewById(R.id.glTextureCamera2Btn).setOnClickListener(this);
        findViewById(R.id.glSurfaceHolderCameraBtn).setOnClickListener(this);
        findViewById(R.id.glSurfaceHolderCamera2Btn).setOnClickListener(this);
        findViewById(R.id.mediaCodecBufferBtn).setOnClickListener(this);
        findViewById(R.id.mediaCodecSurfaceBtn).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        PermissionUtils.getInstance().requestPermission(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, new IPermissionsResult() {
            @Override
            public void passPermissions() {
                switch (v.getId()) {
                    case R.id.surfaceCameraBtn:
                        startCameraActivity(R.layout.activity_surface_camera);
                        break;
                    case R.id.textureCameraBtn:
                        startCameraActivity(R.layout.activity_texture_camera);
                        break;
                    case R.id.glTextureCameraBtn:
                        startCameraActivity(R.layout.activity_gltexture_camera);
                        break;
                    case R.id.glSurfaceCameraBtn:
                        startCameraActivity(R.layout.activity_glsurface_camera);
                        break;
                    case R.id.glSurfaceHolderCameraBtn:
                        startCameraActivity(R.layout.activity_glessurface_camera);
                        break;
                    case R.id.surfaceCamera2Btn:
                        startCameraActivity(R.layout.activity_surface_camera2);
                        break;
                    case R.id.textureCamera2Btn:
                        startCameraActivity(R.layout.activity_texture_camera2);
                        break;
                    case R.id.glSurfaceCamera2Btn:
                        startCameraActivity(R.layout.activity_glsurface_camera2);
                        break;
                    case R.id.glTextureCamera2Btn:
                        startCameraActivity(R.layout.activity_gltexture_camera2);
                        break;
                    case R.id.glSurfaceHolderCamera2Btn:
                        startCameraActivity(R.layout.activity_glessurface_camera2);
                        break;
                    case R.id.mediaCodecBufferBtn:
                        startActivity(new Intent(mContext, MediaCodecBufferActivity.class));
                        break;
                    case R.id.mediaCodecSurfaceBtn:
                        startActivity(new Intent(mContext, MediaCodecSurfaceActivity.class));
                        break;
                }
            }

            @Override
            public void forbidPermissions() {
                Toast.makeText(mContext, "用户拒绝Camera授权", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startCameraActivity(int layoutId) {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.putExtra(CameraActivity.EXTRA_LAYOUT_ID, layoutId);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        PermissionUtils.getInstance().onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.getInstance().onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }
}