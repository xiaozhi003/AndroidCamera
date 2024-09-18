package com.android.xz.gles.filiter;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.android.xz.gles.GLESUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 渲染Camera数据，可离屏渲染到FBO中
 */
public class CameraFilter implements AFilter {

    /**
     * 绘制的流程
     * 1.顶点着色程序 - 用于渲染形状的顶点的 OpenGL ES 图形代码
     * 2.片段着色器 - 用于渲染具有特定颜色或形状的 OpenGL ES 代码 纹理。
     * 3.程序 - 包含您想要用于绘制的着色器的 OpenGL ES 对象 一个或多个形状
     * <p>
     * 您至少需要一个顶点着色器来绘制形状，以及一个 片段 着色器来为该形状着色。
     * 这些着色器必须经过编译，然后添加到 OpenGL ES 程序中，该程序随后用于绘制 形状。
     */

    // 顶点着色器代码
    private final String vertexShaderCode =
            // 顶点矩阵
            "uniform mat4 uMVPMatrix;\n" +
                    // 纹理矩阵
                    "uniform mat4 uTexPMatrix;\n" +
                    // 顶点坐标
                    "attribute vec4 vPosition;\n" +
                    // 输入纹理坐标
                    "attribute vec4 vTexCoordinate;\n" +
                    // 输出纹理坐标
                    "varying vec2 aTexCoordinate;\n" +
                    "void main() {\n" +
                    // the matrix must be included as a modifier of gl_Position
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "  gl_Position = uMVPMatrix * vPosition;\n" +
                    "  aTexCoordinate = (uTexPMatrix * vTexCoordinate).xy;\n" +
                    "}";

    // 片段着色器代码
    private final String fragmentShaderCode =
            // 使用扩展纹理的声明
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    // 使用samplerExternalOES纹理采样器
                    "uniform samplerExternalOES vTexture;\n" +
                    "varying vec2 aTexCoordinate;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(vTexture, aTexCoordinate);\n" +
                    "}\n";

    // 片段着色器代码，黑白滤镜
    private final String grayFragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "uniform samplerExternalOES vTexture;\n" +
                    "varying vec2 aTexCoordinate;\n" +
                    "void main() {\n" +
                    "  vec4 rgba = texture2D(vTexture, aTexCoordinate);\n" +
                    "  float color = rgba.r * 0.3 + rgba.g * 0.59 + rgba.b * 0.11;\n" +
                    "  gl_FragColor = vec4(color, color, color, 1.0);\n" +
                    "}\n";

    /**
     * OpenGL程序句柄
     */
    private int mProgram;

    /**
     * 顶点坐标缓冲区
     */
    private FloatBuffer mVertexBuffer;
    /**
     * 纹理坐标缓冲区
     */
    private FloatBuffer mTextureBuffer;

    /**
     * 此数组中每个顶点的坐标数
     */
    static final int COORDS_PER_VERTEX = 2;

    /**
     * 顶点坐标数组
     */
    private float vertexCoords[] = {
            -1.0f, 1.0f,   // 左上
            -1.0f, -1.0f,  // 左下
            1.0f, 1.0f,    // 右上
            1.0f, -1.0f};  // 右下

    /**
     * 纹理坐标数组
     */
    private float textureCoords[] = {
            0.0f, 1.0f, // 左下
            0.0f, 0.0f, // 左上
            1.0f, 1.0f, // 右下
            1.0f, 0.0f, // 右上
    };

    /**
     * 顶点坐标句柄
     */
    private int mPositionHandle;
    /**
     * 纹理坐标句柄
     */
    private int mTexCoordinateHandle;
    /**
     * 纹理贴图句柄
     */
    private int mTexHandle;
    /**
     * 顶点坐标变换矩阵句柄
     */
    private int vPMatrixHandle;
    /**
     * 纹理坐标变换矩阵句柄
     */
    private int vTexPMatrixHandle;

    private final int vertexCount = vertexCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // vPMatrix是“模型视图投影矩阵”的缩写
    // 最终变化矩阵
    private final float[] mMVPMatrix = new float[16];
    // 投影矩阵
    private final float[] mProjectionMatrix = new float[16];
    // 相机矩阵
    private final float[] mViewMatrix = new float[16];

    //FBO id
    protected int[] mFrameBuffers;
    //fbo 纹理id
    protected int[] mFrameBufferTextures;
    // 是否使用离屏渲染
    protected boolean isFBO;

    public CameraFilter() {
        // 初始化形状坐标的顶点字节缓冲区
        mVertexBuffer = ByteBuffer.allocateDirect(vertexCoords.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexCoords);
        mVertexBuffer.position(0);

        // 初始化纹理坐标顶点字节缓冲区
        mTextureBuffer = ByteBuffer.allocateDirect(textureCoords.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureCoords);
        mTextureBuffer.position(0);
    }

    public void setFBO(boolean FBO) {
        isFBO = FBO;
    }

    public void createFrameBuffers(int width, int height) {
        if (mFrameBuffers != null) {
            destroyFrameBuffers();
        }
        //fbo的创建 (缓存)
        //1、创建fbo （离屏屏幕）
        mFrameBuffers = new int[1];
        // 1、创建几个fbo 2、保存fbo id的数据 3、从这个数组的第几个开始保存
        GLES20.glGenFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);

        //2、创建属于fbo的纹理
        mFrameBufferTextures = new int[1]; //用来记录纹理id
        //创建纹理
        int textureId = GLESUtils.create2DTexture();
        mFrameBufferTextures[0] = textureId;

        //让fbo与 纹理发生关系
        //创建一个 2d的图像
        // 目标 2d纹理+等级 + 格式 +宽、高+ 格式 + 数据类型(byte) + 像素数据
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        // 让fbo与纹理绑定起来 ， 后续的操作就是在操作fbo与这个纹理上了
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
        //解绑
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public void destroyFrameBuffers() {
        //删除fbo的纹理
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        //删除fbo
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    @Override
    public void surfaceCreated() {
        // 创建OpenGLES程序
        mProgram = GLESUtils.createProgram(vertexShaderCode, fragmentShaderCode);

        // 获取顶点着色器vPosition成员的句柄
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        // 获取顶点着色器中纹理坐标的句柄
        mTexCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoordinate");
        // 获取绘制矩阵句柄
        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        // 获取Texture句柄
        mTexHandle = GLES20.glGetUniformLocation(mProgram, "vTexture");
        vTexPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uTexPMatrix");
    }

    @Override
    public void surfaceChanged(int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        //设置正交矩阵
        Matrix.orthoM(mProjectionMatrix, 0, -1, 1, -1, 1, 3, 7);

        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0,
                0, 0, 7.0f,
                0f, 0f, 0f,
                0f, 1.0f, 0.0f);
        //计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        if (isFBO) {
            createFrameBuffers(width, height);
        }
    }

    @Override
    public int draw(int textureId, float[] matrix) {
        GLESUtils.checkGlError("draw start");
        //不调用的话就是默认的操作GLSurfaceView中的纹理了。显示到屏幕上了
        //这里我们还只是把它画到fbo中(缓存)
        if (isFBO) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        }

        // 将程序添加到OpenGL ES环境
        GLES20.glUseProgram(mProgram);
        GLESUtils.checkGlError("glUseProgram");

        // 为正方形顶点启用控制句柄
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLESUtils.checkGlError("glEnableVertexAttribArray");
        // 写入坐标数据
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, mVertexBuffer);
        GLESUtils.checkGlError("glVertexAttribPointer");

        // 启用纹理坐标控制句柄
        GLES20.glEnableVertexAttribArray(mTexCoordinateHandle);
        GLESUtils.checkGlError("glEnableVertexAttribArray");
        // 写入坐标数据
        GLES20.glVertexAttribPointer(mTexCoordinateHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, mTextureBuffer);
        GLESUtils.checkGlError("glVertexAttribPointer");

        // 将投影和视图变换矩阵传递给着色器
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLESUtils.checkGlError("glUniformMatrix4fv");
        // 将纹理变换矩阵传递给着色器
        GLES20.glUniformMatrix4fv(vTexPMatrixHandle, 1, false, matrix, 0);
        GLESUtils.checkGlError("glUniformMatrix4fv");

        // 激活纹理编号0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // 绑定纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        // 设置纹理采样器编号，该编号和glActiveTexture中设置的编号相同
        GLES20.glUniform1i(mTexHandle, 0);

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLESUtils.checkGlError("glDrawArrays");

        // 取消绑定纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        // 禁用顶点阵列
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordinateHandle);
        // 将程序从OpenGLES环境中去除
        GLES20.glUseProgram(0);

        if (isFBO) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            //返回fbo的纹理id
            return mFrameBufferTextures[0];
        } else {
            return textureId;
        }
    }

    @Override
    public void release() {
        GLES20.glDeleteProgram(mProgram);
        mProgram = -1;
        destroyFrameBuffers();
    }
}
