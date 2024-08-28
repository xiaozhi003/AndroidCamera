package com.android.xz.camerademo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.android.xz.camerademo.camera2_activity.GLSurfaceCamera2Activity;
import com.android.xz.camerademo.camera2_activity.GLSurfaceHolderCamera2Activity;
import com.android.xz.camerademo.camera2_activity.GLTextureCamera2Activity;
import com.android.xz.camerademo.camera2_activity.SurfaceCamera2Activity;
import com.android.xz.camerademo.camera2_activity.TextureCamera2Activity;
import com.android.xz.camerademo.camera_activity.GLSurfaceCameraActivity;
import com.android.xz.camerademo.camera_activity.GLSurfaceHolderCameraActivity;
import com.android.xz.camerademo.camera_activity.GLTextureCameraActivity;
import com.android.xz.camerademo.camera_activity.SurfaceCameraActivity;
import com.android.xz.camerademo.camera_activity.TextureCameraActivity;
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
                        startActivity(new Intent(mContext, SurfaceCameraActivity.class));
                        break;
                    case R.id.textureCameraBtn:
                        startActivity(new Intent(mContext, TextureCameraActivity.class));
                        break;
                    case R.id.glTextureCameraBtn:
                        startActivity(new Intent(mContext, GLTextureCameraActivity.class));
                        break;
                    case R.id.glSurfaceCameraBtn:
                        startActivity(new Intent(mContext, GLSurfaceCameraActivity.class));
                        break;
                    case R.id.surfaceCamera2Btn:
                        startActivity(new Intent(mContext, SurfaceCamera2Activity.class));
                        break;
                    case R.id.textureCamera2Btn:
                        startActivity(new Intent(mContext, TextureCamera2Activity.class));
                        break;
                    case R.id.glSurfaceCamera2Btn:
                        startActivity(new Intent(mContext, GLSurfaceCamera2Activity.class));
                        break;
                    case R.id.glTextureCamera2Btn:
                        startActivity(new Intent(mContext, GLTextureCamera2Activity.class));
                        break;
                    case R.id.glSurfaceHolderCameraBtn:
                        startActivity(new Intent(mContext, GLSurfaceHolderCameraActivity.class));
                        break;
                    case R.id.glSurfaceHolderCamera2Btn:
                        startActivity(new Intent(mContext, GLSurfaceHolderCamera2Activity.class));
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