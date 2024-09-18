package com.android.xz.gles.filiter;

public interface AFilter {

    void surfaceCreated();

    void surfaceChanged(int width, int height);

    int draw(int textureId, float[] matrix);

    void release();
}
