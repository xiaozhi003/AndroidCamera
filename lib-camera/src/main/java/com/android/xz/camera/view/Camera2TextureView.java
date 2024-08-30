package com.android.xz.camera.view;

import android.content.Context;
import android.util.AttributeSet;

import com.android.xz.camera.Camera2Manager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.view.base.BaseTextureView;

/**
 * 适用Camera2的TextureView
 * Created by xiaozhi on 2024/8/22.
 */
public class Camera2TextureView extends BaseTextureView {

    public Camera2TextureView(Context context) {
        super(context);
    }

    public Camera2TextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Camera2TextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        // 创建Camera2Manager
        return new Camera2Manager(context);
    }
}
