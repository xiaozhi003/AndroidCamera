package com.android.xz.camera.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.android.xz.camera.CameraManager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.callback.PreviewBufferCallback;
import com.android.xz.camera.view.base.BaseSurfaceView;
import com.android.xz.encoder.BufferMovieEncoder;
import com.android.xz.encoder.MediaRecordListener;
import com.android.xz.util.Logs;

/**
 * 适用Camera的SurfaceView预览
 *
 * @author xiaozhi
 * @since 2024/8/22
 */
public class CameraSurfaceView extends BaseSurfaceView {

    /**
     * 使用Buffer录制视频类
     */
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
