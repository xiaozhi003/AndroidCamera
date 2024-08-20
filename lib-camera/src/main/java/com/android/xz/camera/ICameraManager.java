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

    boolean isOpen();

    Size getPreviewSize();

    void setPreviewSize(Size size);

    void setCameraCallback(CameraCallback cameraCallback);
    void setPreviewBufferCallback(PreviewBufferCallback previewBufferCallback);
}
