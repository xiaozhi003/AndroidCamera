package com.android.xz.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.xz.camera.CameraManager;
import com.android.xz.camera.callback.CameraCallback;
import com.android.xz.util.Logs;

/**
 * Created by wangzhi on 2018/4/21.
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, CameraCallback {

    private static final String TAG = CameraSurfaceView.class.getSimpleName();
    SurfaceHolder mSurfaceHolder;
    private Context mContext;
    private Handler mHandler;

    private boolean hasSurface; // 是否存在摄像头显示层
    private CameraManager mCameraManager;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    public CameraSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mHandler = new Handler(context.getMainLooper());
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);//translucent半透明 transparent透明
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);
        mCameraManager = new CameraManager(context);
        mCameraManager.setCameraCallback(this);
    }

    public CameraManager getCameraManager() {
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
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logs.i(TAG, "surfaceChanged [" + width + ", " + height + "]");
        mSurfaceWidth = width;
        mSurfaceHeight = height;
    }

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
        if (mCameraManager.isOpen()) {
            mCameraManager.startPreview(getSurfaceHolder());
        }
    }

    /**
     * 关闭摄像头
     */
    private void closeCamera() {
        mCameraManager.releaseCamera();
    }

    private String getString(int resId) {
        return getResources().getString(resId);
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
    public void onOpen() {
    }

    @Override
    public void onOpenError(int error, String msg) {
    }

    @Override
    public void onPreview(int previewWidth, int previewHeight) {
        if (mSurfaceWidth > mSurfaceHeight) {
            setAspectRatio(previewWidth, previewHeight);
        } else {
            setAspectRatio(previewHeight, previewWidth);
        }
    }

    @Override
    public void onPreviewError(int error, String msg) {
    }

    @Override
    public void onClose() {
    }
}
