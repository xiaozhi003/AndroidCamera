package com.android.xz.view.base;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.callback.CameraCallback;
import com.android.xz.util.Logs;

/**
 * 摄像头预览SurfaceView
 * Created by wangzhi on 2024/8/22.
 */
public abstract class BaseSurfaceView extends SurfaceView implements SurfaceHolder.Callback, CameraCallback {

    protected static final String TAG = BaseSurfaceView.class.getSimpleName();
    private SurfaceHolder mSurfaceHolder;
    protected Context mContext;
    private boolean hasSurface; // 是否存在摄像头显示层
    private ICameraManager mCameraManager;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    protected int mSurfaceWidth;
    protected int mSurfaceHeight;
    protected Size mPreviewSize = new Size(0, 0);

    public BaseSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public BaseSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BaseSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    protected void init(Context context) {
        mContext = context;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);//translucent半透明 transparent透明
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);
        mCameraManager = createCameraManager(context);
        mCameraManager.setCameraCallback(this);
    }

    public abstract ICameraManager createCameraManager(Context context);

    public ICameraManager getCameraManager() {
        return mCameraManager;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Logs.i(TAG, "surfaceCreated..." + hasSurface);
        if (!hasSurface && holder != null) {
            hasSurface = true;
            openCamera();
        }
    }

    @Override
    public abstract void surfaceChanged(SurfaceHolder holder, int format, int width, int height);

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Logs.v(TAG, "surfaceDestroyed.");
        closeCamera();
        hasSurface = false;
    }

    public SurfaceHolder getSurfaceHolder() {
        return mSurfaceHolder;
    }

    public void onResume() {
        if (hasSurface) {
            // 当activity暂停，但是并未停止的时候，surface仍然存在，所以 surfaceCreated()
            // 并不会调用，需要在此处初始化摄像头
            openCamera();
        }
    }

    public void onPause() {
        closeCamera();
    }

    /**
     * 打开摄像头
     */
    private void openCamera() {
        if (mSurfaceHolder == null) {
            Logs.e(TAG, "SurfaceHolder is null.");
            return;
        }
        if (mCameraManager.isOpen()) {
            Logs.w(TAG, "Camera is opened！");
            return;
        }
        mCameraManager.openCamera();
    }

    /**
     * 关闭摄像头
     */
    private void closeCamera() {
        mCameraManager.releaseCamera();
    }

    private void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            return;
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
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    @Override
    public abstract void onOpen();

    @Override
    public void onOpenError(int error, String msg) {
    }

    @Override
    public void onPreview(int previewWidth, int previewHeight) {
        setAspectRatio(previewHeight, previewWidth);
    }

    @Override
    public void onPreviewError(int error, String msg) {
    }

    @Override
    public void onClose() {
    }
}
