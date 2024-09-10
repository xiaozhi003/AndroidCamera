package com.android.xz.gles.filiter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.android.xz.R;
import com.android.xz.gles.OpenGLUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * 贴纸滤镜
 */
public class TimeFilter extends AbstractFrameFilter {

    private Bitmap mBitmap;
    private Canvas bitmapCanvas;
    private Paint textPaint;
    private SimpleDateFormat textFormat;

    public TimeFilter(Context context) {
        super(context, R.raw.base_vertex, R.raw.base_frag);
    }


    @Override
    public void onReady(int width, int height) {
        super.onReady(width, height);
        initBitmapCanvas();
    }


    @Override
    public int onDrawFrame(int textureId) {

        //设置显示窗口
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);

        //不调用的话就是默认的操作glsurfaceview中的纹理了。显示到屏幕上了
        //这里我们还只是把它画到fbo中(缓存)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);

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
        //因为这一层是摄像头后的第一层，所以需要使用扩展的  GL_TEXTURE_EXTERNAL_OES
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(vTexture, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        onDrawStick();
        //返回fbo的纹理id
        return mFrameBufferTextures[0];
    }

    private void initBitmapCanvas() {
        float aFontSize = mOutputWidth / 15;
        textPaint = new Paint();
        textPaint.setTextSize(aFontSize);
        textPaint.setFakeBoldText(false);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setShadowLayer(4, 0, 0, Color.BLACK);

        textFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.CHINA);

        mBitmap = Bitmap.createBitmap(mOutputWidth / 2, mOutputWidth / 10, Bitmap.Config.ARGB_8888);
        // Creates a new canvas that will draw into a bitmap instead of rendering into the screen
        bitmapCanvas = new Canvas(mBitmap);
    }

    private void drawTextToBitmap() {
        String aText = textFormat.format(new Date());

        mBitmap.eraseColor(Color.RED);

        // Set start drawing position to [1, base_line_position]
        // The base_line_position may vary from one font to another but it usually is equal to 75% of font size (height).
        bitmapCanvas.drawText(aText, 1, 1.0f + mBitmap.getHeight() * 0.75f, textPaint);
    }

    private void onDrawStick() {
        //帖纸画上去
        //开启混合模式 ： 将多张图片进行混合(贴图)
        GLES20.glEnable(GLES20.GL_BLEND);
        //设置贴图模式
        // 1：src 源图因子 ： 要画的是源  (耳朵)
        // 2: dst : 已经画好的是目标  (从其他filter来的图像)
        //画耳朵的时候  GL_ONE:就直接使用耳朵的所有像素 原本是什么样子 我就画什么样子
        // 表示用1.0减去源颜色的alpha值来作为因子
        //  耳朵不透明 (0,0 （全透明）- 1.0（不透明）) 目标图对应位置的像素就被融合掉了 不见了
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        drawTextToBitmap();

        //画画
        //不是画全屏 定位到相应的位置
        //设置显示窗口
        //起始的位置
        float x = 100;
        float y = 200;
        GLES20.glViewport((int) x, (int) y,
                mBitmap.getWidth(),
                mBitmap.getHeight());

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
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
        int[] mTextureId = new int[1];
        OpenGLUtils.glGenTextures(mTextureId);
        //表示后续的操作 就是作用于这个纹理上
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId[0]);
        // 将 Bitmap与纹理id 绑定起来
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);

        GLES20.glUniform1i(vTexture, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);


        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        //关闭
        GLES20.glDisable(GLES20.GL_BLEND);

//        mBitmap.recycle();
    }

    @Override
    public void release() {
        super.release();
        mBitmap.recycle();
    }
}
