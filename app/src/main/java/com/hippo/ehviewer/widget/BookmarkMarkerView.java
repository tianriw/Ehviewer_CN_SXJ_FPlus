/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A transparent overlay that draws tick marks on the gallery seek bar for bookmarked pages.
 * Positioned as a sibling of {@link ReversibleSeekBar} inside the slider's FrameLayout.
 */
public class BookmarkMarkerView extends View {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Integer> mPages = new ArrayList<>();
    private int mSize = 0;
    private boolean mReverse = false;

    public BookmarkMarkerView(Context context) {
        super(context);
        init();
    }

    public BookmarkMarkerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BookmarkMarkerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint.setColor(0xFFFF6B6B);
        mPaint.setStrokeWidth(6f);
        setWillNotDraw(false);
    }

    /**
     * @param pages  bookmarked page indices (0-based)
     * @param size   total page count
     * @param reverse true for right-to-left reading mode (mirror marker positions)
     */
    public void setBookmarks(@Nullable List<Integer> pages, int size, boolean reverse) {
        mPages.clear();
        if (pages != null) {
            mPages.addAll(pages);
        }
        mSize = size;
        mReverse = reverse;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mSize <= 1 || mPages.isEmpty() || getWidth() <= 0) {
            return;
        }
        int width = getWidth();
        int height = getHeight();
        for (int page : mPages) {
            if (page < 0 || page >= mSize) {
                continue;
            }
            float ratio = (float) page / (float) (mSize - 1);
            if (mReverse) {
                ratio = 1f - ratio;
            }
            float x = ratio * width;
            canvas.drawLine(x, 0, x, height, mPaint);
        }
    }
}
