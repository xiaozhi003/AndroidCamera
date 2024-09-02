package com.android.xz.camera.view;

import android.content.Context;
import android.util.AttributeSet;

import com.android.xz.camera.CameraManager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.view.base.BaseGLSurfaceView;

/**
 * 适用Camera的GLSurfaceView预览
 *
 * @author xiaozhi
 * @since 2024/8/22
 */
public class CameraGLSurfaceView extends BaseGLSurfaceView {

    public CameraGLSurfaceView(Context context) {
        super(context);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        // 创建CameraManager
        return new CameraManager(context);
    }
}
