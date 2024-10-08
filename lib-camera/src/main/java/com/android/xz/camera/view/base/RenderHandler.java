package com.android.xz.camera.view.base;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * OpenGL ES渲染Camera预览数据线程的通信Handler
 *
 * @author xiaozhi
 * @since 2024/8/22
 */
public class RenderHandler extends Handler {

    private static final String TAG = RenderHandler.class.getSimpleName();
    private static final int MSG_SURFACE_AVAILABLE = 0;
    private static final int MSG_SURFACE_CHANGED = 1;
    private static final int MSG_SURFACE_DESTROYED = 2;
    private static final int MSG_SHUTDOWN = 3;
    private static final int MSG_FRAME_AVAILABLE = 4;
    private static final int MSG_ZOOM_VALUE = 5;
    private static final int MSG_SIZE_VALUE = 6;
    private static final int MSG_ROTATE_VALUE = 7;
    private static final int MSG_POSITION = 8;
    private static final int MSG_REDRAW = 9;
    private static final int MSG_RECORD_STATE = 10;

    // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
    // but no real harm in it.
    private WeakReference<RenderThread> mWeakRenderThread;

    /**
     * Call from render thread.
     */
    public RenderHandler(RenderThread rt) {
        mWeakRenderThread = new WeakReference<>(rt);
    }

    /**
     * Sends the "surface available" message.  If the surface was newly created (i.e.
     * this is called from surfaceCreated()), set newSurface to true.  If this is
     * being called during Activity startup for a previously-existing surface, set
     * newSurface to false.
     * <p>
     * The flag tells the caller whether or not it can expect a surfaceChanged() to
     * arrive very soon.
     * <p>
     * Call from UI thread.
     */
    public void sendSurfaceAvailable(Object surface, boolean newSurface) {
        sendMessage(obtainMessage(MSG_SURFACE_AVAILABLE, newSurface ? 1 : 0, 0, surface));
    }

    /**
     * Sends the "surface changed" message, forwarding what we got from the SurfaceHolder.
     * <p>
     * Call from UI thread.
     */
    public void sendSurfaceChanged(int format, int width,
                                   int height) {
        // ignore format
        sendMessage(obtainMessage(MSG_SURFACE_CHANGED, width, height));
    }

    /**
     * Sends the "shutdown" message, which tells the render thread to halt.
     * <p>
     * Call from UI thread.
     */
    public void sendSurfaceDestroyed() {
        sendMessage(obtainMessage(MSG_SURFACE_DESTROYED));
    }

    /**
     * Sends the "shutdown" message, which tells the render thread to halt.
     * <p>
     * Call from UI thread.
     */
    public void sendShutdown() {
        sendMessage(obtainMessage(MSG_SHUTDOWN));
    }

    /**
     * Sends the "frame available" message.
     * <p>
     * Call from UI thread.
     */
    public void sendFrameAvailable() {
        sendMessage(obtainMessage(MSG_FRAME_AVAILABLE));
    }

    /**
     * Sends the "rotation" message.
     * <p>
     * Call from UI thread.
     */
    public void sendRotate(int rotation, int cameraId) {
        sendMessage(obtainMessage(MSG_ROTATE_VALUE, rotation, cameraId));
    }

    /**
     * Sends the "preview size" message.
     * <p>
     * Call from UI thread.
     */
    public void sendPreviewSize(int width, int height) {
        sendMessage(obtainMessage(MSG_SIZE_VALUE, width, height));
    }

    public void sendRecordState(boolean state) {
        sendMessage(obtainMessage(MSG_RECORD_STATE, state));
    }

    @Override  // runs on RenderThread
    public void handleMessage(Message msg) {
        int what = msg.what;
        //Log.d(TAG, "RenderHandler [" + this + "]: what=" + what);

        RenderThread renderThread = mWeakRenderThread.get();
        if (renderThread == null) {
            Log.w(TAG, "RenderHandler.handleMessage: weak ref is null");
            return;
        }

        switch (what) {
            case MSG_SURFACE_AVAILABLE:
                renderThread.surfaceAvailable(msg.obj, msg.arg1 != 0);
                break;
            case MSG_SURFACE_CHANGED:
                renderThread.surfaceChanged(msg.arg1, msg.arg2);
                break;
            case MSG_SURFACE_DESTROYED:
                renderThread.surfaceDestroyed();
                break;
            case MSG_SHUTDOWN:
                renderThread.shutdown();
                break;
            case MSG_FRAME_AVAILABLE:
                renderThread.frameAvailable();
                break;
            case MSG_SIZE_VALUE:
                renderThread.setCameraPreviewSize(msg.arg1, msg.arg2);
                break;
            case MSG_ROTATE_VALUE:
//                    renderThread.setRotate(msg.arg1, msg.arg2);
                break;
            case MSG_RECORD_STATE:
                renderThread.changeRecordingState((boolean) msg.obj);
                break;
            default:
                throw new RuntimeException("unknown message " + what);
        }
    }
}
