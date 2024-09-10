package com.android.xz.gles.filiter;

import android.content.Context;

import com.android.xz.R;

/**
 * 从FBO中渲染到屏幕上
 */
public class ScreenFilter extends AbstractFilter {

    public ScreenFilter(Context context) {
        super(context, R.raw.base_vertex, R.raw.base_frag);
    }

}
