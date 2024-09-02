package com.android.xz.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.android.xz.camera.callback.CameraCallback;
import com.android.xz.camera.callback.PictureBufferCallback;
import com.android.xz.camera.callback.PreviewBufferCallback;
import com.android.xz.util.Logs;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Camera2实现
 *
 * @author xiaozhi
 * @since 2024/8/15
 */
public class Camera2Manager implements ICameraManager {

    private static final String TAG = Camera2Manager.class.getSimpleName();

    public static final int CAMERA_ERROR_NO_ID = -1001;
    public static final int CAMERA_ERROR_OPEN = -1002;
    public static final int CAMERA_ERROR_PREVIEW = -2001;

    private Context mContext;

    /**
     * 要打开的摄像头ID
     */
    private int mCameraId = CameraCharacteristics.LENS_FACING_FRONT;
    /**
     * 相机属性
     */
    private CameraCharacteristics mCameraCharacteristics;
    /**
     * 相机管理
     */
    private CameraManager mCameraManager;
    /**
     * 相机对象
     */
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    /**
     * 相机预览请求构造器
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private ImageReader mPictureImageReader;
    private ImageReader mPreviewImageReader;
    private Surface mPreviewSurface;
    private OrientationEventListener mOrientationEventListener;
    private int mSensorOrientation;

    /**
     * 预览大小
     */
    private Size mPreviewSize;
    private int mPreviewWidth = 1440;
    private int mPreviewHeight = 1080;
    private float mPreviewScale = mPreviewHeight * 1f / mPreviewWidth;
    private List<PreviewBufferCallback> mPreviewBufferCallbacks = new ArrayList<>();
    private PictureBufferCallback mPictureBufferCallback;
    /**
     * 拍照大小
     */
    private Size mPictureSize;
    /**
     * 原始Sensor画面顺时针旋转该角度后，画面朝上
     */
    private int mDisplayRotation = 0;
    /**
     * 设备方向，由相机传感器获取
     */
    private int mDeviceOrientation = 0;
    private int mLatestRotation = 0;

    private CameraCallback mCameraCallback;
    private Handler mUIHandler;
    private boolean previewing;

    /* 缩放相关 */
    private final int MAX_ZOOM = 200; // 放大的最大值，用于计算每次放大/缩小操作改变的大小
    private int mZoom = 0; // 0~mMaxZoom之间变化
    private float mStepWidth; // 每次改变的宽度大小
    private float mStepHeight; // 每次改变的高度大小

    /**
     * 打开摄像头的回调
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(TAG, "onOpened");
            mCameraDevice = camera;
            mUIHandler.post(() -> onOpen());
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, "onDisconnected");
            releaseCamera();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "Camera Open failed, error: " + error);
            releaseCamera();
        }
    };

    @Override
    public void setCameraCallback(CameraCallback cameraCallback) {
        mCameraCallback = cameraCallback;
    }

    @Override
    public void addPreviewBufferCallback(PreviewBufferCallback previewBufferCallback) {
        if (previewBufferCallback != null) {
            mPreviewBufferCallbacks.add(previewBufferCallback);
        }
    }

    @Override
    public void setCameraId(int cameraId) {
        mCameraId = cameraId;
    }

    @Override
    public int getCameraId() {
        return mCameraId;
    }

    public Camera2Manager(Context context) {
        mContext = context;
        mUIHandler = new Handler(mContext.getMainLooper());
        mOrientationEventListener = new OrientationEventListener(mContext) {
            @Override
            public void onOrientationChanged(int orientation) {
                mDeviceOrientation = orientation;
            }
        };
    }

    private void setUpCameraOutputs(CameraManager cameraManager) {
        try {
            if (configCameraParams(cameraManager, mCameraId + "")) {
                return;
            }
            for (String cameraId : cameraManager.getCameraIdList()) {
                if (configCameraParams(cameraManager, cameraId)) {
                    return;
                }
            }
        } catch (CameraAccessException e) {
            onOpenError(CAMERA_ERROR_OPEN, e.getMessage());
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.

            onOpenError(CAMERA_ERROR_OPEN, e.getMessage());
        }
    }

    private boolean configCameraParams(CameraManager manager, String cameraId) throws CameraAccessException {
        CameraCharacteristics characteristics
                = manager.getCameraCharacteristics(cameraId);

        StreamConfigurationMap map = characteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            return false;
        }
        Size previewSize = getSuitableSize(new ArrayList<>(Arrays.asList(map.getOutputSizes(SurfaceTexture.class))));
        Logs.i(TAG, "previewSize: " + previewSize);
        mPreviewSize = previewSize;
        mPreviewWidth = mPreviewSize.getWidth();
        mPreviewHeight = mPreviewSize.getHeight();

        Size[] supportPictureSizes = map.getOutputSizes(ImageFormat.JPEG);
        Size pictureSize = Collections.max(Arrays.asList(supportPictureSizes), new CompareSizesByArea());
        mPictureSize = pictureSize;
        Logs.i(TAG, "pictureSize: " + pictureSize);

        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        return true;
    }

    private Size getSuitableSize(List<Size> sizes) {
        int minDelta = Integer.MAX_VALUE; // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        int index = 0; // 最小的差值对应的索引坐标
        for (int i = 0; i < sizes.size(); i++) {
            Size size = sizes.get(i);
            Logs.v(TAG, "SupportedSize, width: " + size.getWidth() + ", height: " + size.getHeight());
            // 先判断比例是否相等
            if (size.getWidth() * mPreviewScale == size.getHeight()) {
                int delta = Math.abs(mPreviewWidth - size.getWidth());
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

    @Override
    public void openCamera() {
        Log.v(TAG, "openCamera");
        if (mCameraDevice != null) {
            return;
        }
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        setUpCameraOutputs(mCameraManager);
        startBackgroundThread(); // 对应 releaseCamera() 方法中的 stopBackgroundThread()
        mOrientationEventListener.enable();
        try {
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(Integer.toString(mCameraId));
            // 每次切换摄像头计算一次就行，结果缓存到成员变量中
            initDisplayRotation();
            initZoomParameter();
            // 打开摄像头
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mCameraManager.openCamera(Integer.toString(mCameraId), mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initDisplayRotation() {
        WindowManager windowManager = (WindowManager) mContext
                .getSystemService(Context.WINDOW_SERVICE);
        int displayRotation = windowManager.getDefaultDisplay().getRotation();
        switch (displayRotation) {
            case Surface.ROTATION_0:
                displayRotation = 90;
                break;
            case Surface.ROTATION_90:
                displayRotation = 0;
                break;
            case Surface.ROTATION_180:
                displayRotation = 270;
                break;
            case Surface.ROTATION_270:
                displayRotation = 180;
                break;
        }
        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        mDisplayRotation = (displayRotation + sensorOrientation + 270) % 360;
        Log.d(TAG, "mDisplayRotation: " + mDisplayRotation);
    }

    private void initZoomParameter() {
        Rect rect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        Log.d(TAG, "sensor_info_active_array_size: " + rect);
        // max_digital_zoom 表示 active_rect 除以 crop_rect 的最大值
        float max_digital_zoom = mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        Log.d(TAG, "max_digital_zoom: " + max_digital_zoom);
        // crop_rect的最小宽高
        float minWidth = rect.width() / max_digital_zoom;
        float minHeight = rect.height() / max_digital_zoom;
        // 因为缩放时两边都要变化，所以要除以2
        mStepWidth = (rect.width() - minWidth) / MAX_ZOOM / 2;
        mStepHeight = (rect.height() - minHeight) / MAX_ZOOM / 2;
    }

    @Override
    public void releaseCamera() {
        Log.v(TAG, "releaseCamera");
        stopPreview();
        if (null != mCaptureSession) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mPictureImageReader != null) {
            mPictureImageReader.close();
            mPictureImageReader = null;
        }
        if (mPreviewImageReader != null) {
            mPreviewImageReader.close();
            mPreviewImageReader = null;
        }
        mOrientationEventListener.disable();
        stopBackgroundThread(); // 对应 openCamera() 方法中的 startBackgroundThread()
        mUIHandler.post(() -> onClose());
    }

    private void createCommonSession() {
        List<Surface> outputs = new ArrayList<>();
        // preview output
        if (mPreviewSurface != null) {
            Log.d(TAG, "createCommonSession add target mPreviewSurface");
            outputs.add(mPreviewSurface);
        }
        // picture output
        Size pictureSize = mPictureSize;
        if (pictureSize != null) {
            Log.d(TAG, "createCommonSession add target mPictureImageReader");
            mPictureImageReader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(), ImageFormat.JPEG, 1);
            outputs.add(mPictureImageReader.getSurface());
        }

        // preview output
        if (!mPreviewBufferCallbacks.isEmpty()) {
            mPreviewImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 2);
            mPreviewImageReader.setOnImageAvailableListener(new OnImageAvailableListenerImpl(), mBackgroundHandler);
            outputs.add(mPreviewImageReader.getSurface());
            mPreviewRequestBuilder.addTarget(mPreviewImageReader.getSurface());
        }

        try {
            // 一个session中，所有CaptureRequest能够添加的target，必须是outputs的子集，所以在创建session的时候需要都添加进来
            mCameraDevice.createCaptureSession(outputs, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCaptureSession = session;
                    startPreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "ConfigureFailed. session: " + session);
                    previewing = false;
                }
            }, mBackgroundHandler); // handle 传入 null 表示使用当前线程的 Looper
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initPreviewRequest() {
        if (mPreviewSurface == null) {
            Log.e(TAG, "initPreviewRequest failed, mPreviewSurface is null");
            return;
        }
        if (!isOpen()) {
            return;
        }
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 设置预览输出的 Surface
            mPreviewRequestBuilder.addTarget(mPreviewSurface);
            // 设置连续自动对焦
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 设置自动曝光
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 设置自动白平衡
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startPreview(SurfaceHolder surfaceHolder) {
        if (previewing) {
            return;
        }
        previewing = true;
        mPreviewSurface = surfaceHolder.getSurface();
        initPreviewRequest();
        createCommonSession();
    }

    @Override
    public void startPreview(SurfaceTexture surfaceTexture) {
        if (previewing) {
            return;
        }
        previewing = true;
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mPreviewSurface = new Surface(surfaceTexture);
        initPreviewRequest();
        createCommonSession();
    }

    private void startPreview() {
        Log.v(TAG, "startPreview");
        if (mCaptureSession == null || mPreviewRequestBuilder == null) {
            Log.w(TAG, "startPreview: mCaptureSession or mPreviewRequestBuilder is null");
            return;
        }
        try {
            // 开始预览，即一直发送预览的请求
            CaptureRequest captureRequest = mPreviewRequestBuilder.build();
            mCaptureSession.setRepeatingRequest(captureRequest, null, mBackgroundHandler);
            Logs.i(TAG, "name:" + Thread.currentThread().getName());
            mUIHandler.post(() -> onPreview(mPreviewWidth, mPreviewHeight));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopPreview() {
        Log.v(TAG, "stopPreview");
        if (mCaptureSession == null) {
            Log.w(TAG, "stopPreview: mCaptureSession is null");
            return;
        }
        try {
            mCaptureSession.stopRepeating();
            previewing = false;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Size getPreviewSize() {
        return mPreviewSize;
    }

    @Override
    public void setPreviewSize(Size previewSize) {
        mPreviewSize = previewSize;
        mPreviewWidth = mPreviewSize.getWidth();
        mPreviewHeight = mPreviewSize.getHeight();
        mPreviewScale = mPreviewHeight * 1f / mPreviewWidth;
    }

    @Override
    public int getOrientation() {
        return mSensorOrientation;
    }

    @Override
    public int getDisplayOrientation() {
        return mDisplayRotation;
    }

    @Override
    public void takePicture(PictureBufferCallback pictureBufferCallback) {
        mPictureBufferCallback = pictureBufferCallback;
        captureStillPicture(reader -> {
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            image.close();
            if (mPictureBufferCallback != null) {
                mPictureBufferCallback.onPictureToken(bytes);
            }
        });
    }

    public void captureStillPicture(ImageReader.OnImageAvailableListener onImageAvailableListener) {
        if (mPictureImageReader == null) {
            Log.w(TAG, "captureStillPicture failed! mPictureImageReader is null");
            return;
        }
        mPictureImageReader.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler);
        try {
            // 创建一个用于拍照的Request
            CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mPictureImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(mDeviceOrientation));
            // 预览如果有放大，拍照的时候也应该保存相同的缩放
            Rect zoomRect = mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
            if (zoomRect != null) {
                captureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            }
            stopPreview();
            mCaptureSession.abortCaptures();
            final long time = System.currentTimeMillis();
            mCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Log.w(TAG, "onCaptureCompleted, time: " + (System.currentTimeMillis() - time));
                    try {
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                        mCaptureSession.capture(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    startPreview();
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getJpegOrientation(int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN)
            return 0;
        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;
        // Reverse device orientation for front-facing cameras
        boolean facingFront = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics
                .LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;
        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;
        Log.d(TAG, "jpegOrientation: " + jpegOrientation);
        mLatestRotation = jpegOrientation;
        return jpegOrientation;
    }

    public int getLatestRotation() {
        return mLatestRotation;
    }

    public boolean isFrontCamera() {
        return mCameraId == CameraCharacteristics.LENS_FACING_BACK;
    }

    public Size getPictureSize() {
        return mPictureSize;
    }

    public void setPictureSize(Size pictureSize) {
        mPictureSize = pictureSize;
    }

    public void switchCamera() {
        mCameraId ^= 1;
        Log.d(TAG, "switchCamera: mCameraId: " + mCameraId);
        releaseCamera();
        openCamera();
    }

    public void handleZoom(boolean isZoomIn) {
        if (mCameraDevice == null || mCameraCharacteristics == null || mPreviewRequestBuilder == null) {
            return;
        }
        if (isZoomIn && mZoom < MAX_ZOOM) { // 放大
            mZoom++;
        } else if (mZoom > 0) { // 缩小
            mZoom--;
        }
        Log.v(TAG, "handleZoom: mZoom: " + mZoom);
        Rect rect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int cropW = (int) (mStepWidth * mZoom);
        int cropH = (int) (mStepHeight * mZoom);
        Rect zoomRect = new Rect(rect.left + cropW, rect.top + cropH, rect.right - cropW, rect.bottom - cropH);
        Log.d(TAG, "zoomRect: " + zoomRect);
        mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
        startPreview(); // 需要重新 start preview 才能生效
    }

    public void triggerFocusAtPoint(float x, float y, int width, int height) {
        Log.d(TAG, "triggerFocusAtPoint (" + x + ", " + y + ")");
        Rect cropRegion = mPreviewRequestBuilder.get(CaptureRequest.SCALER_CROP_REGION);
        MeteringRectangle afRegion = getAFAERegion(x, y, width, height, 1f, cropRegion);
        // ae的区域比af的稍大一点，聚焦的效果比较好
        MeteringRectangle aeRegion = getAFAERegion(x, y, width, height, 1.5f, cropRegion);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{afRegion});
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{aeRegion});
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mAfCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private MeteringRectangle getAFAERegion(float x, float y, int viewWidth, int viewHeight, float multiple, Rect cropRegion) {
        Log.v(TAG, "getAFAERegion enter");
        Log.d(TAG, "point: [" + x + ", " + y + "], viewWidth: " + viewWidth + ", viewHeight: " + viewHeight);
        Log.d(TAG, "multiple: " + multiple);
        // do rotate and mirror
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        Matrix matrix1 = new Matrix();
        matrix1.setRotate(mDisplayRotation);
        matrix1.postScale(isFrontCamera() ? -1 : 1, 1);
        matrix1.invert(matrix1);
        matrix1.mapRect(viewRect);
        // get scale and translate matrix
        Matrix matrix2 = new Matrix();
        RectF cropRect = new RectF(cropRegion);
        matrix2.setRectToRect(viewRect, cropRect, Matrix.ScaleToFit.CENTER);
        Log.d(TAG, "viewRect: " + viewRect);
        Log.d(TAG, "cropRect: " + cropRect);
        // get out region
        int side = (int) (Math.max(viewWidth, viewHeight) / 8 * multiple);
        RectF outRect = new RectF(x - side / 2, y - side / 2, x + side / 2, y + side / 2);
        Log.d(TAG, "outRect before: " + outRect);
        matrix1.mapRect(outRect);
        matrix2.mapRect(outRect);
        Log.d(TAG, "outRect after: " + outRect);
        // 做一个clamp，测光区域不能超出cropRegion的区域
        Rect meteringRect = new Rect((int) outRect.left, (int) outRect.top, (int) outRect.right, (int) outRect.bottom);
        meteringRect.left = clamp(meteringRect.left, cropRegion.left, cropRegion.right);
        meteringRect.top = clamp(meteringRect.top, cropRegion.top, cropRegion.bottom);
        meteringRect.right = clamp(meteringRect.right, cropRegion.left, cropRegion.right);
        meteringRect.bottom = clamp(meteringRect.bottom, cropRegion.top, cropRegion.bottom);
        Log.d(TAG, "meteringRegion: " + meteringRect);
        return new MeteringRectangle(meteringRect, 1000);
    }

    private final CameraCaptureSession.CaptureCallback mAfCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            Integer state = result.get(CaptureResult.CONTROL_AF_STATE);
            Log.d(TAG, "CONTROL_AF_STATE: " + state);
            if (state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || state == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                Log.d(TAG, "process: start normal preview");
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.FLASH_MODE_OFF);
                startPreview();
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }
    };


    private void startBackgroundThread() {
        if (mBackgroundThread == null || mBackgroundHandler == null) {
            Log.v(TAG, "startBackgroundThread");
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        Log.v(TAG, "stopBackgroundThread");
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Size chooseOptimalSize(Size[] sizes, int dstSize, float aspectRatio) {
        if (sizes == null || sizes.length <= 0) {
            Log.e(TAG, "chooseOptimalSize failed, input sizes is empty");
            return null;
        }
        int minDelta = Integer.MAX_VALUE; // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        int index = 0; // 最小的差值对应的索引坐标
        for (int i = 0; i < sizes.length; i++) {
            Size size = sizes[i];
            // 先判断比例是否相等
            if (size.getWidth() * aspectRatio == size.getHeight()) {
                int delta = Math.abs(dstSize - size.getHeight());
                if (delta == 0) {
                    return size;
                }
                if (minDelta > delta) {
                    minDelta = delta;
                    index = i;
                }
            }
        }
        return sizes[index];
    }

    private int clamp(int x, int min, int max) {
        if (x > max) return max;
        if (x < min) return min;
        return x;
    }

    public boolean isOpen() {
        return mCameraDevice != null;
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // 我们在这里投放，以确保乘法不会溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
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

    private class OnImageAvailableListenerImpl implements ImageReader.OnImageAvailableListener {
        private byte[] y;
        private byte[] u;
        private byte[] v;
        private byte[] yuvData;
        private ReentrantLock lock = new ReentrantLock();

        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            // Y:U:V == 4:2:2
            if (image.getFormat() == ImageFormat.YUV_420_888) {
                Image.Plane[] planes = image.getPlanes();
                int width = image.getWidth();
                int height = image.getHeight();

                // 加锁确保y、u、v来源于同一个Image
                lock.lock();

                /** Y */
                ByteBuffer bufferY = planes[0].getBuffer();
                /** U(Cb) */
                ByteBuffer bufferU = planes[1].getBuffer();
                /** V(Cr) */
                ByteBuffer bufferV = planes[2].getBuffer();

                // 重复使用同一批byte数组，减少gc频率
                if (y == null) {
                    y = new byte[bufferY.limit() - bufferY.position()];
                    u = new byte[bufferU.limit() - bufferU.position()];
                    v = new byte[bufferV.limit() - bufferV.position()];
                }
//                Logs.i(TAG, "y.len:" + bufferY.remaining() + " planes[0].pixelStride:" + planes[0].getPixelStride() + " planes[0].rowStride:" + planes[0].getRowStride());
//                Logs.i(TAG, "u.len:" + bufferU.remaining() + " planes[1].pixelStride:" + planes[1].getPixelStride() + " planes[1].rowStride:" + planes[1].getRowStride());
//                Logs.i(TAG, "v.len:" + bufferV.remaining() + " planes[2].pixelStride:" + planes[2].getPixelStride() + " planes[2].rowStride:" + planes[2].getRowStride());

                if (yuvData == null) {
                    yuvData = new byte[width * height * 3 / 2];
                }

                if (bufferY.remaining() == y.length) {
                    bufferY.get(y);
                    bufferU.get(u);
                    bufferV.get(v);

                    // 数据前期处理
                    // 处理y
                    int yRowStride = planes[0].getRowStride();
                    if (yRowStride == width) {
                        System.arraycopy(y, 0, yuvData, 0, y.length);
                    } else {
                        // 按行提取
                        for (int i = 0; i < height; i++) {
                            System.arraycopy(y, i * yRowStride, yuvData, i * width, width);
                        }
                    }

                    int ySize = width * height;

                    // 判断是p还是sp
                    if (planes[1].getPixelStride() == 1) { // P
                        int offset = ySize;
                        // 处理U
                        int uRowStride = planes[1].getRowStride();
                        if (uRowStride == width / 2) {
                            System.arraycopy(u, 0, yuvData, offset, u.length);
                        } else {
                            int rowStride = width / 2;
                            for (int i = 0; i < height / 2; i++) {
                                System.arraycopy(u, i * uRowStride, yuvData, offset + i * rowStride, rowStride);
                            }
                        }

                        offset = ySize + width * height / 4;
                        // 处理V
                        int vRowStride = planes[2].getRowStride();
                        if (vRowStride == width / 2) {
                            System.arraycopy(v, 0, yuvData, offset, v.length);
                        } else {
                            int rowStride = width / 2;
                            for (int i = 0; i < height / 2; i++) {
                                System.arraycopy(v, i * vRowStride, yuvData, offset + i * rowStride, rowStride);
                            }
                        }
                    } else if (planes[1].getPixelStride() == 2) { // SP
                        int offset = width * height;

                        // 处理U分量
                        int uRowStride = planes[1].getRowStride();
                        if (uRowStride == width) {
                            for (int i = 0; i < u.length; i++) {
                                if (0 == (i % 2)) {
                                    yuvData[offset] = u[i];
                                    offset++;
                                }
                            }
                        } else {
                            // 按行提取
                            for (int i = 0; i < height / 2; i++) {
                                for (int j = i * uRowStride; j < uRowStride * i + width; j++) {
                                    if (0 == (j % 2)) {
                                        yuvData[offset] = u[i];
                                        offset++;
                                    }
                                }
                            }
                        }

                        offset = width * height * 5 / 4;
                        // 提取V分量
                        int vRowStride = planes[2].getRowStride();
                        if (vRowStride == width) {
                            for (int i = 0; i < v.length; i++) {
                                if (0 == (i % 2)) {
                                    yuvData[offset] = v[i];
                                    offset++;
                                }
                            }
                        } else {
                            // 按行提取
                            for (int i = 0; i < height / 2; i++) {
                                for (int j = i * vRowStride; j < vRowStride * i + width; j++) {
                                    if (0 == (j % 2)) {
                                        yuvData[offset] = u[i];
                                        offset++;
                                    }
                                }
                            }
                        }
                    }
                }
                if (!mPreviewBufferCallbacks.isEmpty()) {
                    for (PreviewBufferCallback previewBufferCallback : mPreviewBufferCallbacks) {
                        previewBufferCallback.onPreviewBufferFrame(yuvData, image.getWidth(), image.getHeight());
                    }
                }
                lock.unlock();
            }

            image.close();
        }
    }
}
