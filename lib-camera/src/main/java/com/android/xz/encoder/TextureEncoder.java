package com.android.xz.encoder;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.os.Handler;

import java.io.File;

public abstract class TextureEncoder {

    protected static final int MSG_START_RECORDING = 0;
    protected static final int MSG_STOP_RECORDING = 1;
    protected static final int MSG_FRAME_AVAILABLE = 2;
    protected static final int MSG_SET_TEXTURE_ID = 3;
    protected static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    protected static final int MSG_QUIT = 5;

    protected Context mContext;
    protected Handler mUIHandler;
    protected MediaRecordListener mRecordListener;

    public TextureEncoder(Context context) {
        mContext = context;
        mUIHandler = new Handler(mContext.getMainLooper());
    }

    /**
     * Encoder configuration.
     * <p>
     * Object is immutable, which means we can safely pass it between threads without
     * explicit synchronization (and don't need to worry about it getting tweaked out from
     * under us).
     * <p>
     * TODO: make frame rate and iframe interval configurable?  Maybe use builder pattern
     *       with reasonable defaults for those and bit rate.
     */
    public static class EncoderConfig {
        final File mOutputFile;
        final int mWidth;
        final int mHeight;
        final int mBitRate;
        final EGLContext mEglContext;

        public EncoderConfig(File outputFile, int width, int height, int bitRate,
                             EGLContext sharedEglContext) {
            mOutputFile = outputFile;
            mWidth = width;
            mHeight = height;
            mBitRate = bitRate;
            mEglContext = sharedEglContext;
        }

        @Override
        public String toString() {
            return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate +
                    " to '" + mOutputFile.toString() + "' ctxt=" + mEglContext;
        }
    }

    public abstract void setRecordListener(MediaRecordListener recordListener);

    public abstract void startRecord(EncoderConfig config);

    public abstract void stopRecord();

    public abstract boolean isRecording();

    public abstract void updateSharedContext(EGLContext sharedContext);

    public abstract void frameAvailable(SurfaceTexture st);

    public abstract void setTextureId(int id);
}
