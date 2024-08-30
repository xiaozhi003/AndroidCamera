package com.android.xz.camera.callback;

/**
 * 摄像头预览数据回调
 * Created by wangzhi on 2024/8/15.
 */
public interface PreviewBufferCallback {

    void onPreviewBufferFrame(byte[] data, int width, int height);
}
