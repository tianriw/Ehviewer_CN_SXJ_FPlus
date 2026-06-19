package com.hippo.ehviewer.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhEngine;
import com.hippo.ehviewer.client.data.FavListUrlBuilder;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.parser.FavoritesParser;
import com.hippo.lib.yorozuya.SimpleHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;

public final class CloudFavoritesSync {

    private static final long AUTO_SYNC_INTERVAL = 24L * 60L * 60L * 1000L;
    private static final int MAX_PAGES_PER_CATEGORY = 200;

    private CloudFavoritesSync() {
    }

    public static void sync(@NonNull Context context, boolean force, @Nullable Callback callback) {
        Context appContext = context.getApplicationContext();
        if (!Settings.isLogin()) {
            notifyFailure(callback, new IllegalStateException("Not signed in"));
            return;
        }
        long now = System.currentTimeMillis();
        if (!force && (!Settings.getAutoSyncCloudFavorites()
                || now - Settings.getLastCloudFavoritesSync() < AUTO_SYNC_INTERVAL)) {
            return;
        }
        EhApplication.getExecutorService(appContext).execute(() -> {
            try {
                int imported = syncInternal(appContext);
                Settings.putLastCloudFavoritesSync(System.currentTimeMillis());
                SimpleHandler.getInstance().post(() -> {
                    if (callback != null) {
                        callback.onSuccess(imported);
                    }
                });
            } catch (Throwable e) {
                notifyFailure(callback, e);
            }
        });
    }

    private static int syncInternal(Context context) throws Throwable {
        OkHttpClient client = EhApplication.getOkHttpClient(context);
        Set<Long> importedGids = new HashSet<>();
        for (int slot = 0; slot < 10; slot++) {
            FavListUrlBuilder builder = new FavListUrlBuilder();
            builder.setFavCat(slot);
            String url = builder.build();
            String categoryName = "";
            List<GalleryInfo> categoryItems = new ArrayList<>();
            for (int page = 0; page < MAX_PAGES_PER_CATEGORY && url != null; page++) {
                FavoritesParser.Result result = EhEngine.getFavorites(
                        null, client, url, Settings.getShowJpnTitle());
                if (result.catArray != null && slot < result.catArray.length) {
                    categoryName = result.catArray[slot];
                }
                if (result.galleryInfoList != null) {
                    categoryItems.addAll(result.galleryInfoList);
                    importedGids.addAll(extractGids(result.galleryInfoList));
                    EhDB.putLocalFavorites(result.galleryInfoList);
                }
                String next = result.nextHref;
                url = next == null || next.isEmpty() || next.equals(url) ? null : next;
            }
            EhDB.replaceCloudFavoriteCategory(slot, categoryName, categoryItems);
        }
        return importedGids.size();
    }

    private static Set<Long> extractGids(List<GalleryInfo> items) {
        Set<Long> result = new HashSet<>();
        for (GalleryInfo item : items) {
            result.add(item.gid);
        }
        return result;
    }

    private static void notifyFailure(@Nullable Callback callback, Throwable error) {
        if (callback == null) {
            return;
        }
        SimpleHandler.getInstance().post(() -> callback.onFailure(error));
    }

    public interface Callback {
        void onSuccess(int imported);

        void onFailure(Throwable error);
    }
}
