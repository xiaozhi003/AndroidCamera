package com.android.xz.view;

import android.content.Context;
import android.util.AttributeSet;

import com.android.xz.camera.CameraManager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.view.base.BaseGLESSurfaceView;

public class CameraGLSurfaceHolderView extends BaseGLESSurfaceView {

    public CameraGLSurfaceHolderView(Context context) {
        super(context);
    }

    public CameraGLSurfaceHolderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraGLSurfaceHolderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CameraGLSurfaceHolderView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        return new CameraManager(context);
    }
}
