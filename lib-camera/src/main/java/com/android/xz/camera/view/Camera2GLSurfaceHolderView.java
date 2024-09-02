package com.android.xz.camera.view;

import android.content.Context;
import android.util.AttributeSet;

import com.android.xz.camera.Camera2Manager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.view.base.BaseGLESSurfaceView;

/**
 * 适用Camera2的SurfaceView，自定义opengl
 *
 * @author xiaozhi
 * @since 2024/8/22
 */
public class Camera2GLSurfaceHolderView extends BaseGLESSurfaceView {

    public Camera2GLSurfaceHolderView(Context context) {
        super(context);
    }

    public Camera2GLSurfaceHolderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Camera2GLSurfaceHolderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        return new Camera2Manager(context);
    }
}
