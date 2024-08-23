package com.android.xz.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.xz.camera.Camera2Manager;
import com.android.xz.camera.ICameraManager;
import com.android.xz.view.base.BaseGLTextureView;

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

    public Camera2GLTextureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public ICameraManager createCameraManager(Context context) {
        return new Camera2Manager(context);
    }
}
