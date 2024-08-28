package com.android.xz.camerademo;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.android.xz.util.ImageUtils;
import com.android.xz.util.Logs;

import java.io.File;

public class MediaDisplayActivity extends AppCompatActivity {

    public static final String EXTRA_MEDIA_PATH = "com.android.xz.media_path";

    private static final String TAG = MediaDisplayActivity.class.getSimpleName();

    private String mMediaPath;
    private VideoView mVideoView;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_media);

        mMediaPath = getIntent().getStringExtra(EXTRA_MEDIA_PATH);
        mVideoView = findViewById(R.id.videoView);
        mImageView = findViewById(R.id.imageView);

        if (mMediaPath.endsWith("mp4")) {
            displayVideo();
        } else {
            displayImage();
        }
    }

    private void displayVideo() {
        mVideoView.setVisibility(View.VISIBLE);
        mImageView.setVisibility(View.GONE);
        // 设置path会报java.io.FileNotFoundException: No content provider 警告
//        mVideoView.setVideoPath(mVideoPath);
        mVideoView.setVideoURI(Uri.fromFile(new File(mMediaPath)));

        // 创建媒体控制器(MediaController)
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(mVideoView);

        // 关联媒体控制器
        mVideoView.setMediaController(mediaController);

        // 开始播放视频
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Logs.i(TAG, "onCompletion...");
            }
        });
        mVideoView.setOnPreparedListener(mp -> {
            Logs.i(TAG, "onPrepared...");
            mp.start();
        });
    }

    private void displayImage() {
        mVideoView.setVisibility(View.GONE);
        mImageView.setVisibility(View.VISIBLE);
        mImageView.getViewTreeObserver().addOnGlobalLayoutListener(() -> new Thread(() -> {
            Bitmap bitmap = ImageUtils.getCorrectOrientationBitmap(mMediaPath, new Size(mImageView.getMeasuredWidth(), mImageView.getMeasuredHeight()));
            mImageView.post(() -> mImageView.setImageBitmap(bitmap));
        }).start());
    }
}