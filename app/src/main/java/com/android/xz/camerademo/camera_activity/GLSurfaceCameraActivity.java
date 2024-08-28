package com.android.xz.camerademo.camera_activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Size;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.xz.camera.CameraManager;
import com.android.xz.camera.callback.PreviewBufferCallback;
import com.android.xz.camerademo.MediaDisplayActivity;
import com.android.xz.camerademo.R;
import com.android.xz.camerademo.view.CaptureButton;
import com.android.xz.encoder.MediaRecordListener;
import com.android.xz.util.ImageUtils;
import com.android.xz.util.Logs;
import com.android.xz.view.CameraGLSurfaceView;

public class GLSurfaceCameraActivity extends AppCompatActivity {

    private static final String TAG = GLSurfaceCameraActivity.class.getSimpleName();
    private CameraGLSurfaceView mCameraGLSurfaceView;
    private CameraManager mCameraManager;
    private ImageView mPictureIv;
    private CaptureButton mCaptureBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glsurface_camera);

        mCameraGLSurfaceView = findViewById(R.id.cameraView);
        mCameraGLSurfaceView.setRecordListener(mRecordListener);
        mCameraManager = (CameraManager) mCameraGLSurfaceView.getCameraManager();
        mCaptureBtn = findViewById(R.id.captureBtn);
        mCaptureBtn.setClickListener(mClickListener);
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

    private void capture() {
        mCameraManager.takePicture(mPictureCallback);
    }

    private void startRecord() {
        mCameraGLSurfaceView.startRecord();
    }

    private void stopRecord() {
        mCameraGLSurfaceView.stopRecord();
    }

    private final CaptureButton.ClickListener mClickListener = new CaptureButton.ClickListener() {
        @Override
        public void onCapture() {
            capture();
        }

        @Override
        public void onStartRecord() {
            startRecord();
        }

        @Override
        public void onStopRecord() {
            stopRecord();
        }
    };

    private final Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mCameraManager.startPreview(mCameraGLSurfaceView.getSurfaceTexture()); // 拍摄结束继续预览
            new ImageSaveTask().execute(data); // 保存图片
        }
    };

    private final MediaRecordListener mRecordListener = new MediaRecordListener() {
        @Override
        public void onStart() {

        }

        @Override
        public void onStopped(String videoPath) {
            mPictureIv.setImageBitmap(getVideoThumb(videoPath));
            mPictureIv.setTag(videoPath);
            mCaptureBtn.stopRecord();
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

    /**
     * 获取视频文件第一帧图
     *
     * @param path 视频文件的路径
     * @return Bitmap 返回获取的Bitmap
     */
    public static Bitmap getVideoThumb(String path) {
        MediaMetadataRetriever media = new MediaMetadataRetriever();
        media.setDataSource(path);
        return media.getFrameAtTime();
    }
}