package com.android.xz.util;

public class YUVUtils {

    static {
        System.loadLibrary("yuv");
    }

    public static native int nativeNV21ToRGB24(byte[] yuvBytes, byte[] rgb24Bytes, int[] hw, int orientation);
    public static native int nativeNV21ToBGR24(byte[] yuvBytes, byte[] rgb24Bytes, int[] hw, int orientation);

    public native static void  nativeNV21ToI420(byte[] src, int width, int height, byte[] dst);

    public native static void nativeNV21ScaleToI420(byte[] src, int width, int height, byte[] dst, int dstW, int dstH);

    public native static void  nativeI420ToNV12(byte[] src, int width, int height, byte[] dst);

    public native static void  nativeI420ToNV21(byte[] src, int width, int height, byte[] dst);

    public native static void  nativeNV21ToNV12(byte[] src, int width, int height, byte[] dst);

    /**
     * 将nv21转换为yuv420p(I420), YYYYYYYY VUVU ---> YYYYYYYY UU VV
     *
     * @param src
     * @param width
     * @param height
     * @return
     */
    public static byte[] nv21ToYuv420p(byte[] src, int width, int height) {
        int yLength = width * height;
        int uLength = yLength / 4;
        int vLength = yLength / 4;
        int frameSize = yLength + uLength + vLength;
        byte[] yuv420p = new byte[frameSize];
        // Y分量
        System.arraycopy(src, 0, yuv420p, 0, yLength);
        for (int i = 0; i < yLength / 4; i++) {
            // U分量
            yuv420p[yLength + i] = src[yLength + 2 * i + 1];
            // V分量
            yuv420p[yLength + uLength + i] = src[yLength + 2 * i];
        }
        return yuv420p;
    }

    /**
     * 将nv21转换为yuv420p(I420), YYYYYYYY VUVU ---> YYYYYYYY VV UU
     *
     * @param src
     * @param width
     * @param height
     * @return
     */
    public static byte[] nv21ToYV12(byte[] src, int width, int height) {
        int yLength = width * height;
        int uLength = yLength / 4;
        int vLength = yLength / 4;
        int frameSize = yLength + uLength + vLength;
        byte[] yuv420p = new byte[frameSize];
        // Y分量
        System.arraycopy(src, 0, yuv420p, 0, yLength);
        for (int i = 0; i < yLength / 4; i++) {
            // V分量
            yuv420p[yLength + i] = src[yLength + 2 * i];
            // U分量
            yuv420p[yLength + uLength + i] = src[yLength + 2 * i + 1];
        }
        return yuv420p;
    }

    /**
     * 将YUV420Planner（I420）转换为NV21, YYYYYYYY UU VV --> YYYYYYYY VUVU
     *
     * @param src
     * @param width
     * @param height
     * @return
     */
    public static byte[] yuv420pToNV21(byte[] src, int width, int height) {
        int yLength = width * height;
        int uLength = yLength / 4;
        int vLength = yLength / 4;
        int frameSize = yLength + uLength + vLength;
        byte[] nv21 = new byte[frameSize];

        System.arraycopy(src, 0, nv21, 0, yLength); // Y分量
        for (int i = 0; i < yLength / 4; i++) {
            // U分量
            nv21[yLength + 2 * i + 1] = src[yLength + i];
            // V分量
            nv21[yLength + 2 * i] = src[yLength + uLength + i];
        }
        return nv21;
    }

    /**
     * 将YUV420Planner（I420）转换为NV21, YYYYYYYY UU VV --> YYYYYYYY UVUV
     *
     * @param src
     * @param width
     * @param height
     * @return
     */
    public static byte[] yuv420pToNV12(byte[] src, int width, int height) {
        int yLength = width * height;
        int uLength = yLength / 4;
        int vLength = yLength / 4;
        int frameSize = yLength + uLength + vLength;
        byte[] nv12 = new byte[frameSize];

        System.arraycopy(src, 0, nv12, 0, yLength); // Y分量
        for (int i = 0; i < yLength / 4; i++) {
            // U分量
            nv12[yLength + 2 * i] = src[yLength + i];
            // V分量
            nv12[yLength + 2 * i + 1] = src[yLength + uLength + i];
        }
        return nv12;
    }

    /**
     * 将NV21转换为Yuv420sp, YYYYYYYY VUVU  --> YYYYYYYY UVUV
     *
     * @param src
     * @param width
     * @param height
     * @return
     */
    public static void _nv21ToYuv420sp(byte[] src, int width, int height, byte[] dst) {
        int yLength = width * height;
        int uLength = yLength >> 2;
        int vLength = yLength >> 2;
        int frameSize = yLength + uLength + vLength;
        // Y分量
        System.arraycopy(src, 0, dst, 0, yLength);
        for (int i = 0; i < uLength; i++) {
            // U分量
            dst[yLength + 2 * i] = src[yLength + 2 * i + 1];
            // V分量
            dst[yLength + 2 * i + 1] = src[yLength + 2 * i];
        }
    }

    /**
     * 将YUV420SemiPlanner转换为NV21, YYYYYYYY UVUV(yuv420sp)--> YYYYYYYY VUVU(nv21)
     *
     * @param src
     * @param width
     * @param height
     * @return
     */
    public static byte[] yuv420spToNV21(byte[] src, int width, int height) {
        int yLength = width * height;
        int uLength = yLength / 4;
        int vLength = yLength / 4;
        int frameSize = yLength + uLength + vLength;
        byte[] nv21 = new byte[frameSize];
        // Y分量
        System.arraycopy(src, 0, nv21, 0, yLength);
        for (int i = 0; i < yLength / 4; i++) {
            // U分量
            nv21[yLength + 2 * i + 1] = src[yLength + 2 * i];
            // V分量
            nv21[yLength + 2 * i] = src[yLength + 2 * i + 1];
        }
        return nv21;
    }

    /**
     * 将YV12转换为NV21, YYYYYYYY VV UU --> YYYYYYYY VUVU
     *
     * @param src
     * @param width
     * @param height
     * @return
     */
    public static byte[] yv12ToNV21(byte[] src, int width, int height) {
        int yLength = width * height;
        int uLength = yLength / 4;
        int vLength = yLength / 4;
        int frameSize = yLength + uLength + vLength;
        byte[] nv21 = new byte[frameSize];

        System.arraycopy(src, 0, nv21, 0, yLength); // Y分量
        for (int i = 0; i < yLength / 4; i++) {
            // U分量
            nv21[yLength + 2 * i + 1] = src[yLength + vLength + i];
            // V分量
            nv21[yLength + 2 * i] = src[yLength + i];
        }
        return nv21;
    }
}
