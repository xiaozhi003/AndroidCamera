package com.android.xz.view.base;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.callback.CameraCallback;
import com.android.xz.encoder.MediaRecordListener;
import com.android.xz.encoder.TextureMovieEncoder;
import com.android.xz.gles.FullFrameRect;
import com.android.xz.gles.Texture2dProgram;
import com.android.xz.util.ImageUtils;
import com.android.xz.util.Logs;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 摄像头预览GLSurfaceView
 * Created by wangzhi on 2024/8/22.
 */
public abstract class BaseGLSurfaceView extends GLSurfaceView implements SurfaceTexture.OnFrameAvailableListener, CameraCallback, BaseCameraView {

    private static final String TAG = BaseGLSurfaceView.class.getSimpleName();

    private Context mContext;
    private SurfaceTexture mSurfaceTexture;
    private CameraHandler mCameraHandler;
    private boolean hasSurface; // 是否存在摄像头显示层
    private ICameraManager mCameraManager;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private int mGLSurfaceWidth;
    private int mGLSurfaceHeight;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private CameraSurfaceRenderer mRenderer;

    private TextureMovieEncoder mMovieEncoder;

    public BaseGLSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public BaseGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mCameraHandler = new CameraHandler(this);

        mCameraManager = createCameraManager(context);
        mCameraManager.setCameraCallback(this);

        setEGLContextClientVersion(2);
        mMovieEncoder = new TextureMovieEncoder(context);
        mRenderer = new CameraSurfaceRenderer(mCameraHandler, mMovieEncoder);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void startRecord() {
        queueEvent(() -> mRenderer.changeRecordingState(true));
    }

    public void stopRecord() {
        if (mMovieEncoder.isRecording()) {
            queueEvent(() -> mRenderer.changeRecordingState(false));
        }
    }

    public abstract ICameraManager createCameraManager(Context context);

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    private void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, width * 4 / 3);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    /**
     * Connects the SurfaceTexture to the Camera preview output, and starts the preview.
     */
    private void handleSetSurfaceTexture(SurfaceTexture st) {
        Logs.i(TAG, "handleSetSurfaceTexture.");
        mSurfaceTexture = st;
        hasSurface = true;
        mSurfaceTexture.setOnFrameAvailableListener(this);
        openCamera();
    }

    /**
     * @param width
     * @param height
     */
    private void handleSurfaceChanged(int width, int height) {
        Logs.i(TAG, "handleSurfaceChanged.");
        mGLSurfaceWidth = width;
        mGLSurfaceHeight = height;
        setAspectRatio();
    }

    /**
     * 打开摄像头并预览
     */
    @Override
    public void onResume() {
        super.onResume();
        if (hasSurface) {
            // 当activity暂停，但是并未停止的时候，surface仍然存在，所以 surfaceCreated()
            // 并不会调用，需要在此处初始化摄像头
            openCamera();
        }
    }

    /**
     * 停止预览并关闭摄像头
     */
    public void onPause() {
        super.onPause();
        closeCamera();
        stopRecord();
    }

    @Override
    public void onDestroy() {
        mCameraHandler.invalidateHandler();
    }

    public ICameraManager getCameraManager() {
        return mCameraManager;
    }

    /**
     * 打开摄像头
     */
    private void openCamera() {
        if (mSurfaceTexture == null) {
            Logs.e(TAG, "mSurfaceTexture is null.");
            return;
        }
        if (mCameraManager.isOpen()) {
            Logs.w(TAG, "Camera is opened！");
            return;
        }
        mCameraManager.openCamera();
    }

    private void closeCamera() {
        queueEvent(() -> mRenderer.notifyStopRecord());
        mCameraManager.releaseCamera();
        queueEvent(() -> mRenderer.notifyPausing());
        mSurfaceTexture = null;
    }

    @Override
    public void onOpen() {
        mCameraManager.startPreview(mSurfaceTexture);
    }

    @Override
    public void onOpenError(int error, String msg) {

    }

    @Override
    public void onPreview(int previewWidth, int previewHeight) {
        Logs.i(TAG, "onPreview " + previewWidth + " " + previewHeight);
        queueEvent(() -> mRenderer.setCameraPreviewSize(previewWidth, previewHeight));
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
        setAspectRatio();
    }

    @Override
    public void onPreviewError(int error, String msg) {

    }

    @Override
    public void onClose() {

    }

    private void setAspectRatio() {
        if (mGLSurfaceWidth > mGLSurfaceHeight) {
            setAspectRatio(mPreviewWidth, mPreviewHeight);
        } else {
            setAspectRatio(mPreviewHeight, mPreviewWidth);
        }
    }

    /**
     * Handles camera operation requests from other threads.  Necessary because the Camera
     * must only be accessed from one thread.
     * <p>
     * The object is created on the UI thread, and all handlers run there.  Messages are
     * sent from other threads, using sendMessage().
     */
    static class CameraHandler extends Handler {
        public static final int MSG_SET_SURFACE_TEXTURE = 0;

        public static final int MSG_SURFACE_CHANGED = 1;

        private WeakReference<BaseGLSurfaceView> mWeakGLSurfaceView;

        public CameraHandler(BaseGLSurfaceView view) {
            mWeakGLSurfaceView = new WeakReference<>(view);
        }

        /**
         * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
         * attempts to access a stale Activity through a handler are caught.
         */
        public void invalidateHandler() {
            mWeakGLSurfaceView.clear();
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            int what = msg.what;

            BaseGLSurfaceView view = mWeakGLSurfaceView.get();
            if (view == null) {
                return;
            }

            switch (what) {
                case MSG_SET_SURFACE_TEXTURE:
                    view.handleSetSurfaceTexture((SurfaceTexture) msg.obj);
                    break;
                case MSG_SURFACE_CHANGED:
                    view.handleSurfaceChanged(msg.arg1, msg.arg2);
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }
    }

    /**
     * Renderer object for our GLSurfaceView.
     * <p>
     * Do not call any methods here directly from another thread -- use the
     * GLSurfaceView#queueEvent() call.
     */
    static class CameraSurfaceRenderer implements Renderer {

        private static final int RECORDING_OFF = 0;
        private static final int RECORDING_ON = 1;
        private static final int RECORDING_RESUMED = 2;

        private CameraHandler mCameraHandler;

        private final float[] mSTMatrix = new float[16];
        private FullFrameRect mFullScreen;
        // width/height of the incoming camera preview frames
        private boolean mIncomingSizeUpdated;
        private int mIncomingWidth;
        private int mIncomingHeight;
        private int mTextureId;
        private SurfaceTexture mSurfaceTexture;


        private File mOutputFile;
        private TextureMovieEncoder mVideoEncoder;
        private boolean mRecordingEnabled;
        private int mRecordingStatus;
        private long mVideoMillis;

        public CameraSurfaceRenderer(CameraHandler cameraHandler, TextureMovieEncoder textureMovieEncoder) {
            mCameraHandler = cameraHandler;
            mVideoEncoder = textureMovieEncoder;

            mTextureId = -1;

            mRecordingStatus = -1;
            mRecordingEnabled = false;
            mIncomingSizeUpdated = false;
            mIncomingWidth = mIncomingHeight = -1;
        }

        /**
         * Notifies the renderer that we want to stop or start recording.
         */
        public void changeRecordingState(boolean isRecording) {
            Log.d(TAG, "changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
            mRecordingEnabled = isRecording;
        }

        public void notifyStopRecord() {
            if (mVideoEncoder != null && mVideoEncoder.isRecording()) {
                mVideoEncoder.stopRecording();
                mRecordingStatus = RECORDING_OFF;
            }
        }

        /**
         * Notifies the renderer thread that the activity is pausing.
         * <p>
         * For best results, call this *after* disabling Camera preview.
         */
        public void notifyPausing() {
            if (mSurfaceTexture != null) {
                Logs.d(TAG, "renderer pausing -- releasing SurfaceTexture");
                mSurfaceTexture.release();
                mSurfaceTexture = null;
            }
            if (mFullScreen != null) {
                mFullScreen.release(false);     // assume the GLSurfaceView EGL context is about
                mFullScreen = null;             //  to be destroyed
            }
            mIncomingWidth = mIncomingHeight = -1;
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Logs.i(TAG, "onSurfaceCreated. " + Thread.currentThread().getName());
            // We're starting up or coming back.  Either way we've got a new EGLContext that will
            // need to be shared with the video encoder, so figure out if a recording is already
            // in progress.
            mRecordingEnabled = mVideoEncoder.isRecording();
            if (mRecordingEnabled) {
                mRecordingStatus = RECORDING_RESUMED;
            } else {
                mRecordingStatus = RECORDING_OFF;
            }

            // Set up the texture blitter that will be used for on-screen display.  This
            // is *not* applied to the recording, because that uses a separate shader.
            mFullScreen = new FullFrameRect(
                    new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));

            mTextureId = mFullScreen.createTextureObject();

            // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
            // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
            // available messages will arrive on the main thread.
            mSurfaceTexture = new SurfaceTexture(mTextureId);

            mCameraHandler.sendMessage(mCameraHandler.obtainMessage(CameraHandler.MSG_SET_SURFACE_TEXTURE, mSurfaceTexture));
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
            mCameraHandler.sendMessage(mCameraHandler.obtainMessage(CameraHandler.MSG_SURFACE_CHANGED, width, height));
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (mSurfaceTexture == null) return;

            mSurfaceTexture.updateTexImage();

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
                        mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                                mOutputFile, mIncomingHeight, mIncomingWidth, mIncomingWidth * mIncomingHeight * 10, EGL14.eglGetCurrentContext()));
                        mRecordingStatus = RECORDING_ON;
                        break;
                    case RECORDING_RESUMED:
                        Log.d(TAG, "RESUME recording");
                        mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
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
                        mVideoEncoder.stopRecording();
                        mRecordingStatus = RECORDING_OFF;
                        break;
                    case RECORDING_OFF:
                        // yay
                        break;
                    default:
                        throw new RuntimeException("unknown status " + mRecordingStatus);
                }
            }

            //        if (mVideoEncoder.isRecording() && System.currentTimeMillis() - mVideoMillis > 50) {
            if (mVideoEncoder.isRecording()) {
                // Set the video encoder's texture name.  We only need to do this once, but in the
                // current implementation it has to happen after the video encoder is started, so
                // we just do it here.
                //
                // TODO: be less lame.
                mVideoEncoder.setTextureId(mTextureId);

                // Tell the video encoder thread that a new frame is available.
                // This will be ignored if we're not actually recording.
                mVideoEncoder.frameAvailable(mSurfaceTexture);

                mVideoMillis = System.currentTimeMillis();
            }

            if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
                return;
            }
            if (mIncomingSizeUpdated) {
                mFullScreen.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
                mIncomingSizeUpdated = false;
            }

            mSurfaceTexture.getTransformMatrix(mSTMatrix);
            mFullScreen.drawFrame(mTextureId, mSTMatrix);
        }

        public void setCameraPreviewSize(int width, int height) {
            mIncomingWidth = width;
            mIncomingHeight = height;
            mIncomingSizeUpdated = true;
        }
    }

    public void setRecordListener(MediaRecordListener recordListener) {
        if (mMovieEncoder != null) {
            mMovieEncoder.setRecordListener(recordListener);
        }
    }
}
