package com.android.xz.camerademo.view;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.xz.util.Logs;

public class CaptureButton extends View {

    private static final String TAG = CaptureButton.class.getSimpleName();
    private Context mContext;
    private int mWidth;
    private int mHeight;
    private int mBgColor = Color.parseColor("#CCCCCC");
    private int mRecordColor = Color.parseColor("#FF0000");
    private float mHalfLength;
    private float mCircleRadius;
    private Paint mBgPaint;
    private boolean mRecording = false;
    private ObjectAnimator mCaptureAnimator;
    private ObjectAnimator mRecordAnimator;
    private AnimatorSet mRecordAnimatorSet;

    public CaptureButton(Context context) {
        super(context);
        init(context);
    }

    public CaptureButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CaptureButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CaptureButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgPaint.setColor(mBgColor);
        mBgPaint.setStrokeWidth(6);

        setOnClickListener(onClickListener);
        setLongClickable(true);
        setOnLongClickListener(onLongClickListener);
    }

    public void setHalfLength(float halfLength) {
        mHalfLength = halfLength;
        invalidate();
    }

    public void setCircleRadius(float circleRadius) {
        mCircleRadius = circleRadius;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (width > height) {
            setMeasuredDimension(height, height);
            mWidth = height;
            mHeight = height;
        } else {
            setMeasuredDimension(width, width);
            mWidth = width;
            mHeight = width;
        }
        Logs.i(TAG, "width:" + width);
        setCircleRadius(mWidth * 9 / 10 / 2);
    }

    public void stopRecord() {
        Logs.i(TAG, "stopRecord...");
        if (mRecording) {
            mRecording = false;
            setCircleRadius(mWidth * 9 / 10 / 2);
            invalidate();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        mBgPaint.setColor(mBgColor);
        int centerX = mWidth / 2;
        int centerY = mHeight / 2;
        int radius = mWidth / 2;
        canvas.drawCircle(centerX, centerY, radius, mBgPaint);

        mBgPaint.setColor(mRecordColor);
        canvas.drawRoundRect(new RectF(centerX - mHalfLength, centerY - mHalfLength, centerX + mHalfLength, centerY + mHalfLength), 6, 6, mBgPaint);
        mBgPaint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, mCircleRadius, mBgPaint);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mRecording) {
                stopRecord();
                if (mClickListener != null) {
                    mClickListener.onStopRecord();
                }
            } else {
                if (mClickListener != null) {
                    mClickListener.onCapture();
                }
            }
        }
    };

    private View.OnLongClickListener onLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (!mRecording) {
                mRecording = true;
                if (mCaptureAnimator == null) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(CaptureButton.this, "circleRadius", mWidth * 9 / 10 / 2, 0);
                    animator.setInterpolator(new DecelerateInterpolator());
                    animator.setDuration(100);
                    ((ValueAnimator) animator).addUpdateListener(animation -> setCircleRadius((float) animation.getAnimatedValue()));
                    mCaptureAnimator = animator;
                }
                if (mRecordAnimator == null) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(CaptureButton.this, "halfLength", 0, mWidth / 6);
                    animator.setInterpolator(new DecelerateInterpolator());
                    animator.setDuration(100);
                    ((ValueAnimator) animator).addUpdateListener(animation -> setHalfLength((float) animation.getAnimatedValue()));
                    mRecordAnimator = animator;
                }
                if (mRecordAnimatorSet == null) {
                    mRecordAnimatorSet = new AnimatorSet();
                }
                mRecordAnimatorSet.playSequentially(mCaptureAnimator, mRecordAnimator);
                mRecordAnimatorSet.start();
                if (mClickListener != null) {
                    mClickListener.onStartRecord();
                }
            }
            return true;
        }
    };

    public void setClickListener(ClickListener clickListener) {
        mClickListener = clickListener;
    }

    private ClickListener mClickListener;

    public interface ClickListener {
        void onCapture();

        void onStartRecord();

        void onStopRecord();
    }
}
