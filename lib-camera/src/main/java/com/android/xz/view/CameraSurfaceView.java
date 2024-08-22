package com.android.xz.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.android.xz.camera.CameraManager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.util.Logs;
import com.android.xz.view.base.BaseSurfaceView;

/**
 * Created by wangzhi on 2024/8/22.
 */
public class CameraSurfaceView extends BaseSurfaceView {

    public CameraSurfaceView(Context context) {
        super(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logs.i(TAG, "surfaceChanged [" + width + ", " + height + "]");
        mSurfaceWidth = width;
        mSurfaceHeight = height;
    }

    @Override
    public void onOpen() {
        mPreviewSize = getCameraManager().getPreviewSize();
        getCameraManager().startPreview(getSurfaceHolder());
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        return new CameraManager(context);
    }
}
