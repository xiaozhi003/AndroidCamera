package com.android.xz.camera.view;

import android.content.Context;
import android.util.AttributeSet;

import com.android.xz.camera.CameraManager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.view.base.BaseTextureView;

/**
 * 适用Camera的TextureView
 * Created by xiaozhi on 2024/8/22.
 */
public class CameraTextureView extends BaseTextureView {

    public CameraTextureView(Context context) {
        super(context);
    }

    public CameraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        // 创建CameraManager
        return new CameraManager(context);
    }
}
