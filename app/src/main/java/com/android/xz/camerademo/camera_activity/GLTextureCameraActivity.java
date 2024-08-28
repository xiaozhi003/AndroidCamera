package com.android.xz.camerademo.camera_activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;

import com.android.xz.camera.CameraManager;
import com.android.xz.camera.callback.PreviewBufferCallback;
import com.android.xz.camerademo.MediaDisplayActivity;
import com.android.xz.camerademo.R;
import com.android.xz.util.ImageUtils;
import com.android.xz.view.CameraGLTextureView;

public class GLTextureCameraActivity extends AppCompatActivity {

    private CameraGLTextureView mCameraGLTextureView;
    private CameraManager mCameraManager;
    private ImageView mPictureIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gltexture_camera);

        mCameraGLTextureView = findViewById(R.id.cameraView);
        mCameraManager = (CameraManager) mCameraGLTextureView.getCameraManager();
        findViewById(R.id.captureBtn).setOnClickListener(v -> capture());
        findViewById(R.id.switchCameraBtn).setOnClickListener(v -> mCameraManager.switchCamera());
        mPictureIv = findViewById(R.id.pictureIv);
        mPictureIv.setOnClickListener(v -> {
            String path = (String) v.getTag();
            Intent intent = new Intent(this, MediaDisplayActivity.class);
            intent.putExtra(MediaDisplayActivity.EXTRA_MEDIA_PATH, path);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraGLTextureView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraGLTextureView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraGLTextureView.onDestroy();
    }

    private void capture() {
        mCameraManager.takePicture(mPictureCallback);
    }

    private final Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mCameraManager.startPreview(mCameraGLTextureView.getSurfaceTexture()); // 拍摄结束继续预览
            new ImageSaveTask().execute(data); // 保存图片
        }
    };

    private class ImageSaveTask extends AsyncTask<byte[], Void, Bitmap> {

        private String path;

        @Override
        protected Bitmap doInBackground(byte[]... bytes) {
            path = ImageUtils.saveImage(bytes[0]);
            return ImageUtils.getLatestThumbBitmap();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mPictureIv.setImageBitmap(bitmap);
            mPictureIv.setTag(path);
        }
    }
}