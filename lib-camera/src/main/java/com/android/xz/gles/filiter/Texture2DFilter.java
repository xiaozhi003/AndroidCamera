package com.android.xz.gles.filiter;

import android.opengl.GLES20;

import com.android.xz.gles.GLESUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 将离屏渲染的数据绘制到屏幕中
 */
public class Texture2DFilter implements AFilter {

    /**
     * 绘制的流程
     * 1.顶点着色程序 - 用于渲染形状的顶点的 OpenGL ES 图形代码
     * 2.片段着色器 - 用于渲染具有特定颜色或形状的 OpenGL ES 代码 纹理。
     * 3.程序 - 包含您想要用于绘制的着色器的 OpenGL ES 对象 一个或多个形状
     * <p>
     * 您至少需要一个顶点着色器来绘制形状，以及一个 片段着色器来为该形状着色。
     * 这些着色器必须经过编译，然后添加到 OpenGL ES 程序中，该程序随后用于绘制 形状。
     */

    // 顶点着色器代码
    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "attribute vec4 vPosition;\n" +
                    "attribute vec2 vTexCoordinate;\n" +
                    "varying vec2 aTexCoordinate;\n" +
                    "void main() {\n" +
                    // the matrix must be included as a modifier of gl_Position
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "  gl_Position = vPosition;\n" +
                    "  aTexCoordinate = vTexCoordinate;\n" +
                    "}";

    // 片段着色器代码
    private final String fragmentShaderCode =
            "precision mediump float;\n" +
                    "uniform sampler2D vTexture;\n" +
                    "varying vec2 aTexCoordinate;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(vTexture, aTexCoordinate);\n" +
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
     * 顶点坐标系中原点(0,0)在画布中心
     * 向左为x轴正方向
     * 向上为y轴正方向
     * 画布四个角坐标如下：
     * (-1, 1),(1, 1)
     * (-1,-1),(1,-1)
     */
    private float vertexCoords[] = {
            -1.0f, 1.0f,  // 左上
            -1.0f, -1.0f, // 左下
            1.0f, 1.0f,   // 右上
            1.0f, -1.0f   // 右下
    };

    /**
     * 纹理坐标数组
     * 这里我们需要注意纹理坐标系，原点(0,0s)在画布左下角
     * 向左为x轴正方向
     * 向上为y轴正方向
     * 画布四个角坐标如下：
     * (0,1),(1,1)
     * (0,0),(1,0)
     */
    private float textureCoords[] = {
            0.0f, 1.0f, // 左上
            0.0f, 0.0f, // 左下
            1.0f, 1.0f, // 右上
            1.0f, 0.0f, // 右下
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

    private final int vertexCount = vertexCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    public Texture2DFilter() {
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

    @Override
    public void surfaceCreated() {
        // 创建OpenGLES程序
        mProgram = GLESUtils.createProgram(vertexShaderCode, fragmentShaderCode);

        // 获取顶点着色器vPosition成员的句柄
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        // 获取顶点着色器中纹理坐标的句柄
        mTexCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "vTexCoordinate");
        // 获取Texture句柄
        mTexHandle = GLES20.glGetUniformLocation(mProgram, "vTexture");
    }

    @Override
    public void surfaceChanged(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public int draw(int textureId, float[] matrix) {
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

        // 激活纹理编号0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // 绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        // 设置纹理采样器编号，该编号和glActiveTexture中设置的编号相同
        GLES20.glUniform1i(mTexHandle, 0);

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLESUtils.checkGlError("glDrawArrays");

        // 取消绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        // 禁用顶点阵列
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordinateHandle);

        return textureId;
    }

    @Override
    public void release() {
        GLES20.glDeleteProgram(mProgram);
        mProgram = -1;
    }
}
