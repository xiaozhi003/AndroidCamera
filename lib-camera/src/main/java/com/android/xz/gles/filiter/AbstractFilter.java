package com.android.xz.gles.filiter;

import android.content.Context;
import android.opengl.GLES20;


import com.android.xz.gles.OpenGLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @author Lance
 * @date 2018/10/31
 */
public abstract class AbstractFilter {

    protected FloatBuffer mGLVertexBuffer;
    protected FloatBuffer mGLTextureBuffer;

    //顶点着色
    protected int mVertexShaderId;
    //片段着色
    protected int mFragmentShaderId;


    protected int mGLProgramId;
    /**
     * 顶点着色器
     * attribute vec4 position;
     * 赋值给gl_Position(顶点)
     */
    protected int vPosition;
    /**
     * varying vec2 textureCoordinate;
     */
    protected int vCoord;


    /**
     * uniform mat4 vMatrix;
     */
    protected int vMatrix;

    /**
     * 片元着色器
     * Samlpe2D 扩展 samplerExternalOES
     */
    protected int vTexture;


    protected int mOutputWidth;
    protected int mOutputHeight;

    public AbstractFilter(Context context, int vertexShaderId, int fragmentShaderId) {
        this.mVertexShaderId = vertexShaderId;
        this.mFragmentShaderId = fragmentShaderId;
        // 4个点 x，y = 4*2 float 4字节 所以 4*2*4
        mGLVertexBuffer = ByteBuffer.allocateDirect(4 * 2 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLVertexBuffer.clear();
        float[] VERTEX = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f
        };
        mGLVertexBuffer.put(VERTEX);


        mGLTextureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.clear();
        float[] TEXTURE = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f
        };
        mGLTextureBuffer.put(TEXTURE);

        initilize(context);
        initCoordinate();
    }


    protected void initilize(Context context) {
        String vertexSharder = OpenGLUtils.readRawTextFile(context, mVertexShaderId);
        String framentShader = OpenGLUtils.readRawTextFile(context, mFragmentShaderId);
        mGLProgramId = OpenGLUtils.loadProgram(vertexSharder, framentShader);
        // 获得着色器中的 attribute 变量 position 的索引值
        vPosition = GLES20.glGetAttribLocation(mGLProgramId, "vPosition");
        vCoord = GLES20.glGetAttribLocation(mGLProgramId,
                "vCoord");
        vMatrix = GLES20.glGetUniformLocation(mGLProgramId,
                "vMatrix");
        // 获得Uniform变量的索引值
        vTexture = GLES20.glGetUniformLocation(mGLProgramId,
                "vTexture");
    }


    public void onReady(int width, int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }


    public void release() {
        GLES20.glDeleteProgram(mGLProgramId);
    }


    public int onDrawFrame(int textureId) {
        //设置显示窗口
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);

        //使用着色器
        GLES20.glUseProgram(mGLProgramId);

        //传递坐标
        mGLVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, mGLVertexBuffer);
        GLES20.glEnableVertexAttribArray(vPosition);

        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(vCoord);


        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(vTexture, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return textureId;
    }

    //修改坐标
    protected void initCoordinate() {

    }

}
