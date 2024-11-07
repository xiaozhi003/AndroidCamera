/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.android.xz.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.android.xz.util.Logs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * This class receives video images as ByteBuffer(strongly recommend direct ByteBuffer) as NV21(YUV420SP)
 * and encode them to h.264.
 * If you use this directly with IFrameCallback, you should know UVCCamera and it backend native libraries
 * never execute color space conversion. This means that color tone of resulted movie will be different
 * from that you expected/can see on screen.
 */
public class MediaVideoBufferEncoder extends MediaEncoder implements IVideoEncoder {
    private static final boolean DEBUG = Logs.issIsLogEnabled();  // TODO set false on release
    private static final String TAG = "MediaVideoBufferEncoder";

    private static final String MIME_TYPE = "video/avc";

    // parameters for recording
    private static final int FRAME_RATE = 15;
    private static float BPP = 1.0f;

    private final int mWidth, mHeight;
    protected int mColorFormat;

    public MediaVideoBufferEncoder(final MediaMuxerWrapper muxer, final int width, final int height, final MediaEncoderListener listener) {
        super(muxer, listener);
        if (DEBUG) Log.i(TAG, "MediaVideoEncoder: ");
        mWidth = width;
        mHeight = height;
    }

    public int getColorFormat() {
        return mColorFormat;
    }

    public static void setBPP(float BPP) {
        if (BPP > 1.0f || BPP < 0f) {
            MediaVideoBufferEncoder.BPP = 0.25f;
            return;
        }
        MediaVideoBufferEncoder.BPP = BPP;
    }

    public void encode(final ByteBuffer buffer) {
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) return;
        }
        if (mMediaCodec == null) {
            Log.e(TAG, "mMediaCodec is null");
            return;
        }
        encode(buffer, buffer.capacity(), getPTSUs());
    }

    @Override
    protected void prepare() throws IOException {
        if (DEBUG) Log.i(TAG, "prepare: ");
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;

        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
        if (videoCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Logs.i(TAG, "codec:" + videoCodecInfo.getName() + ", " + videoCodecInfo.isVendor() + ", " + getSupportColorFormatStr(videoCodecInfo, MIME_TYPE));
        } else {
            Logs.i(TAG, "codec:" + videoCodecInfo.getName() + ", " + getSupportColorFormatStr(videoCodecInfo, MIME_TYPE));
        }
        if (DEBUG) Log.i(TAG, "selected codec: " + videoCodecInfo.getName());
        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
        format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 描述编码器要使用的所需比特率模式的键
            // BITRATE_MODE_CQ: 表示完全不控制码率，尽最大可能保证图像质量
            // BITRATE_MODE_CBR: 表示编码器会尽量把输出码率控制为设定值
            // BITRATE_MODE_VBR: 表示编码器会根据图像内容的复杂度（实际上是帧间变化量的大小）来动态调整输出码率，图像复杂则码率高，图像简单则码率低；
            boolean isBitrateModeSupported = videoCodecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).getEncoderCapabilities().isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            Logs.i(TAG, "isBitrateModeSupported:" + isBitrateModeSupported);
            if (isBitrateModeSupported) {
                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            }
        }
        if (DEBUG) Log.i(TAG, "format: " + format);

        // 创建MediaCodec，此时是Uninitialized 状态
        mMediaCodec = MediaCodec.createByCodecName(videoCodecInfo.getName());
        // 调用 configure 进入 Configured 状态
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // 调用 start 进入 Executing 状态，开始编解码工作
        mMediaCodec.start();
        if (DEBUG) Log.i(TAG, "prepare finishing");
        if (mListener != null) {
            mListener.onPrepared(this);
        }
    }

    private int calcBitRate() {
        final int bitrate = (int) (BPP * FRAME_RATE * mWidth * mHeight);
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return null if no codec matched
     */
    @SuppressWarnings("deprecation")
    protected final MediaCodecInfo selectVideoCodec(final String mimeType) {
        if (DEBUG) Log.v(TAG, "selectVideoCodec:");

        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        // 硬编码器列表
        List<MediaCodecInfo> hardwareCodecInfoList = new ArrayList<>();
        // 软编码器列表
        List<MediaCodecInfo> softwareCodecInfoList = new ArrayList<>();

        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {  // skipp decoder
                continue;
            }

            /**
             * 硬件编解码器
             * OMX.qcom.video.encoder.heic
             * OMX.qcom.video.decoder.avc
             * OMX.qcom.video.decoder.avc.secure
             * OMX.qcom.video.decoder.mpeg2
             * OMX.google.gsm.decoder
             * OMX.qti.video.decoder.h263sw
             * c2.qti.avc.decoder
             *
             * 软件编解码器，通常以OMX.google或者c2.android开头
             * OMX.google.h264.encoder
             * c2.android.aac.decoder
             * c2.android.aac.decoder
             * c2.android.aac.encoder
             * c2.android.aac.encoder
             * c2.android.amrnb.decoder
             * c2.android.amrnb.decoder
             */
            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                        if (codecInfo.getName().contains("qcom") || codecInfo.getName().contains("c2.qti")|| codecInfo.getName().contains("c2.android")) continue;
                        if (codecInfo.isVendor()) { // 硬解码
                            hardwareCodecInfoList.add(codecInfo);
                        } else { // 软解码
                            softwareCodecInfoList.add(codecInfo);
                        }
                    } else {
                        if (codecInfo.getName().contains("google") || codecInfo.getName().startsWith("c2.android")) { // 软解码
                            softwareCodecInfoList.add(codecInfo);
                        } else { // 硬解码
                            hardwareCodecInfoList.add(codecInfo);
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Logs.i(TAG, "codec:" + codecInfo.getName() + ", " + codecInfo.isVendor() + ", " + getSupportColorFormatStr(codecInfo, mimeType));
                    } else {
                        Logs.i(TAG, "codec:" + codecInfo.getName() + ", " + getSupportColorFormatStr(codecInfo, mimeType));
                    }
                }
            }
        }

        for (int i = 0; i < recognizedFormats.length; i++) {
            int colorFormat = recognizedFormats[i];
            for (MediaCodecInfo codecInfo : hardwareCodecInfoList) {
                if (containsColorFormatByCodec(colorFormat, codecInfo, mimeType)) {
                    mColorFormat = colorFormat;
                    return codecInfo;
                }
            }
        }
        for (int i = 0; i < recognizedFormats.length; i++) {
            int colorFormat = recognizedFormats[i];
            for (MediaCodecInfo codecInfo : softwareCodecInfoList) {
                if (containsColorFormatByCodec(colorFormat, codecInfo, mimeType)) {
                    mColorFormat = colorFormat;
                    return codecInfo;
                }
            }
        }

        return null;
    }

    protected static final boolean containsColorFormatByCodec(int requestColorFormat, final MediaCodecInfo codecInfo, final String mimeType) {
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        for (int i = 0; i < caps.colorFormats.length; i++) {
            if (requestColorFormat == caps.colorFormats[i]) {
                return true;
            }
        }
        return false;
    }

    protected static final String getSupportColorFormatStr(final MediaCodecInfo codecInfo, final String mimeType) {
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < caps.colorFormats.length; i++) {
            sb.append(caps.colorFormats[i]);
            if (i == caps.colorFormats.length - 1) {
                sb.append("]");
            } else {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * color formats that we can use in this class
     */
    protected static int[] recognizedFormats;

    /**
     A5. The color formats for the camera output and the MediaCodec encoder input are different. Camera supports YV12 (planar YUV 4:2:0) and NV21 (semi-planar YUV 4:2:0). The MediaCodec encoders support one or more of:

     #19 COLOR_FormatYUV420Planar (I420)
     #20 COLOR_FormatYUV420PackedPlanar (also I420)
     #21 COLOR_FormatYUV420SemiPlanar (NV12)
     #39 COLOR_FormatYUV420PackedSemiPlanar (also NV12)
     #0x7f000100 COLOR_TI_FormatYUV420PackedSemiPlanar (also also NV12)
     */
    static {
        recognizedFormats = new int[]{
                // 最常用的
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar, // 19， I420，测试通过
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar, // 21， NV12，测试通过
                // 其他不常用
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar, // 20， I420，测试通过
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar, // 39， NV12, 测试通过

//                MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar, //  2141391872, NV21, 测试通过，该格式部分设备录制绿屏(不支持部分分辨率的编解码)
//                MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar, // 2130706688, NV12, 未测试
        };
    }
}
