package com.android.xz.view;

import android.content.Context;
import android.util.AttributeSet;

import com.android.xz.camera.Camera2Manager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.view.base.BaseGLESSurfaceView;

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

    public Camera2GLSurfaceHolderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        return new Camera2Manager(context);
    }
}
