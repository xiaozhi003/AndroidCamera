package com.android.xz.camerademo.camera_activity;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.xz.camera.CameraManager;
import com.android.xz.camera.callback.PreviewBufferCallback;
import com.android.xz.camerademo.R;
import com.android.xz.util.ImageUtils;
import com.android.xz.view.CameraTextureView;

public class TextureCameraActivity extends AppCompatActivity {

    private static final String TAG = TextureCameraActivity.class.getSimpleName();
    private CameraTextureView mCameraTextureView;
    private CameraManager mCameraManager;
    private ImageView mPictureIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_camera);

        mCameraTextureView = findViewById(R.id.textureView);
        mCameraManager = (CameraManager) mCameraTextureView.getCameraManager();
        mCameraManager.setPreviewBufferCallback(mPreviewBufferCallback);
        findViewById(R.id.captureBtn).setOnClickListener(v -> capture());
        findViewById(R.id.switchCameraBtn).setOnClickListener(v -> mCameraManager.switchCamera());
        mPictureIv = findViewById(R.id.pictureIv);
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

    private void capture() {
        mCameraManager.takePicture(mPictureCallback);
    }

    private final Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mCameraManager.startPreview(mCameraTextureView.getSurfaceTexture()); // 拍摄结束继续预览
            new ImageSaveTask().execute(data); // 保存图片
        }
    };

    private class ImageSaveTask extends AsyncTask<byte[], Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(byte[]... bytes) {
            ImageUtils.saveImage(bytes[0]);
            return ImageUtils.getLatestThumbBitmap();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mPictureIv.setImageBitmap(bitmap);
        }
    }

    private PreviewBufferCallback mPreviewBufferCallback = new PreviewBufferCallback() {
        @Override
        public void onPreviewBufferFrame(byte[] data, int width, int height) {
//            Logs.i(TAG, "onPreviewBufferFrame...");
        }
    };
}