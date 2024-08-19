package com.android.xz.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.TextureView;

import com.android.xz.camera.CameraManager;
import com.android.xz.camera.callback.CameraCallback;
import com.android.xz.util.Logs;

/**
 * Ø¬
 * 摄像头预览View
 * Created by xiaozhi on 2018/4/21.
 */
public class CameraTextureView extends TextureView implements TextureView.SurfaceTextureListener, CameraCallback {

    private static final String TAG = CameraTextureView.class.getSimpleName();

    private Context mContext;
    private SurfaceTexture mSurfaceTexture;
    private Handler mHandler;
    private boolean isMirror;
    private boolean isMirrorCameraBytes;
    private boolean hasSurface; // 是否存在摄像头显示层
    private CameraManager mCameraManager;

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    private int mTextureWidth;
    private int mTextureHeight;

    public CameraTextureView(Context context) {
        super(context);
        init(context);
    }

    public CameraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mHandler = new Handler(mContext.getMainLooper());
        mCameraManager = new CameraManager(context);
        mCameraManager.setCameraCallback(this);
        setSurfaceTextureListener(this);
    }

    /**
     * 获取摄像头工具类
     *
     * @return
     */
    public CameraManager getCameraManager() {
        return mCameraManager;
    }

    /**
     * 获取camera数据
     *
     * @return
     */
    public byte[] getCameraBytes() {
        return mCameraManager.getCameraBytes();
    }

    /**
     * 是否镜像
     *
     * @return
     */
    public boolean isMirror() {
        return isMirror;
    }

    /**
     * 设置是否镜像
     *
     * @param mirror
     */
    public void setMirror(boolean mirror) {
        isMirror = mirror;
        requestLayout();
    }

    public boolean isMirrorCameraBytes() {
        return isMirrorCameraBytes;
    }

    public void setMirrorCameraBytes(boolean mirrorCameraBytes) {
        isMirrorCameraBytes = mirrorCameraBytes;
        mCameraManager.setMirror(isMirrorCameraBytes);
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

    /**
     * 获取SurfaceTexture
     *
     * @return
     */
    @Override
    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Logs.i(TAG, "onSurfaceTextureAvailable.");
        mTextureWidth = width;
        mTextureHeight = height;
        mSurfaceTexture = surfaceTexture;
        hasSurface = true;
        openCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        Logs.i(TAG, "onSurfaceTextureSizeChanged.");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Logs.v(TAG, "onSurfaceTextureDestroyed.");
        closeCamera();
        hasSurface = false;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    /**
     * 打开摄像头并预览
     */
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
    public void onPause() {
        closeCamera();
    }

    /**
     * 初始化摄像头，较为关键的内容
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
        if (mCameraManager.isOpen()) {
            mCameraManager.startPreview(mSurfaceTexture);
        }
    }

    private void closeCamera() {
        mCameraManager.releaseCamera();
    }

    @Override
    public void onOpen() {

    }

    @Override
    public void onOpenError(int error, String msg) {

    }

    @Override
    public void onPreview(int previewWidth, int previewHeight) {
        if (mTextureWidth > mTextureHeight) {
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
