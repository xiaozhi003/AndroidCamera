package com.android.xz.view;

import android.content.Context;
import android.util.AttributeSet;

import com.android.xz.camera.Camera2Manager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.view.base.BaseGLSurfaceView;

/**
 * Created by wangzhi on 2024/8/22.
 */
public class Camera2GLSurfaceView extends BaseGLSurfaceView {

    public Camera2GLSurfaceView(Context context) {
        super(context);
    }

    public Camera2GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        // 创建Camera2Manager
        return new Camera2Manager(context);
    }
}
