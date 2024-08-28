package com.android.xz.view;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.media.MediaMetadata;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.android.xz.camera.CameraManager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.callback.PreviewBufferCallback;
import com.android.xz.encoder.BufferMovieEncoder;
import com.android.xz.encoder.MediaEncoder;
import com.android.xz.encoder.MediaMuxerWrapper;
import com.android.xz.encoder.MediaRecordListener;
import com.android.xz.encoder.MediaVideoBufferEncoder;
import com.android.xz.util.ImageUtils;
import com.android.xz.util.Logs;
import com.android.xz.util.YUVUtils;
import com.android.xz.view.base.BaseSurfaceView;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Created by wangzhi on 2024/8/22.
 */
public class CameraSurfaceView extends BaseSurfaceView {

    private BufferMovieEncoder mEncoder;

    public CameraSurfaceView(Context context) {
        super(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        mEncoder = new BufferMovieEncoder(context);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logs.i(TAG, "surfaceChanged [" + width + ", " + height + "]");
        mSurfaceWidth = width;
        mSurfaceHeight = height;
    }

    @Override
    public void onOpen() {
        mPreviewSize = getCameraManager().getPreviewSize();
        getCameraManager().addPreviewBufferCallback(mPreviewBufferCallback);
        getCameraManager().startPreview(getSurfaceHolder());
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRecord();
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        return new CameraManager(context);
    }

    private PreviewBufferCallback mPreviewBufferCallback = new PreviewBufferCallback() {
        @Override
        public void onPreviewBufferFrame(byte[] data, int width, int height) {
            if (mEncoder != null) {
                mEncoder.encode(data);
            }
        }
    };

    public void startRecord() {
        if (!getCameraManager().isOpen()) {
            return;
        }
        mEncoder.setRecordListener(mRecordListener);
        mEncoder.startRecord(getCameraManager().getOrientation(), getCameraManager().getPreviewSize());
    }

    public void stopRecord() {
        mEncoder.stopRecord();
    }

    private MediaRecordListener mRecordListener;

    public void setRecordListener(MediaRecordListener recordListener) {
        mRecordListener = recordListener;
    }
}
