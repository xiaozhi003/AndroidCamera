package com.android.xz.view.base;

import com.android.xz.camera.ICameraManager;

public interface BaseCameraView {

    ICameraManager getCameraManager();

    void onResume();

    void onPause();

    void onDestroy();
}
