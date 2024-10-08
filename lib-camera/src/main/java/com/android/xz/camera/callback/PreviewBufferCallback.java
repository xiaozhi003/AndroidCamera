package com.android.xz.camera.callback;

import com.android.xz.camera.YUVFormat;

/**
 * 摄像头预览数据回调
 *
 * @author xiaozhi
 * @since 2024/8/15
 */
public interface PreviewBufferCallback {

    void onPreviewBufferFrame(byte[] data, int width, int height, YUVFormat format);
}
