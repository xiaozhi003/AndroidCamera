package com.android.xz.camera.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.xz.camera.CameraManager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.view.base.BaseGLTextureView;

/**
 * 适用Camera的TextureView，自定义opengl
 *
 * @author xiaozhi
 * @since 2024/8/22
 */
public class CameraGLTextureView extends BaseGLTextureView {

    public CameraGLTextureView(@NonNull Context context) {
        super(context);
    }

    public CameraGLTextureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraGLTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        return new CameraManager(context);
    }
}
