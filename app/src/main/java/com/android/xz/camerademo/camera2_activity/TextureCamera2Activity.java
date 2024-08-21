package com.android.xz.camerademo.camera2_activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.xz.camera.Camera2Manager;
import com.android.xz.camera.callback.PreviewBufferCallback;
import com.android.xz.camerademo.R;
import com.android.xz.camerademo.camera_activity.TextureCameraActivity;
import com.android.xz.util.ImageUtils;
import com.android.xz.view.Camera2TextureView;
import com.android.xz.view.CameraTextureView;

import java.nio.ByteBuffer;

public class TextureCamera2Activity extends AppCompatActivity {

    private static final String TAG = TextureCamera2Activity.class.getSimpleName();
    private Camera2TextureView mCameraTextureView;
    private Camera2Manager mCameraManager;
    private ImageView mPictureIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_camera2);

        mCameraTextureView = findViewById(R.id.textureView);
        mCameraManager = (Camera2Manager) mCameraTextureView.getCameraManager();
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
        mCameraManager.captureStillPicture(reader -> new ImageSaveTask().execute(reader.acquireNextImage()));
    }

    private class ImageSaveTask extends AsyncTask<Image, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Image ... images) {
            ByteBuffer buffer = images[0].getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            long time = System.currentTimeMillis();
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Log.d(TAG, "BitmapFactory.decodeByteArray time: " + (System.currentTimeMillis() - time));
            int rotation = mCameraManager.getLatestRotation();
            time = System.currentTimeMillis();
            Bitmap rotateBitmap = ImageUtils.rotateBitmap(bitmap, rotation, mCameraManager.isFrontCamera(), true);
            Log.d(TAG, "rotateBitmap time: " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            ImageUtils.saveBitmap(rotateBitmap);
            Log.d(TAG, "saveBitmap time: " + (System.currentTimeMillis() - time));
            rotateBitmap.recycle();

            images[0].close();
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