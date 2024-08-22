package com.android.xz.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.android.xz.camera.Camera2Manager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.util.Logs;
import com.android.xz.view.base.BaseSurfaceView;

/**
 * Created by wangzhi on 2024/8/22.
 */
public class Camera2SurfaceView extends BaseSurfaceView {

    public Camera2SurfaceView(Context context) {
        super(context);
    }

    public Camera2SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Camera2SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logs.i(TAG, "surfaceChanged [" + width + ", " + height + "]");
        mSurfaceWidth = width;
        mSurfaceHeight = height;

        float ratio;
        if (width > height) {
            ratio = height * 1.0f / width;
        } else {
            ratio = width * 1.0f / height;
        }
        if (ratio == mPreviewSize.getHeight() * 1f / mPreviewSize.getWidth()) {
            getCameraManager().startPreview(holder);
        }
    }

    @Override
    public void onOpen() {
        Logs.v(TAG, "onOpen.");
        mPreviewSize = getCameraManager().getPreviewSize();
        getSurfaceHolder().setFixedSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        float ratio;
        if (mSurfaceWidth > mSurfaceHeight) {
            ratio = mSurfaceHeight * 1.0f / mSurfaceWidth;
        } else {
            ratio = mSurfaceWidth * 1.0f / mSurfaceHeight;
        }
        if (ratio == mPreviewSize.getHeight() * 1f / mPreviewSize.getWidth()) {
            getCameraManager().startPreview(getSurfaceHolder());
        }
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        return new Camera2Manager(context);
    }
}
