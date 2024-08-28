package com.android.xz.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.android.xz.camera.callback.CameraCallback;
import com.android.xz.camera.callback.PreviewBufferCallback;
import com.android.xz.util.Logs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangzhi on 2024/8/15.
 */
public class CameraManager implements Camera.AutoFocusCallback, ICameraManager {

    public static final int CAMERA_ERROR_NO_ID = -1001;
    public static final int CAMERA_ERROR_OPEN = -1002;
    public static final int CAMERA_ERROR_PREVIEW = -2001;

    private static final String TAG = CameraManager.class.getSimpleName();

    /**
     * 为了实现拍照的快门声音及拍照保存照片需要下面三个回调变量
     * 快门按下的回调，在这里我们可以设置类似播放“咔嚓”声之类的操作。默认的就是咔嚓。
     */
    ShutterCallback mShutterCallback = new ShutterCallback() {
        public void onShutter() {
            // TODO Auto-generated method stub
            Logs.i(TAG, "myShutterCallback:onShutter...");
        }
    };

    /**
     * 拍摄的未压缩原数据的回调,可以为null
     */
    PictureCallback mRawCallback = new PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            Logs.i(TAG, "myRawCallback:onPictureTaken...");
        }
    };

    private Camera mCamera;
    private Parameters mParameters;
    private Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
    private boolean isPreviewing = false;
    private int mDisplayOrientation = -1;
    private int mOrientation = -1;
    private int mCameraId = 0;
    private Size mPreviewSize;
    private int mPreviewWidth = 1440;
    private int mPreviewHeight = 1080;

    private float mPreviewScale = mPreviewHeight * 1f / mPreviewWidth;
    private Context mContext;
    private byte[] mCameraBytes = null;
    private boolean isSupportZoom;
    private CameraCallback mCameraCallback;
    private List<PreviewBufferCallback> mPreviewBufferCallbacks = new ArrayList<>();

    private PreviewCallback mPreviewCallback = new PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (!mPreviewBufferCallbacks.isEmpty()) {
                for (PreviewBufferCallback previewBufferCallback : mPreviewBufferCallbacks) {
                    previewBufferCallback.onPreviewBufferFrame(data, mPreviewWidth, mPreviewHeight);
                }
            }
            mCameraBytes = data;
            camera.addCallbackBuffer(data);
        }
    };
    private Camera.ErrorCallback errorCallback = (error, camera) -> {
        String msg = "";
        switch (error) {
            case Camera.CAMERA_ERROR_SERVER_DIED:
                Logs.e(TAG, "Camera.CAMERA_ERROR_SERVER_DIED camera id:" + mCameraId);
                msg = "Camera Media server died.";
                //这里重新初始化Camera即可
                break;
            case Camera.CAMERA_ERROR_UNKNOWN:
                Logs.e(TAG, "Camera.CAMERA_ERROR_UNKNOWN");
                msg = "Camera unknown error.";
                break;
        }
        onOpenError(error, msg);
        releaseCamera();
    };

    private OrientationEventListener mOrientationEventListener;
    private int mLatestRotation = 0;

    public CameraManager(Context context) {
        mContext = context;
        mOrientationEventListener = new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(int orientation) {
                setPictureRotate(orientation);
            }
        };
    }

    /**
     * 获取Camera实例
     *
     * @return
     */
    public Camera getCamera() {
        return mCamera;
    }

    /**
     * 设置预览尺寸
     */
    @Override
    public void setPreviewSize(Size size) {
        mPreviewSize = size;
        mPreviewWidth = size.getWidth();
        mPreviewHeight = size.getHeight();
        mPreviewScale = mPreviewHeight * 1f / mPreviewWidth;
    }

    @Override
    public Size getPreviewSize() {
        return mPreviewSize;
    }

    /**
     * @return
     */
    public int getDisplayOrientation() {
        return mDisplayOrientation;
    }

    /**
     * 设置预览角度
     *
     * @param displayOrientation
     */
    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
    }

    /**
     * 获取摄像头旋转角度
     *
     * @return
     */
    public int getOrientation() {
        return mOrientation;
    }

    public int getCameraId() {
        return mCameraId;
    }

    /**
     * 设置打开摄像头号（0：后置，1：前置）
     *
     * @param cameraId
     */
    @Override
    public void setCameraId(int cameraId) {
        mCameraId = cameraId;
    }

    @Override
    public void addPreviewBufferCallback(PreviewBufferCallback previewBufferCallback) {
        if (previewBufferCallback != null) {
            mPreviewBufferCallbacks.add(previewBufferCallback);
        }
    }

    @Override
    public void setCameraCallback(CameraCallback cameraCallback) {
        mCameraCallback = cameraCallback;
    }

    public void takePicture(PictureCallback pictureCallback) {
        takePicture(null, null, pictureCallback);
    }

    public void takePicture(ShutterCallback shutterCallback, PictureCallback rawCallback, PictureCallback jpegCallback) {
        if (null != mCamera && isPreviewing) {
            isPreviewing = false;
            Logs.i(TAG, "latestRotation:" + getLatestRotation());
            mParameters.setRotation(getLatestRotation());
            mCamera.setParameters(mParameters);
            mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
        }
    }

    /**
     * 打开Camera
     */
    @Override
    public synchronized void openCamera() {
        Logs.i(TAG, "Camera open #" + mCameraId);
        if (mCamera == null) {
            if (mCameraId >= Camera.getNumberOfCameras()) {
                onOpenError(CAMERA_ERROR_NO_ID, "No camera.");
                return;
            }
            try {
                mCamera = Camera.open(mCameraId);
                Camera.getCameraInfo(mCameraId, mCameraInfo);
                mCamera.setErrorCallback(errorCallback);
                initCamera();
                onOpen();
                mOrientationEventListener.enable();
            } catch (Exception e) {
                onOpenError(CAMERA_ERROR_OPEN, e.getMessage());
            }
        }
    }

    /**
     * 摄像头打开状态
     *
     * @return
     */
    @Override
    public synchronized boolean isOpen() {
        return mCamera != null;
    }

    /**
     * 使用Surfaceview开启预览
     *
     * @param holder
     */
    @Override
    public synchronized void startPreview(SurfaceHolder holder) {
        Logs.i(TAG, "startPreview...");
        if (isPreviewing) {
            return;
        }
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(holder);
                if (!mPreviewBufferCallbacks.isEmpty()) {
                    mCamera.addCallbackBuffer(new byte[mPreviewWidth * mPreviewHeight * 3 / 2]);
                    mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);
                }
                mCamera.startPreview();
                onPreview(mPreviewWidth, mPreviewHeight);
            } catch (Exception e) {
                onPreviewError(CAMERA_ERROR_PREVIEW, e.getMessage());
            }
        }
    }

    /**
     * 使用TextureView预览Camera
     *
     * @param surface
     */
    @Override
    public synchronized void startPreview(SurfaceTexture surface) {
        Logs.i(TAG, "startPreview...");
        if (isPreviewing) {
            return;
        }
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(surface);
                if (!mPreviewBufferCallbacks.isEmpty()) {
                    mCamera.addCallbackBuffer(new byte[mPreviewWidth * mPreviewHeight * 3 / 2]);
                    mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);
                }
                mCamera.startPreview();
                onPreview(mPreviewWidth, mPreviewHeight);
            } catch (Exception e) {
                onPreviewError(CAMERA_ERROR_PREVIEW, e.getMessage());
            }
        }
    }

    /**
     * 关闭预览
     */
    @Override
    public synchronized void stopPreview() {
        Logs.v(TAG, "stopPreview.");
        if (isPreviewing && null != mCamera) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                mPreviewBufferCallbacks.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        isPreviewing = false;
    }

    /**
     * 停止预览，释放Camera
     */
    @Override
    public synchronized void releaseCamera() {
        Logs.v(TAG, "releaseCamera.");
        if (null != mCamera) {
            stopPreview();
            try {
                mCamera.release();
                mCamera = null;
                mCameraBytes = null;
                mDisplayOrientation = -1;
            } catch (Exception e) {
            }
            onClose();
        }
    }

    /**
     * 配置Camera参数
     */
    private void initCamera() {
        if (mCamera != null) {
            mParameters = mCamera.getParameters();
            if (mDisplayOrientation == -1) {
                setCameraDisplayOrientation(mContext, mCameraId, mCamera);
            }
            // 设置预览方向
            mCamera.setDisplayOrientation(mDisplayOrientation);
            // 设置拍照方向
            mParameters.setRotation(mOrientation);

            // 如果摄像头不支持这些参数都会出错的，所以设置的时候一定要判断是否支持
            List<String> supportedFlashModes = mParameters.getSupportedFlashModes();
            if (supportedFlashModes != null && supportedFlashModes.contains(Parameters.FLASH_MODE_OFF)) {
                mParameters.setFlashMode(Parameters.FLASH_MODE_OFF); // 设置闪光模式
            }
            List<String> supportedFocusModes = mParameters.getSupportedFocusModes();
            if (supportedFocusModes != null && supportedFocusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
                mParameters.setFocusMode(Parameters.FOCUS_MODE_AUTO); // 设置聚焦模式
            }
            mParameters.setPreviewFormat(ImageFormat.NV21); // 设置预览图片格式
            mParameters.setPictureFormat(ImageFormat.JPEG); // 设置拍照图片格式
            mParameters.setExposureCompensation(0); // 设置曝光强度

            Camera.Size previewSize = getSuitableSize(mParameters.getSupportedPreviewSizes());
            mPreviewWidth = previewSize.width;
            mPreviewHeight = previewSize.height;
            mPreviewSize = new Size(mPreviewWidth, mPreviewHeight);
            mParameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
            Logs.d(TAG, "previewWidth: " + mPreviewWidth + ", previewHeight: " + mPreviewHeight);

            Camera.Size pictureSize = mParameters.getPictureSize();
            mParameters.setPictureSize(pictureSize.width, pictureSize.height);
            Logs.d(TAG, "pictureWidth: " + pictureSize.width + ", pictureHeight: " + pictureSize.height);

            mCamera.setParameters(mParameters);
            isSupportZoom = mParameters.isSmoothZoomSupported();
        }
    }

    /**
     * 开启闪光灯
     */
    public void setFlashModeOn() {
        if (mCamera == null)
            return;

        Parameters parameters = mCamera.getParameters();
        List<String> flashModes = parameters.getSupportedFlashModes();
        // Check if camera flash exists
        if (flashModes == null) {
            // Use the screen as a flashlight (next best thing)
            return;
        }
        String flashMode = parameters.getFlashMode();
        if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
            // Turn on the flash
            if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
            } else {
            }
        }
    }

    /**
     * 关闭闪光灯
     */
    public void setFlashModeOff() {
        if (mCamera == null)
            return;

        Parameters parameters = mCamera.getParameters();
        List<String> flashModes = parameters.getSupportedFlashModes();
        // Check if camera flash exists
        if (flashModes == null) {
            // Use the screen as a flashlight (next best thing)
            return;
        }
        if (!Parameters.FLASH_MODE_OFF.equals(flashModes)) {
            // Turn off the flash
            if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
            }
        }
    }

    /**
     * 是否正在预览
     *
     * @return
     */
    public boolean isPreviewing() {
        return isPreviewing;
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        // 先改变摄像头方向
        mCameraId ^= 1;
        List<PreviewBufferCallback> previewBufferCallbacks = new ArrayList<>();
        if (!mPreviewBufferCallbacks.isEmpty()) {
            previewBufferCallbacks.addAll(mPreviewBufferCallbacks);
        }
        releaseCamera();
        if (!previewBufferCallbacks.isEmpty()) {
            mPreviewBufferCallbacks.addAll(previewBufferCallbacks);
        }
        openCamera();
    }

    public void focusOnPoint(int x, int y, int width, int height) {
        Logs.v(TAG, "touch point (" + x + ", " + y + ")");
        if (mCamera == null) {
            return;
        }
        Parameters parameters = mCamera.getParameters();
        // 1.先要判断是否支持设置聚焦区域
        if (parameters.getMaxNumFocusAreas() > 0) {
            // 2.以触摸点为中心点，view窄边的1/4为聚焦区域的默认边长
            int length = Math.min(width, height) >> 3; // 1/8的长度
            int left = x - length;
            int top = y - length;
            int right = x + length;
            int bottom = y + length;
            // 3.映射，因为相机聚焦的区域是一个(-1000,-1000)到(1000,1000)的坐标区域
            left = left * 2000 / width - 1000;
            top = top * 2000 / height - 1000;
            right = right * 2000 / width - 1000;
            bottom = bottom * 2000 / height - 1000;
            // 4.判断上述矩形区域是否超过边界，若超过则设置为临界值
            left = left < -1000 ? -1000 : left;
            top = top < -1000 ? -1000 : top;
            right = right > 1000 ? 1000 : right;
            bottom = bottom > 1000 ? 1000 : bottom;
            Logs.d(TAG, "focus area (" + left + ", " + top + ", " + right + ", " + bottom + ")");
            ArrayList<Camera.Area> areas = new ArrayList<>();
            areas.add(new Camera.Area(new Rect(left, top, right, bottom), 600));
            parameters.setFocusAreas(areas);
        }
        try {
            mCamera.cancelAutoFocus(); // 先要取消掉进程中所有的聚焦功能
            mCamera.setParameters(parameters);
            mCamera.autoFocus(this); // 调用聚焦
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取合适的尺寸
     *
     * @param sizes
     * @return
     */
    private Camera.Size getSuitableSize(List<Camera.Size> sizes) {
        int minDelta = Integer.MAX_VALUE; // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        int index = 0; // 最小的差值对应的索引坐标
        for (int i = 0; i < sizes.size(); i++) {
            Camera.Size size = sizes.get(i);
            Logs.v(TAG, "SupportedSize, width: " + size.width + ", height: " + size.height);
            // 先判断比例是否相等
            if (size.width * mPreviewScale == size.height) {
                int delta = Math.abs(mPreviewWidth - size.width);
                if (delta == 0) {
                    return size;
                }
                if (minDelta > delta) {
                    minDelta = delta;
                    index = i;
                }
            }
        }
        return sizes.get(index);
    }

    public void setZoom(int zoomValue) {
        if (isSupportZoom) {
            try {
                Parameters params = mCamera.getParameters();
                final int MAX = params.getMaxZoom();
                if (MAX == 0) return;
                params.setZoom(zoomValue);
                mCamera.setParameters(params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public int getZoom() {
        if (isSupportZoom) {
            try {
                Parameters params = mCamera.getParameters();
                return params.getZoom();
            } catch (Exception e) {
                Logs.e(TAG, e.getMessage());
                return 0;
            }
        } else {
            return 0;
        }
    }

    public synchronized void handleZoom(boolean isZoomIn) {
        if (mCamera == null) {
            return;
        }
        Parameters parameters = mCamera.getParameters();
        if (parameters.isZoomSupported()) {
            int maxZoom = parameters.getMaxZoom();
            int zoom = parameters.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            Logs.d(TAG, "handleZoom: zoom: " + zoom);
            parameters.setZoom(zoom);
            mCamera.setParameters(parameters);
        } else {
            Logs.i(TAG, "zoom not supported");
        }
    }

    private void setPictureRotate(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return;
        orientation = (orientation + 45) / 90 * 90;
        int rotation;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (mCameraInfo.orientation - orientation + 360) % 360;
        } else {  // back-facing camera
            rotation = (mCameraInfo.orientation + orientation) % 360;
        }
        mLatestRotation = rotation;
    }

    public int getLatestRotation() {
        return mLatestRotation;
    }

    public void setCameraDisplayOrientation(Context context, int cameraId,
                                            Camera camera) {
        if (context == null)
            return;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
        mDisplayOrientation = result;
        mOrientation = info.orientation;
        Logs.d(TAG, "displayOrientation:" + mDisplayOrientation);
        Logs.d(TAG, "orientation:" + mOrientation);
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

    private void onOpen() {
        if (mCameraCallback != null) {
            mCameraCallback.onOpen();
        }
    }

    private void onOpenError(int error, String msg) {
        if (mCameraCallback != null) {
            mCameraCallback.onOpenError(error, msg);
        }
    }

    private void onPreview(int width, int height) {
        isPreviewing = true;
        if (mCameraCallback != null) {
            mCameraCallback.onPreview(width, height);
        }
    }

    private void onPreviewError(int error, String msg) {
        if (mCameraCallback != null) {
            mCameraCallback.onPreviewError(error, msg);
        }
    }

    private void onClose() {
        if (mCameraCallback != null) {
            mCameraCallback.onClose();
        }
    }
}
