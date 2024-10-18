package com.android.xz.camerademo.mediacodec_activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.view.CameraSurfaceView;
import com.android.xz.camerademo.MediaDisplayActivity;
import com.android.xz.camerademo.R;
import com.android.xz.camerademo.view.CaptureButton;
import com.android.xz.encoder.MediaRecordListener;
import com.android.xz.util.ImageUtils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

public class MediaCodecBufferActivity extends AppCompatActivity {

    private static final String TAG = MediaCodecBufferActivity.class.getSimpleName();
    private CameraSurfaceView mCameraSurfaceView;
    private ICameraManager mCameraManager;
    private ImageView mPictureIv;
    private CaptureButton mCaptureBtn;
    private TextView mTimeTv;
    private Timer mTimer = new Timer();
    private TimerTask mTimerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec_buffer);

        mCameraSurfaceView = findViewById(R.id.cameraView);
        mCameraSurfaceView.setRecordListener(mRecordListener);
        mCameraManager = mCameraSurfaceView.getCameraManager();
        mCaptureBtn = findViewById(R.id.captureBtn);
        mCaptureBtn.setClickListener(mClickListener);
        mTimeTv = findViewById(R.id.timeTv);
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
        mCameraSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraSurfaceView.onPause();
    }

    private void capture() {
        mCameraManager.takePicture(data -> {
            new ImageSaveTask().executeOnExecutor(Executors.newSingleThreadExecutor(), data); // 保存图片
        });
    }

    private void startRecord() {
        mCameraSurfaceView.startRecord();
    }

    private void stopRecord() {
        mCameraSurfaceView.stopRecord();
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

    private final MediaRecordListener mRecordListener = new MediaRecordListener() {
        @Override
        public void onStart() {
            mTimeTv.setVisibility(View.VISIBLE);
            mTimer.scheduleAtFixedRate(mTimerTask = new RecordTimerTask(), 0, 1000);
        }

        @Override
        public void onStopped(String videoPath) {
            new VideoTask().executeOnExecutor(Executors.newSingleThreadExecutor(), videoPath);
            mPictureIv.setTag(videoPath);
            mCaptureBtn.stopRecord();
            mTimeTv.setVisibility(View.GONE);
            if (mTimerTask != null) {
                mTimerTask.cancel();
                mTimerTask = null;
            }
        }
    };

    private class VideoTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... strings) {
            String videoPath = strings[0];
            return getVideoThumb(videoPath);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mPictureIv.setImageBitmap(bitmap);
        }
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

    private class RecordTimerTask extends TimerTask {

        private int mRecordSeconds;

        @Override
        public void run() {
            int hours = mRecordSeconds / 3600;
            mRecordSeconds %= 3600;
            int minutes = mRecordSeconds / 60;
            mRecordSeconds %= 60;
            int remainingSeconds = mRecordSeconds;

            String formattedTime = String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
            mTimeTv.post(() -> mTimeTv.setText(formattedTime));

            mRecordSeconds++;
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