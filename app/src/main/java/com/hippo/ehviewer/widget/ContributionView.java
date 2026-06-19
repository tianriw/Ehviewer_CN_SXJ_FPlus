package com.hippo.ehviewer.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.Map;

public class ContributionView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Map<Integer, Integer> counts = Collections.emptyMap();
    private int maxCount = 1;

    public ContributionView(Context context) {
        super(context);
    }

    public ContributionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCounts(Map<Integer, Integer> counts) {
        this.counts = counts;
        maxCount = 1;
        for (int value : counts.values()) {
            maxCount = Math.max(maxCount, value);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float gap = getResources().getDisplayMetrics().density * 2f;
        float cell = Math.max(2f, Math.min(
                (getWidth() - gap * 52) / 53f,
                (getHeight() - gap * 6) / 7f));
        for (int day = 1; day <= 366; day++) {
            int zeroBased = day - 1;
            int week = zeroBased / 7;
            int weekday = zeroBased % 7;
            Integer storedCount = counts.get(day);
            int count = storedCount != null ? storedCount : 0;
            paint.setColor(colorFor(count));
            float left = week * (cell + gap);
            float top = weekday * (cell + gap);
            canvas.drawRect(left, top, left + cell, top + cell, paint);
        }
    }

    private int colorFor(int count) {
        if (count <= 0) {
            return 0xffe5e7eb;
        }
        float ratio = (float) count / maxCount;
        if (ratio <= 0.25f) return 0xff9be9a8;
        if (ratio <= 0.5f) return 0xff40c463;
        if (ratio <= 0.75f) return 0xff30a14e;
        return 0xff216e39;
    }
}
