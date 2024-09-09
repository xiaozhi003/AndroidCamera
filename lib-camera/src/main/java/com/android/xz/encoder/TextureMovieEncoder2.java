/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.xz.encoder;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.android.xz.gles.EglCore;
import com.android.xz.gles.WindowSurface;
import com.android.xz.util.Logs;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Encode a movie from frames rendered from an external texture image.
 * <p>
 * The object wraps an encoder running on a dedicated thread.  The various control messages
 * may be sent from arbitrary threads (typically the app UI thread).  The encoder thread
 * manages both sides of the encoder (feeding and draining); the only external input is
 * the GL texture.
 * <p>
 * The design is complicated slightly by the need to create an EGL context that shares state
 * with a view that gets restarted if (say) the device orientation changes.  When the view
 * in question is a GLSurfaceView, we don't have full control over the EGL context creation
 * on that side, so we have to bend a bit backwards here.
 * <p>
 * To use:
 * <ul>
 * <li>create TextureMovieEncoder object
 * <li>create an EncoderConfig
 * <li>call TextureMovieEncoder#startRecording() with the config
 * <li>call TextureMovieEncoder#setTextureId() with the texture object that receives frames
 * <li>for each frame, after latching it with SurfaceTexture#updateTexImage(),
 *     call TextureMovieEncoder#frameAvailable().
 * </ul>
 * <p>
 * TODO: tweak the API (esp. textureId) so it's less awkward for simple use cases.
 */
public class TextureMovieEncoder2 {
    private static final String TAG = TextureMovieEncoder2.class.getSimpleName();
    private static final boolean VERBOSE = false;

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private MediaEncoder mEncoder;
    private MediaMuxerWrapper mMuxerWrapper;

    // ----- accessed by multiple threads -----
    private MediaRecordListener mRecordListener;
    private Context mContext;
    private Handler mUIHandler;

    private boolean isRecording;

    private int mVideoWidth;
    private int mVideoHeight;

    public TextureMovieEncoder2(Context context) {
        mContext = context;
        mUIHandler = new Handler(context.getMainLooper());
    }

    /**
     * 开始录制视频
     *
     * @param encoderConfig
     */
    public void startRecord(EncoderConfig encoderConfig) {
        Logs.i(TAG, "startRecord.");
        if (mMuxerWrapper != null) return;
        MediaSurfaceEncoder mediaSurfaceEncoder;
        try {
            mVideoWidth = encoderConfig.mWidth;
            mVideoHeight = encoderConfig.mHeight;
            mMuxerWrapper = new MediaMuxerWrapper(".mp4", encoderConfig.mOutputFile);
            mediaSurfaceEncoder = new MediaSurfaceEncoder(mMuxerWrapper, encoderConfig.mWidth, encoderConfig.mHeight, new MediaEncoder.MediaEncoderListener() {
                @Override
                public void onPrepared(MediaEncoder encoder) {
                    Logs.i(TAG, "onPrepared.");
                    isRecording = true;
                    mEncoder = encoder;
                    mUIHandler.post(() -> {
                        if (mRecordListener != null) {
                            mRecordListener.onStart();
                        }
                    });
                }

                @Override
                public void onStopped(MediaEncoder encoder) {
                    Logs.i(TAG, "onStopped.");
                    mUIHandler.post(() -> {
                        if (mRecordListener != null) {
                            mRecordListener.onStopped(encoder.getOutputPath());
                        }
                    });
                }
            });
            mMuxerWrapper.prepare();
            mMuxerWrapper.startRecording();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mEglCore = encoderConfig.mCore;
        mInputWindowSurface = new WindowSurface(mEglCore, mediaSurfaceEncoder.getInputSurface(), true);
    }

    /**
     * 停止录制视频
     */
    public void stopRecord() {
        Log.d(TAG, "stopRecord.");
        isRecording = false;
        MediaMuxerWrapper muxerWrapper = mMuxerWrapper;
        mMuxerWrapper = null;
        mEncoder = null;
        if (muxerWrapper != null) {
            muxerWrapper.stopRecording();
        }
        releaseEncoder();
    }

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        mVideoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        mVideoHeight = videoHeight;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void frameAvailable() {
        if (mEncoder != null) {
            mEncoder.frameAvailableSoon();
        }
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
        final EglCore mCore;

        public EncoderConfig(File outputFile, int width, int height, int bitRate,
                             EglCore eglCore) {
            mOutputFile = outputFile;
            mWidth = width;
            mHeight = height;
            mBitRate = bitRate;
            mCore = eglCore;
        }

        @Override
        public String toString() {
            return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate +
                    " to '" + mOutputFile.toString() + "' ctxt=" + mCore;
        }
    }

    public void setRecordListener(MediaRecordListener recordListener) {
        mRecordListener = recordListener;
    }

    public WindowSurface getInputWindowSurface() {
        return mInputWindowSurface;
    }

    private void releaseEncoder() {
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
    }
}
