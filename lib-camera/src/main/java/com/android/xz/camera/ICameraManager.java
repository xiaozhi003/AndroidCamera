package com.android.xz.camera;

import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;

import com.android.xz.camera.callback.CameraCallback;

public interface ICameraManager {

    void openCamera();

    void releaseCamera();

    void startPreview(SurfaceHolder surfaceHolder);

    void startPreview(SurfaceTexture surfaceTexture);

    void stopPreview();

    boolean isOpen();

    void setCameraCallback(CameraCallback cameraCallback);
}
