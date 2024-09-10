package com.android.xz.gles.filiter;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.android.xz.R;


/**
 * 将SurfaceTexture纹理渲染到FBO或者Screen中
 * 写入fbo (帧缓存)
 */
public class CameraFilter extends AbstractFrameFilter {

    private float[] matrix;

    public CameraFilter(Context context) {
        super(context, R.raw.camera_vertex, R.raw.camera_frag);
    }

    @Override
    public int onDrawFrame(int textureId) {
        //设置显示窗口
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);

        //不调用的话就是默认的操作GLSurfaceView中的纹理了。显示到屏幕上了
        //这里我们还只是把它画到fbo中(缓存)
        if (isFBO) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        }

        //使用着色器
        GLES20.glUseProgram(mGLProgramId);

        //传递坐标
        mGLVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, mGLVertexBuffer);
        GLES20.glEnableVertexAttribArray(vPosition);

        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(vCoord);

        //变换矩阵
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, matrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //因为这一层是摄像头后的第一层，所以需要使用扩展的  GL_TEXTURE_EXTERNAL_OES
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(vTexture, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        if (isFBO) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            //返回fbo的纹理id
            return mFrameBufferTextures[0];
        } else {
            return textureId;
        }
    }


    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }
}
