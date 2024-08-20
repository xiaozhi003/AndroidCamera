package com.android.xz.camera.callback;

public interface PreviewBufferCallback {

    void onPreviewBufferFrame(byte[] data, int width, int height);
}
