package com.android.xz.encoder;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.os.Build;
import android.os.Handler;
import android.util.Size;

import com.android.xz.util.ImageUtils;
import com.android.xz.util.Logs;
import com.android.xz.util.YUVUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Date;

public class BufferMovieEncoder {

    private static final String TAG = BufferMovieEncoder.class.getSimpleName();
    private MediaVideoBufferEncoder mEncoder;
    private MediaMuxerWrapper mMuxerWrapper;
    private MediaRecordListener mRecordListener;
    private Context mContext;
    private Handler mUIHandler;
    private byte[] mTempData;
    private Size mSize;
    private int mOrientation;

    public BufferMovieEncoder(Context context) {
        mContext = context;
        mUIHandler = new Handler(mContext.getMainLooper());
    }

    public void setRecordListener(MediaRecordListener recordListener) {
        mRecordListener = recordListener;
    }

    public void startRecord(int orientation, Size size) {
        mOrientation = orientation;
        mSize = size;
        try {
            if (mMuxerWrapper != null) return;
            if (mTempData == null) {
                mTempData = new byte[mSize.getWidth() * mSize.getHeight() * 3 / 2];
            }
            String name = "VID_" + ImageUtils.DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".mp4";
            File outputFile = new File(ImageUtils.getVideoPath(), name);

            final MediaMuxerWrapper muxerWrapper = new MediaMuxerWrapper(".mp4", outputFile);
            muxerWrapper.setOrientationHint(mOrientation);
            new MediaVideoBufferEncoder(muxerWrapper, mSize.getWidth(), mSize.getHeight(), new MediaEncoder.MediaEncoderListener() {
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
                    String outputPath = encoder.getOutputPath();
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mRecordListener != null) {
                                mRecordListener.onStopped(outputPath);
                            }
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

    public void encode(byte[] data) {
        if (mEncoder != null) {
            int mColorFormat = mEncoder.getColorFormat();
            byte[] encodeData = null;
            long start = System.currentTimeMillis();
            if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                    || mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar) { // 19, 20：I420
                YUVUtils.nativeNV21ToI420(data, mSize.getWidth(), mSize.getHeight(), mTempData);
                encodeData = mTempData;
            } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                    || mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar
                    || mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar) { // 21, 39：NV12
                // 使用C层转换最快
                YUVUtils.nativeNV21ToNV12(data, mSize.getWidth(), mSize.getHeight(), mTempData);
                encodeData = mTempData;
            } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar) {// 2141391872：NV21
                encodeData = data;
            }
            long end = System.currentTimeMillis();
            mEncoder.frameAvailableSoon();
            ByteBuffer buffer = ByteBuffer.wrap(encodeData);
            mEncoder.encode(buffer);
//            Logs.i(TAG, "video encode:" + (System.currentTimeMillis() - end) + "ms");
        }
    }
}
