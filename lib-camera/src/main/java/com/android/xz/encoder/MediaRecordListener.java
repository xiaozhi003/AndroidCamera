package com.android.xz.encoder;

public interface MediaRecordListener {
    void onStart();

    void onStopped(String videoPath);
}
