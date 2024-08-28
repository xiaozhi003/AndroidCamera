package com.android.xz.camerademo.camera2_activity;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.xz.camera.Camera2Manager;
import com.android.xz.camera.callback.PreviewBufferCallback;
import com.android.xz.camerademo.R;
import com.android.xz.util.ImageUtils;
import com.android.xz.view.Camera2GLSurfaceView;

import java.nio.ByteBuffer;

public class GLSurfaceCamera2Activity extends AppCompatActivity {

    private static final String TAG = GLSurfaceCamera2Activity.class.getSimpleName();
    private Camera2GLSurfaceView mCameraGLSurfaceView;
    private Camera2Manager mCameraManager;
    private ImageView mPictureIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glsurface_camera2);

        mCameraGLSurfaceView = findViewById(R.id.cameraView);
        mCameraManager = (Camera2Manager) mCameraGLSurfaceView.getCameraManager();
        findViewById(R.id.captureBtn).setOnClickListener(v -> capture());
        findViewById(R.id.switchCameraBtn).setOnClickListener(v -> mCameraManager.switchCamera());
        mPictureIv = findViewById(R.id.pictureIv);
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
        mCameraManager.captureStillPicture(reader -> new ImageSaveTask().execute(reader.acquireNextImage()));
    }

    private class ImageSaveTask extends AsyncTask<Image, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Image ... images) {
            ByteBuffer buffer = images[0].getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            ImageUtils.saveImage(bytes);
            images[0].close();
            return ImageUtils.getLatestThumbBitmap();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mPictureIv.setImageBitmap(bitmap);
        }
    }
}