package com.android.xz.camera.view.base;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.callback.CameraCallback;
import com.android.xz.encoder.MediaRecordListener;
import com.android.xz.encoder.TextureMovieEncoder2;
import com.android.xz.util.Logs;

/**
 * 摄像头预览SurfaceView，自定义opengl
 *
 * @author xiaozhi
 * @since 2024/8/22
 */
public abstract class BaseGLESSurfaceView extends SurfaceView implements SurfaceHolder.Callback,
        SurfaceTexture.OnFrameAvailableListener,
        CameraCallback,
        BaseCameraView, RenderThread.GLSurfaceTextureCallback {

    private static final String TAG = BaseGLESSurfaceView.class.getSimpleName();

    private Context mContext;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceTexture mPreviewSurfaceTexture;
    private boolean hasSurface; // 是否存在摄像头显示层
    private ICameraManager mCameraManager;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private RenderThread mRenderThread;
    private TextureMovieEncoder2 mMovieEncoder;

    public BaseGLESSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public BaseGLESSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BaseGLESSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public BaseGLESSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);
        mCameraManager = createCameraManager(context);
        mCameraManager.setCameraCallback(this);
        mMovieEncoder = new TextureMovieEncoder2(context);
        mRenderThread = new RenderThread(mContext, mMovieEncoder);
        mRenderThread.setGLSurfaceTextureCallback(this);
        mRenderThread.start();
        mRenderThread.waitUntilReady();
    }

    public void startRecord() {
        if (mRenderThread != null) {
            RenderHandler handler = mRenderThread.getHandler();
            if (handler != null) {
                handler.sendRecordState(true);
            }
        }
    }

    public void stopRecord() {
        if (mRenderThread != null) {
            RenderHandler handler = mRenderThread.getHandler();
            if (handler != null) {
                handler.sendRecordState(false);
            }
        }
    }

    public abstract ICameraManager createCameraManager(Context context);

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Logs.i(TAG, "surfaceCreated.");
        mSurfaceHolder = holder;
        if (mRenderThread != null) {
            RenderHandler handler = mRenderThread.getHandler();
            if (handler != null) {
                handler.sendSurfaceAvailable(mSurfaceHolder);
            }
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Logs.i(TAG, "surfaceChanged " + width + "x" + height);
        if (mRenderThread != null) {
            RenderHandler handler = mRenderThread.getHandler();
            if (handler != null) {
                handler.sendSurfaceChanged(0, width, height);
            }
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Logs.v(TAG, "onSurfaceTextureDestroyed.");
        closeCamera();
        mSurfaceHolder = null;
        if (mRenderThread != null) {
            RenderHandler handler = mRenderThread.getHandler();
            if (handler != null) {
                handler.sendSurfaceDestroyed();
            }
        }
        hasSurface = false;
    }

    /**
     * 获取摄像头工具类
     *
     * @return
     */
    public ICameraManager getCameraManager() {
        return mCameraManager;
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

    /**
     * 获取SurfaceTexture
     *
     * @return
     */
    public SurfaceTexture getSurfaceTexture() {
        return mPreviewSurfaceTexture;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (mRenderThread != null) {
            RenderHandler handler = mRenderThread.getHandler();
            if (handler != null) {
                handler.sendFrameAvailable();
            }
        }
    }

    /**
     * Connects the SurfaceTexture to the Camera preview output, and starts the preview.
     */
    @Override
    public void onGLSurfaceCreated(SurfaceTexture st) {
        Logs.i(TAG, "onGLSurfaceCreated " + st);
        mPreviewSurfaceTexture = st;
        hasSurface = true;
        mPreviewSurfaceTexture.setOnFrameAvailableListener(this);
        openCamera();
    }

    /**
     * 打开摄像头并预览
     */
    @Override
    public void onResume() {
        if (hasSurface) {
            // 当activity暂停，但是并未停止的时候，surface仍然存在，所以 surfaceCreated()
            // 并不会调用，需要在此处初始化摄像头
            openCamera();
        }
    }

    /**
     * 停止预览并关闭摄像头
     */
    @Override
    public void onPause() {
        closeCamera();
    }

    @Override
    public void onDestroy() {
        if (mRenderThread != null) {
            RenderHandler handler = mRenderThread.getHandler();
            if (handler != null) {
                handler.sendShutdown();
            }
        }
    }

    /**
     * 初始化摄像头，较为关键的内容
     */
    private void openCamera() {
        if (mPreviewSurfaceTexture == null) {
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
        stopRecord();
        mCameraManager.releaseCamera();
    }

    @Override
    public void onOpen() {
        mCameraManager.startPreview(mPreviewSurfaceTexture);
    }

    @Override
    public void onOpenError(int error, String msg) {

    }

    @Override
    public void onPreview(int previewWidth, int previewHeight) {
        Logs.i(TAG, "onPreview " + previewWidth + " " + previewHeight);
        if (mRenderThread != null) {
            RenderHandler handler = mRenderThread.getHandler();
            if (handler != null) {
                handler.sendPreviewSize(previewWidth, previewHeight);
            }
        }

        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
        setAspectRatio(mPreviewHeight, mPreviewWidth);
    }

    @Override
    public void onPreviewError(int error, String msg) {

    }

    @Override
    public void onClose() {

    }

    public void setRecordListener(MediaRecordListener recordListener) {
        if (mMovieEncoder != null) {
            mMovieEncoder.setRecordListener(recordListener);
        }
    }
}
 