package com.android.xz.camerademo.base;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.xz.camera.ICameraManager;
import com.android.xz.camerademo.MediaDisplayActivity;
import com.android.xz.camerademo.R;
import com.android.xz.util.ImageUtils;
import com.android.xz.camera.view.base.BaseCameraView;

import java.util.concurrent.Executors;

/**
 * 展示相机页面
 *
 * @author xiaozhi
 */
public abstract class BaseCameraActivity extends AppCompatActivity {

    protected ICameraManager mCameraManager;
    protected ImageView mPictureIv;
    protected BaseCameraView mBaseCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        mBaseCameraView = findViewById(R.id.cameraView);
        mCameraManager = mBaseCameraView.getCameraManager();
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

    /**
     * 设置不同的layout id
     *
     * @return
     */
    public abstract int getLayoutId();

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBaseCameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBaseCameraView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBaseCameraView.onDestroy();
    }

    protected void capture() {
        mCameraManager.takePicture(data -> {
            new ImageSaveTask().executeOnExecutor(Executors.newSingleThreadExecutor(), data); // 保存图片
        });
    }

    private class ImageSaveTask extends AsyncTask<byte[], Void, Bitmap> {

        private String path;

        @Override
        protected Bitmap doInBackground(byte[]... bytes) {
            path = ImageUtils.saveImage(bytes[0]);
            return ImageUtils.getCorrectOrientationBitmap(path, new Size(mPictureIv.getMeasuredWidth(), mPictureIv.getMeasuredHeight()));
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mPictureIv.setImageBitmap(bitmap);
            mPictureIv.setTag(path);
        }
    }
}