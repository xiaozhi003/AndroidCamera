package com.android.xz.camera.view.base;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;

import com.android.xz.encoder.TextureMovieEncoder2;
import com.android.xz.gles.EglCore;
import com.android.xz.gles.GLESUtils;
import com.android.xz.gles.WindowSurface;
import com.android.xz.gles.filiter.CameraFilter;
import com.android.xz.gles.filiter.Texture2DFilter;
import com.android.xz.util.ImageUtils;
import com.android.xz.util.Logs;

import java.io.File;
import java.util.Date;

/**
 * OpenGL ES渲染Camera预览数据的线程
 *
 * @author xiaozhi
 * @since 2024/8/22
 */
public class RenderThread extends Thread {

    private static final String TAG = RenderThread.class.getSimpleName();
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;

    // Used to wait for the thread to start.
    private Object mStartLock = new Object();
    private Context mContext;
    private boolean mReady = false;
    private Handler mMainHandler;
    private RenderHandler mHandler;

    // width/height of the incoming camera preview frames
    private SurfaceTexture mPreviewTexture;
    private int mTextureId;

    private float[] mDisplayProjectionMatrix = new float[16];
    private EglCore mEglCore;
    private WindowSurface mWindowSurface;
    private CameraFilter mCameraFilter;
    private CameraFilter mFBOFilter;
    private Texture2DFilter mScreenFilter;
    private int mCameraPreviewWidth, mCameraPreviewHeight;
    private boolean mSizeUpdated;

    private File mOutputFile;
    private TextureMovieEncoder2 mVideoEncoder;
    private boolean mRecordingEnabled;
    private int mRecordingStatus;
    private long mVideoMillis;

    public RenderThread(Context context, TextureMovieEncoder2 textureMovieEncoder) {
        super("Renderer Thread");
        mContext = context;
        mMainHandler = new Handler(mContext.getMainLooper());
        mVideoEncoder = textureMovieEncoder;
        mSizeUpdated = false;
        mCameraFilter = new CameraFilter();
        mFBOFilter = new CameraFilter();
        mFBOFilter.setFBO(true);
        mScreenFilter = new Texture2DFilter();
    }

    @Override
    public void run() {
        super.run();
        Logs.i(TAG, "Render Thread start.");
        Looper.prepare();

        // We need to create the Handler before reporting ready.
        mHandler = new RenderHandler(this);
        synchronized (mStartLock) {
            mReady = true;
            mStartLock.notify();    // signal waitUntilReady()
        }

        // Prepare EGL and open the camera before we start handling messages.
        mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);
        Logs.i(TAG, "Egl version:" + mEglCore.getGlVersion());

        Looper.loop();
        mHandler = null;
        releaseGl();
        mEglCore.release();

        Logs.v(TAG, "Render Thread exit.");
    }

    /**
     * Notifies the renderer that we want to stop or start recording.
     */
    public void changeRecordingState(boolean isRecording) {
        Log.d(TAG, "changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
        mRecordingEnabled = isRecording;
        if (!mRecordingEnabled) {
            notifyStopRecord();
        }
    }

    public void notifyStopRecord() {
        if (mVideoEncoder != null && mVideoEncoder.isRecording()) {
            mVideoEncoder.stopRecord();
            mRecordingStatus = RECORDING_OFF;
        }
    }

    /**
     * Waits until the render thread is ready to receive messages.
     * <p>
     * Call from the UI thread.
     */
    public void waitUntilReady() {
        synchronized (mStartLock) {
            while (!mReady) {
                try {
                    mStartLock.wait();
                } catch (InterruptedException ie) { /* not expected */ }
            }
        }
    }

    /**
     * Shuts everything down.
     */
    public void shutdown() {
        Log.d(TAG, "shutdown");
        Looper.myLooper().quit();
    }

    public RenderHandler getHandler() {
        return mHandler;
    }

    public void surfaceAvailable(Object surface) {
        mRecordingEnabled = mVideoEncoder.isRecording();
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            mRecordingStatus = RECORDING_OFF;
        }
        if (surface instanceof SurfaceHolder) {
            mWindowSurface = new WindowSurface(mEglCore, ((SurfaceHolder) surface).getSurface(), false);
        } else if (surface instanceof SurfaceTexture) {
            mWindowSurface = new WindowSurface(mEglCore, (SurfaceTexture) surface);
        }

        mWindowSurface.makeCurrent();

        // Set up the texture blitter that will be used for on-screen display.  This
        // is *not* applied to the recording, because that uses a separate shader.

        mTextureId = GLESUtils.createOESTexture();
        mPreviewTexture = new SurfaceTexture(mTextureId);
        mCameraFilter.surfaceCreated();
        mFBOFilter.surfaceCreated();
        mScreenFilter.surfaceCreated();

        mMainHandler.post(() -> {
            if (mGLSurfaceTextureCallback != null) {
                mGLSurfaceTextureCallback.onGLSurfaceCreated(mPreviewTexture);
            }
        });
    }

    public void surfaceChanged(int width, int height) {
        mCameraFilter.surfaceChanged(width, height);
        mScreenFilter.surfaceChanged(width, height);
    }

    public void surfaceDestroyed() {
        // In practice this never appears to be called -- the activity is always paused
        // before the surface is destroyed.  In theory it could be called though.
        Log.d(TAG, "RenderThread surfaceDestroyed");
        releaseGl();
    }

    public void frameAvailable() {
        draw();
    }

    public void draw() {
        if (mPreviewTexture == null) return;
        if (mWindowSurface == null) return;

        mPreviewTexture.updateTexImage();
        GLESUtils.checkGlError("draw start");

        // If the recording state is changing, take care of it here.  Ideally we wouldn't
        // be doing all this in onDrawFrame(), but the EGLContext sharing with GLSurfaceView
        // makes it hard to do elsewhere.
        if (mRecordingEnabled) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    Log.d(TAG, "START recording");
                    // 开始录制前删除之前的视频文件
                    String name = "VID_" + ImageUtils.DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ".mp4";
                    mOutputFile = new File(ImageUtils.getVideoPath(), name);
                    // start recording
                    mVideoEncoder.startRecord(new TextureMovieEncoder2.EncoderConfig(
                            mOutputFile, mCameraPreviewHeight, mCameraPreviewWidth, mCameraPreviewWidth * mCameraPreviewHeight * 10, mEglCore));
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    Log.d(TAG, "RESUME recording");
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        } else {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    // stop recording
                    Log.d(TAG, "STOP recording");
                    mVideoEncoder.stopRecord();
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        }

        if (mCameraPreviewWidth <= 0 || mCameraPreviewHeight <= 0) {
            return;
        }
        if (mSizeUpdated) {
            mSizeUpdated = false;
        }

        boolean swapResult;
        if (!mVideoEncoder.isRecording()) {
            swapResult = drawScreen();
        } else {
            swapResult = drawRecord();
        }

        if (!swapResult) {
            // This can happen if the Activity stops without waiting for us to halt.
            Log.w(TAG, "swapBuffers failed, killing renderer thread");
            shutdown();
        }
    }

    /**
     * 绘制到屏幕Surface中，用来在界面上显示
     */
    private boolean drawScreen() {
        mPreviewTexture.getTransformMatrix(mDisplayProjectionMatrix);
        GLES20.glViewport(0, 0, mWindowSurface.getWidth(), mWindowSurface.getHeight());
        mCameraFilter.draw(mTextureId, mDisplayProjectionMatrix);
        return mWindowSurface.swapBuffers();
    }

    /**
     * 绘制到视频Surface中，用来录制视频
     */
    private boolean drawRecord() {
        boolean swapResult;
        WindowSurface recordWindowSurface = mVideoEncoder.getInputWindowSurface();

        if (recordWindowSurface != null && mEglCore.getGlVersion() >= 3) { // draw blit framebuffer
            swapResult = drawBlitFrameBuffer(recordWindowSurface);
        } else if (recordWindowSurface != null) { // draw twice or draw FBO
            // 两种方式任选其一
            swapResult = drawTwice(recordWindowSurface);
//            swapResult = drawFBO(recordWindowSurface);
        } else {
            swapResult = drawScreen();
        }
        return swapResult;
    }

    /**
     * BlitFramebuffer方式
     *
     * @param recordWindowSurface
     * @return
     */
    private boolean drawBlitFrameBuffer(WindowSurface recordWindowSurface) {
        boolean swapResult;
        // 先绘制到屏幕上
        mPreviewTexture.getTransformMatrix(mDisplayProjectionMatrix);
        GLES20.glViewport(0, 0, mWindowSurface.getWidth(), mWindowSurface.getHeight());
        mCameraFilter.draw(mTextureId, mDisplayProjectionMatrix);

        mVideoEncoder.frameAvailable();
        // 把屏幕Surface渲染数据拷贝到视频Surface中
        // 该中方式的效率是最高的，一次渲染输出给多个目标，但是只有OpenGL3.0才有该方法
        recordWindowSurface.makeCurrentReadFrom(mWindowSurface);
        GLES30.glBlitFramebuffer(
                0, 0, mWindowSurface.getWidth(), mWindowSurface.getHeight(),
                0, 0, mVideoEncoder.getVideoWidth(), mVideoEncoder.getVideoHeight(),
                GLES30.GL_COLOR_BUFFER_BIT, GLES30.GL_NEAREST);
        int err;
        if ((err = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.w(TAG, "ERROR: glBlitFramebuffer failed: 0x" +
                    Integer.toHexString(err));
        }
        recordWindowSurface.setPresentationTime(mPreviewTexture.getTimestamp());
        recordWindowSurface.swapBuffers();

        // 切换为屏幕Surface
        mWindowSurface.makeCurrent();
        swapResult = mWindowSurface.swapBuffers();
        return swapResult;
    }

    /**
     * 二次渲染方式
     *
     * @param recordWindowSurface
     * @return
     */
    private boolean drawTwice(WindowSurface recordWindowSurface) {
        boolean swapResult;
        // 先绘制到屏幕上
        mPreviewTexture.getTransformMatrix(mDisplayProjectionMatrix);
        GLES20.glViewport(0, 0, mWindowSurface.getWidth(), mWindowSurface.getHeight());
        mCameraFilter.draw(mTextureId, mDisplayProjectionMatrix);
        swapResult = mWindowSurface.swapBuffers();

        // 再绘制到视频Surface中
        mVideoEncoder.frameAvailable();
        recordWindowSurface.makeCurrent();
        GLES20.glViewport(0, 0,
                mVideoEncoder.getVideoWidth(), mVideoEncoder.getVideoHeight());
        mCameraFilter.draw(mTextureId, mDisplayProjectionMatrix);
        recordWindowSurface.setPresentationTime(mPreviewTexture.getTimestamp());
        recordWindowSurface.swapBuffers();

        // Restore
        mWindowSurface.makeCurrent();
        return swapResult;
    }


    /**
     * 离屏渲染
     *
     * @param recordWindowSurface
     * @return
     */
    private boolean drawFBO(WindowSurface recordWindowSurface) {
        boolean swapResult;
        // 将数据绘制到FBO Buffer中
        mPreviewTexture.getTransformMatrix(mDisplayProjectionMatrix);
        GLES20.glViewport(0, 0, mCameraPreviewWidth, mCameraPreviewHeight);
        int fboId = mFBOFilter.draw(mTextureId, mDisplayProjectionMatrix);

        // 将离屏FrameBuffer绘制到视频Surface中
        mVideoEncoder.frameAvailable();
        recordWindowSurface.makeCurrent();
        GLES20.glViewport(0, 0,
                mVideoEncoder.getVideoWidth(), mVideoEncoder.getVideoHeight());
        mScreenFilter.draw(fboId, mDisplayProjectionMatrix);
        recordWindowSurface.setPresentationTime(mPreviewTexture.getTimestamp());
        recordWindowSurface.swapBuffers();

        // 将离屏FrameBuffer绘制到屏幕Surface中
        mWindowSurface.makeCurrent();
        GLES20.glViewport(0, 0, mWindowSurface.getWidth(), mWindowSurface.getHeight());
        mScreenFilter.draw(fboId, mDisplayProjectionMatrix);
        swapResult = mWindowSurface.swapBuffers();

        return swapResult;
    }

    public void setCameraPreviewSize(int width, int height) {
        mCameraPreviewWidth = width;
        mCameraPreviewHeight = height;
        mSizeUpdated = true;
        mFBOFilter.surfaceChanged(width, height);
    }

    /**
     * Releases most of the GL resources we currently hold (anything allocated by
     * surfaceAvailable()).
     * <p>
     * Does not release EglCore.
     */
    private void releaseGl() {
        GLESUtils.checkGlError("releaseGl start");

        if (mWindowSurface != null) {
            mWindowSurface.release();
            mWindowSurface = null;
        }
        if (mPreviewTexture != null) {
            mPreviewTexture.release();
            mPreviewTexture = null;
        }
        if (mCameraFilter != null) {
            mCameraFilter.release();
        }
        if (mFBOFilter != null) {
            mFBOFilter.release();
        }
        if (mScreenFilter != null) {
            mScreenFilter.release();
        }
        GLESUtils.checkGlError("releaseGl done");

        mEglCore.makeNothingCurrent();
    }

    private GLSurfaceTextureCallback mGLSurfaceTextureCallback;

    public void setGLSurfaceTextureCallback(GLSurfaceTextureCallback GLSurfaceTextureCallback) {
        mGLSurfaceTextureCallback = GLSurfaceTextureCallback;
    }

    public interface GLSurfaceTextureCallback {
        void onGLSurfaceCreated(SurfaceTexture surfaceTexture);
    }
}
