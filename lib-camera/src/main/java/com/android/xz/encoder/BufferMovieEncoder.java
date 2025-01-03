package com.android.xz.encoder;

import android.content.Context;
import android.media.MediaCodecInfo;
import android.os.Handler;
import android.util.Log;
import android.util.Size;

import com.android.xz.camera.YUVFormat;
import com.android.xz.util.ImageUtils;
import com.android.xz.util.Logs;
import com.android.xz.util.YUVUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * 对Camera预览NV21数据编码
 *
 * @author xiaozhi
 * @since 2024/8/30
 */
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

    /**
     * 开始录制
     *
     * @param orientation 编码数据方向
     * @param size        编码视频预览尺寸，通Camera的预览尺寸
     */
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
                String path;
                @Override
                public void onPrepared(MediaEncoder encoder) {
                    Logs.i(TAG, "onPrepared.");
                    mEncoder = (MediaVideoBufferEncoder) encoder;
                    path = mEncoder.getOutputPath();
                    if (mRecordListener != null) {
                        mRecordListener.onStart();
                    }
                }

                @Override
                public void onStopped(MediaEncoder encoder) {
                    Logs.i(TAG, "onStopped");
                    mUIHandler.post(() -> {
                        if (mRecordListener != null) {
                            mRecordListener.onStopped(path);
                        }
                    });
                }
            });
            // for audio capturing
            new MediaAudioEncoder(mContext, muxerWrapper, new MediaEncoder.MediaEncoderListener() {
                @Override
                public void onPrepared(MediaEncoder encoder) {

                }

                @Override
                public void onStopped(MediaEncoder encoder) {

                }
            });

            muxerWrapper.prepare();
            muxerWrapper.startRecording();
            mMuxerWrapper = muxerWrapper;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        final MediaMuxerWrapper muxerWrapper = mMuxerWrapper;
        mMuxerWrapper = null;
        mEncoder = null;
        if (muxerWrapper != null) {
            muxerWrapper.stopRecording();
        }
    }

    /**
     * 编码数据
     *
     * @param data nv21
     */
    public void encode(byte[] data) {
        encode(data, YUVFormat.NV21);
    }

    /**
     * 编码数据
     *
     * @param data YUV420
     */
    public void encode(byte[] data, YUVFormat yuvFormat) {
        if (mEncoder != null) {
            int mColorFormat = mEncoder.getColorFormat();
            byte[] encodeData = null;
            long start = System.currentTimeMillis();
            if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                    || mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar) { // 19, 20：I420
                if (yuvFormat == YUVFormat.NV21) {
                    YUVUtils.nativeNV21ToI420(data, mSize.getWidth(), mSize.getHeight(), mTempData);
                    encodeData = mTempData;
                } else {
                    encodeData = data;
                }
            } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                    || mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar
                    || mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar) { // 21, 39：NV12
                // 使用C层转换最快
                if (yuvFormat == YUVFormat.NV21) {
                    YUVUtils.nativeNV21ToNV12(data, mSize.getWidth(), mSize.getHeight(), mTempData);
                } else {
                    YUVUtils.nativeI420ToNV12(data, mSize.getWidth(), mSize.getHeight(), mTempData);
                }
                encodeData = mTempData;
            } else if (mColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar) {// 2141391872：NV21
                if (yuvFormat == YUVFormat.NV21) {
                    encodeData = data;
                } else {
                    YUVUtils.nativeI420ToNV21(data, mSize.getWidth(), mSize.getHeight(), mTempData);
                    encodeData = mTempData;
                }
            }
//            Log.i(TAG, "耗时：" + (System.currentTimeMillis() - start) + "ms");
            mEncoder.frameAvailableSoon();
            ByteBuffer buffer = ByteBuffer.wrap(encodeData);
            mEncoder.encode(buffer);
        }
    }
}
