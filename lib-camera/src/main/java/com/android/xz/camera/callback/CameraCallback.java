package com.android.xz.camera.callback;

import android.hardware.Camera;

public interface CameraCallback {

    void onOpen();

    void onOpenError(int error, String msg);

    void onPreview(int previewWidth, int previewHeight);

    void onPreviewError(int error, String msg);

    void onClose();
}
