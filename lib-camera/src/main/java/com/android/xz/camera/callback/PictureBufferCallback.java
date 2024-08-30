package com.android.xz.camera.callback;

/**
 * 摄像头拍照回调
 * Created by wangzhi on 2024/8/30.
 */
public interface PictureBufferCallback {

    void onPictureToken(byte[] data);
}
