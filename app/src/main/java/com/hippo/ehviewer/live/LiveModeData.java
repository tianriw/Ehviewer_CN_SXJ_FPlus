package com.hippo.ehviewer.live;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class LiveModeData {

    public static final String[] TITLES = {
            "测试图集 1",
            "测试图集 2",
            "测试图集 3"
    };

    public static final String[] TAGS = {
            "我是测试1",
            "我是测试2  我是测试3  我是测试4  我是测试5",
            "我是测试6  我是测试7  我是测试8  我是测试9  我是测试10"
    };

    private static final int[] COLORS = {
            Color.rgb(35, 105, 155),
            Color.rgb(142, 70, 95),
            Color.rgb(47, 122, 82)
    };

    private LiveModeData() {
    }

    @NonNull
    public static File ensureGallery(@NonNull Context context, int galleryIndex)
            throws IOException {
        File dir = new File(context.getFilesDir(), "live_mode/gallery_" + (galleryIndex + 1));
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create live mode gallery");
        }
        for (int page = 0; page < 3; page++) {
            File image = new File(dir, String.format("%04d.png", page + 1));
            if (!image.isFile()) {
                createPage(image, galleryIndex, page);
            }
        }
        return dir;
    }

    private static void createPage(File file, int galleryIndex, int page) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(720, 960, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(COLORS[galleryIndex]);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(52f);
        canvas.drawText("直播模式", 360f, 250f, paint);

        paint.setTextSize(42f);
        canvas.drawText(TITLES[galleryIndex], 360f, 365f, paint);
        paint.setTextSize(30f);
        canvas.drawText("第 " + (page + 1) + " / 3 页", 360f, 450f, paint);

        paint.setTextSize(24f);
        String[] tags = TAGS[galleryIndex].split("  ");
        float y = 565f;
        for (String tag : tags) {
            canvas.drawText(tag, 360f, y, paint);
            y += 44f;
        }

        try (FileOutputStream output = new FileOutputStream(file)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw new IOException("Unable to encode live mode page");
            }
        } finally {
            bitmap.recycle();
        }
    }
}
