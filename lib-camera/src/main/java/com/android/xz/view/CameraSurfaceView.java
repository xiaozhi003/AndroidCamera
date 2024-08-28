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

    private MediaVideoBufferEncoder mEncoder;
    private MediaMuxerWrapper mMuxerWrapper;
    private String mVideoPath;
    private byte[] mTempData;

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
                int mColorFormat = mEncoder.getColorFormat();
                byte[] encodeData = null;
                long start = System.currentTimeMillis();
                if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                        || mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar) { // 19, 20：I420
                    YUVUtils.nativeNV21ToI420(data, mPreviewSize.getWidth(), mPreviewSize.getHeight(), mTempData);
                    encodeData = mTempData;
                } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                        || mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar
                        || mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar) { // 21, 39：NV12
                    // 使用C层转换最快
                    YUVUtils.nativeNV21ToNV12(data, mPreviewSize.getWidth(), mPreviewSize.getHeight(), mTempData);
                    encodeData = mTempData;
                } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar) {// 2141391872：NV21
                    encodeData = data;
                }
                long end = System.currentTimeMillis();
//                Logs.i(TAG, "yuv encode:" + (end - start) + "ms");
                mEncoder.frameAvailableSoon();
                ByteBuffer buffer = ByteBuffer.wrap(encodeData);
//                Logs.i(TAG, "video wrap:" + (System.currentTimeMillis() - end) + "ms");
                mEncoder.encode(buffer);
                Logs.i(TAG, "video encode:" + (System.currentTimeMillis() - end) + "ms");
            }
        }
    };

    public void startRecord() {
        if (!getCameraManager().isOpen()) {
            return;
        }
        try {
            if (mMuxerWrapper != null) return;
            if (mTempData == null) {
                mTempData = new byte[mPreviewSize.getWidth() * mPreviewSize.getHeight() * 3 / 2];
            }
            String name = "VID_" + ImageUtils.DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".mp4";
            File outputFile = new File(ImageUtils.getGalleryPath(), name);
            final MediaMuxerWrapper muxerWrapper = new MediaMuxerWrapper(".mp4", outputFile);
            muxerWrapper.setOrientationHint(getCameraManager().getOrientation());
            new MediaVideoBufferEncoder(muxerWrapper, mPreviewSize.getWidth(), mPreviewSize.getHeight(), new MediaEncoder.MediaEncoderListener() {
                @Override
                public void onPrepared(MediaEncoder encoder) {
                    Logs.i(TAG, "onPrepared.");
                    mEncoder = (MediaVideoBufferEncoder) encoder;
                    if (mRecordListener != null) {
                        mRecordListener.onStart();
                    }
                }

                @Override
                public void onStopped(MediaEncoder encoder) {
                    Logs.i(TAG, "onStopped");
                    mVideoPath = encoder.getOutputPath();
                    CameraSurfaceView.this.post(() -> {
                        if (mRecordListener != null) {
                            mRecordListener.onStopped(mVideoPath);
                        }
                    });
                }
            });
            muxerWrapper.prepare();
            muxerWrapper.startRecording();
            mMuxerWrapper = muxerWrapper;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecord() {
        final MediaMuxerWrapper muxerWrapper = mMuxerWrapper;
        mMuxerWrapper = null;
        mEncoder = null;
        if (muxerWrapper != null) {
            muxerWrapper.stopRecording();
        }
    }

    private MediaRecordListener mRecordListener;

    public void setRecordListener(MediaRecordListener recordListener) {
        mRecordListener = recordListener;
    }
}
