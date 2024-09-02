package com.android.xz.camera.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.xz.camera.Camera2Manager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.camera.view.base.BaseGLTextureView;

/**
 * 适用Camera2的TextureView，自定义opengl
 *
 * @author xiaozhi
 * @since 2024/8/22
 */
public class Camera2GLTextureView extends BaseGLTextureView {

    public Camera2GLTextureView(@NonNull Context context) {
        super(context);
    }

    public Camera2GLTextureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Camera2GLTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        return new Camera2Manager(context);
    }
}
