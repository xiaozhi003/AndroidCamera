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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.android.xz.gles.EglCore;
import com.android.xz.gles.WindowSurface;
import com.android.xz.util.Logs;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * 该编码器主要用来获取MediaCodec创建的Surface
 * Renderer线程将纹理刷新到该Surface中进行视频编码
 */
public class TextureMovieEncoder2 implements Runnable {
    private static final String TAG = TextureMovieEncoder2.class.getSimpleName();

    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_QUIT = 5;

    private Object mSync = new Object();

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private MediaEncoder mEncoder;
    private MediaMuxerWrapper mMuxerWrapper;

    private volatile EncoderHandler mHandler;

    private Object mReadyFence = new Object();      // guards ready/running
    private boolean mReady;
    private boolean mRunning;
    private MediaRecordListener mRecordListener;
    private Context mContext;
    private Handler mUIHandler;

    private boolean isRecording;

    private int mVideoWidth;
    private int mVideoHeight;

    public TextureMovieEncoder2(Context context) {
        mContext = context;
        mUIHandler = new Handler(mContext.getMainLooper());
    }

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

    /**
     * Tells the video recorder to start recording.  (Call from non-encoder thread.)
     * <p>
     * Creates a new thread, which will create an encoder using the provided configuration.
     * <p>
     * Returns after the recorder thread has started and is ready to accept Messages.  The
     * encoder may not yet be fully configured.
     */
    public void startRecord(EncoderConfig config) {
        Logs.i(TAG, "startRecord.");
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "TextureMovieEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }

    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     * <p>
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     * <p>
     * TODO: have the encoder thread invoke a callback on the UI thread just before it shuts down
     * so we can provide reasonable status UI (and let the caller know that movie encoding
     * has completed).
     */
    public void stopRecord() {
        Log.d(TAG, "stopRecord.");
        isRecording = false;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
        mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
    }

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void frameAvailable() {
        if (mEncoder != null) {
            mEncoder.frameAvailableSoon();
        }
    }

    public void setRecordListener(MediaRecordListener recordListener) {
        mRecordListener = recordListener;
    }

    public WindowSurface getInputWindowSurface() {
        return mInputWindowSurface;
    }

    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<TextureMovieEncoder2> mWeakEncoder;


        public EncoderHandler(TextureMovieEncoder2 encoder) {
            mWeakEncoder = new WeakReference<>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            TextureMovieEncoder2 encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((TextureMovieEncoder2.EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    private void handleStartRecording(EncoderConfig encoderConfig) {
        if (mMuxerWrapper != null) return;
        MediaSurfaceEncoder mediaSurfaceEncoder;
        try {
            mVideoWidth = encoderConfig.mWidth;
            mVideoHeight = encoderConfig.mHeight;
            mMuxerWrapper = new MediaMuxerWrapper(".mp4", encoderConfig.mOutputFile);
            mediaSurfaceEncoder = new MediaSurfaceEncoder(mMuxerWrapper, encoderConfig.mWidth, encoderConfig.mHeight, new MediaEncoder.MediaEncoderListener() {
                String path;
                @Override
                public void onPrepared(MediaEncoder encoder) {
                    Logs.i(TAG, "onPrepared.");
                    isRecording = true;
                    mEncoder = encoder;
                    path = mEncoder.getOutputPath();
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
                            mRecordListener.onStopped(path);
                        }
                    });
                }
            });
            mMuxerWrapper.prepare();
            mMuxerWrapper.startRecording();

            mEglCore = encoderConfig.mCore;
            mInputWindowSurface = new WindowSurface(mEglCore, mediaSurfaceEncoder.getInputSurface(), true);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private void handleStopRecording() {
        isRecording = false;
        MediaMuxerWrapper muxerWrapper = mMuxerWrapper;
        mMuxerWrapper = null;
        mEncoder = null;
        if (muxerWrapper != null) {
            muxerWrapper.stopRecording();
        }
        releaseEncoder();
    }

    private void releaseEncoder() {
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
    }
}
