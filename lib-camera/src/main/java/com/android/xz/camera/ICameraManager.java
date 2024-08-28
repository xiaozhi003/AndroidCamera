package com.android.xz.camera;

import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.SurfaceHolder;

import com.android.xz.camera.callback.CameraCallback;
import com.android.xz.camera.callback.PreviewBufferCallback;

public interface ICameraManager {

    void openCamera();

    void releaseCamera();

    void startPreview(SurfaceHolder surfaceHolder);

    void startPreview(SurfaceTexture surfaceTexture);

    void stopPreview();

    void setCameraId(int cameraId);

    int getCameraId();

    boolean isOpen();

    Size getPreviewSize();

    void setPreviewSize(Size size);

    int getOrientation();

    int getDisplayOrientation();

    void setCameraCallback(CameraCallback cameraCallback);
    void addPreviewBufferCallback(PreviewBufferCallback previewBufferCallback);
}
