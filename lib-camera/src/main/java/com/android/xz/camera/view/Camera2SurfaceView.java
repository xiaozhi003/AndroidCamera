package com.android.xz.camera.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.android.xz.camera.Camera2Manager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.YUVFormat;
import com.android.xz.camera.callback.PreviewBufferCallback;
import com.android.xz.encoder.BufferMovieEncoder;
import com.android.xz.encoder.MediaRecordListener;
import com.android.xz.util.Logs;
import com.android.xz.camera.view.base.BaseSurfaceView;

/**
 * 适用Camera2的SurfaceView
 *
 * @author xiaozhi
 * @since 2024/8/22
 */
public class Camera2SurfaceView extends BaseSurfaceView {

    /**
     * 使用Buffer录制视频类
     */
    private BufferMovieEncoder mEncoder;

    public Camera2SurfaceView(Context context) {
        super(context);
    }

    public Camera2SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Camera2SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
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

        float ratio;
        if (width > height) {
            ratio = height * 1.0f / width;
        } else {
            ratio = width * 1.0f / height;
        }
        if (ratio == mPreviewSize.getHeight() * 1f / mPreviewSize.getWidth()) {
            Logs.i(TAG, "startPreview1");
            getCameraManager().startPreview(holder);
        }
    }

    @Override
    public void onOpen() {
        Logs.v(TAG, "onOpen.");
        mPreviewSize = getCameraManager().getPreviewSize();
        getSurfaceHolder().setFixedSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        float ratio = mSurfaceHeight * 1.0f / mSurfaceWidth;
        getCameraManager().addPreviewBufferCallback(mPreviewBufferCallback);
        if (ratio == mPreviewSize.getHeight() * 1f / mPreviewSize.getWidth()) {
            Logs.i(TAG, "startPreview2");
            getCameraManager().startPreview(getSurfaceHolder());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRecord();
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        return new Camera2Manager(context);
    }

    private PreviewBufferCallback mPreviewBufferCallback = new PreviewBufferCallback() {
        @Override
        public void onPreviewBufferFrame(byte[] data, int width, int height, YUVFormat format) {
            if (mEncoder != null) {
                mEncoder.encode(data, format);
            }
        }
    };

    /**
     * 开始录制视频
     */
    public void startRecord() {
        if (!getCameraManager().isOpen()) {
            return;
        }
        mEncoder.setRecordListener(mRecordListener);
        mEncoder.startRecord(getCameraManager().getOrientation(), getCameraManager().getPreviewSize());
    }

    /**
     * 停止录制视频
     */
    public void stopRecord() {
        mEncoder.stopRecord();
    }

    private MediaRecordListener mRecordListener;

    public void setRecordListener(MediaRecordListener recordListener) {
        mRecordListener = recordListener;
    }
}
