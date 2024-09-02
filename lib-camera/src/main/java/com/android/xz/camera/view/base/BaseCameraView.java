package com.android.xz.camera.view.base;

import com.android.xz.camera.ICameraManager;

/**
 * Camera预览View通用接口
 *
 * @author xiaozhi
 * @since 2024/8/30
 */
public interface BaseCameraView {

    ICameraManager getCameraManager();

    void onResume();

    void onPause();

    void onDestroy();
}
