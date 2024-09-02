package com.android.xz.camera;

import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.SurfaceHolder;

import com.android.xz.camera.callback.CameraCallback;
import com.android.xz.camera.callback.PictureBufferCallback;
import com.android.xz.camera.callback.PreviewBufferCallback;

/**
 * Camera和Camera2通用接口
 *
 * @author xiaozhi
 * @since 2024/8/15
 */
public interface ICameraManager {

    /**
     * 打开Camera
     */
    void openCamera();

    /**
     * 关闭释放Camera
     */
    void releaseCamera();

    /**
     * 开启预览
     *
     * @param surfaceHolder
     */
    void startPreview(SurfaceHolder surfaceHolder);

    /**
     * 开启预览
     *
     * @param surfaceTexture
     */
    void startPreview(SurfaceTexture surfaceTexture);

    /**
     * 停止预览
     */
    void stopPreview();

    /**
     * 设置Camera序号，0：后置，1：前置
     *
     * @param cameraId
     */
    void setCameraId(int cameraId);

    /**
     * 获取当前Camera序号
     *
     * @return
     */
    int getCameraId();

    /**
     * Camera是否打开
     *
     * @return
     */
    boolean isOpen();

    /**
     * 获取当前预览尺寸
     *
     * @return
     */
    Size getPreviewSize();

    /**
     * 设置预览尺寸
     *
     * @param size
     */
    void setPreviewSize(Size size);

    /**
     * 获取Camera预览数据方向
     *
     * @return
     */
    int getOrientation();

    /**
     * 获取Camera预览方向
     *
     * @return
     */
    int getDisplayOrientation();

    /**
     * 设置摄像头回调
     *
     * @param cameraCallback
     */
    void setCameraCallback(CameraCallback cameraCallback);

    /**
     * 设置预览数据回调
     *
     * @param previewBufferCallback
     */
    void addPreviewBufferCallback(PreviewBufferCallback previewBufferCallback);

    /**
     * 拍照接口
     *
     * @param pictureCallback
     */
    void takePicture(PictureBufferCallback pictureCallback);

    /**
     * 切换摄像头
     */
    void switchCamera();
}
