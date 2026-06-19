package com.hippo.ehviewer.sync;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhEngine;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.client.data.userTag.UserTag;
import com.hippo.ehviewer.client.data.userTag.UserTagList;
import com.hippo.ehviewer.client.parser.GalleryListParser;
import com.hippo.lib.yorozuya.SimpleHandler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;

public final class TrackingManager {

    public static final int SOURCE_LOCAL = 0;
    public static final int SOURCE_CLOUD = 1;

    private TrackingManager() {
    }

    public static void addLocalTag(@NonNull String name) {
        String normalized = name.trim();
        if (normalized.isEmpty()) {
            return;
        }
        SQLiteDatabase db = EhDB.getRawDatabase();
        db.execSQL("INSERT OR IGNORE INTO TRACKED_TAG " +
                        "(NAME, SOURCE, ENABLED, BASELINE_GID, LAST_SCAN) VALUES (?, ?, 1, 0, 0)",
                new Object[]{normalized, SOURCE_LOCAL});
    }

    public static void syncCloudTags(@NonNull Context context) {
        UserTagList list = EhApplication.getUserTagList(context);
        if (list == null || list.userTags == null) {
            return;
        }
        SQLiteDatabase db = EhDB.getRawDatabase();
        for (UserTag tag : list.userTags) {
            if (tag.watched && !TextUtils.isEmpty(tag.tagName)) {
                db.execSQL("INSERT OR IGNORE INTO TRACKED_TAG " +
                                "(NAME, SOURCE, ENABLED, BASELINE_GID, LAST_SCAN) VALUES (?, ?, 1, 0, 0)",
                        new Object[]{tag.tagName, SOURCE_CLOUD});
            }
        }
    }

    public static List<TrackedTag> getTags() {
        return getTags(false);
    }

    public static List<TrackedTag> getAllTags() {
        return getTags(true);
    }

    @Nullable
    public static TrackedTag findTag(@NonNull String name) {
        String normalized = name.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        SQLiteDatabase db = EhDB.getRawDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT NAME, SOURCE, ENABLED, BASELINE_GID, LAST_SCAN FROM TRACKED_TAG " +
                        "WHERE NAME = ? COLLATE NOCASE LIMIT 1",
                new String[]{normalized})) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new TrackedTag(
                    cursor.getString(0), cursor.getInt(1), cursor.getInt(2) != 0,
                    cursor.getLong(3), cursor.getLong(4));
        }
    }

    private static List<TrackedTag> getTags(boolean includeDisabled) {
        List<TrackedTag> result = new ArrayList<>();
        SQLiteDatabase db = EhDB.getRawDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT NAME, SOURCE, ENABLED, BASELINE_GID, LAST_SCAN FROM TRACKED_TAG " +
                        (includeDisabled ? "" : "WHERE ENABLED = 1 ") +
                        "ORDER BY SOURCE, NAME", null)) {
            while (cursor.moveToNext()) {
                result.add(new TrackedTag(
                        cursor.getString(0), cursor.getInt(1), cursor.getInt(2) != 0,
                        cursor.getLong(3), cursor.getLong(4)));
            }
        }
        return result;
    }

    public static void setTagEnabled(@NonNull String name, boolean enabled) {
        EhDB.getRawDatabase().execSQL(
                "UPDATE TRACKED_TAG SET ENABLED = ? WHERE NAME = ?",
                new Object[]{enabled ? 1 : 0, name});
    }

    public static void resetTagBaseline(@NonNull String name) {
        EhDB.getRawDatabase().execSQL(
                "UPDATE TRACKED_TAG SET BASELINE_GID = 0, LAST_SCAN = 0 WHERE NAME = ?",
                new Object[]{name});
    }

    public static void deleteTag(@NonNull String name) {
        EhDB.getRawDatabase().execSQL(
                "DELETE FROM TRACKED_TAG WHERE NAME = ?", new Object[]{name});
    }

    public static List<TrackedGallery> getUnread() {
        List<TrackedGallery> result = new ArrayList<>();
        SQLiteDatabase db = EhDB.getRawDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT DATA, MATCHED_TAGS FROM TRACKED_GALLERY WHERE IS_READ = 0 " +
                        "ORDER BY DISCOVERED DESC", null)) {
            while (cursor.moveToNext()) {
                GalleryInfo info = GalleryInfo.galleryInfoFromJson(
                        com.alibaba.fastjson.JSONObject.parseObject(cursor.getString(0)));
                result.add(new TrackedGallery(info, cursor.getString(1)));
            }
        }
        return result;
    }

    public static void markRead(long gid) {
        EhDB.getRawDatabase().execSQL(
                "UPDATE TRACKED_GALLERY SET IS_READ = 1 WHERE GID = ?", new Object[]{gid});
    }

    public static void markAllRead() {
        EhDB.getRawDatabase().execSQL("UPDATE TRACKED_GALLERY SET IS_READ = 1");
    }

    public static void check(@NonNull Context context, boolean force, @Nullable Callback callback) {
        if (!Settings.getTrackingEnabled()) {
            return;
        }
        long interval = Settings.getTrackingIntervalHours() * 60L * 60L * 1000L;
        if (!force && System.currentTimeMillis() - Settings.getLastTrackingCheck() < interval) {
            return;
        }
        Context appContext = context.getApplicationContext();
        EhApplication.getExecutorService(appContext).execute(() -> {
            try {
                syncCloudTags(appContext);
                int added = checkInternal(appContext);
                Settings.putLastTrackingCheck(System.currentTimeMillis());
                notifySuccess(callback, added);
            } catch (Throwable e) {
                notifyFailure(callback, e);
            }
        });
    }

    private static int checkInternal(Context context) throws Throwable {
        OkHttpClient client = EhApplication.getOkHttpClient(context);
        int globalRemaining = Math.min(Settings.getTrackingGlobalLimit(), 500);
        int added = 0;
        for (TrackedTag tag : getTags()) {
            if (globalRemaining <= 0) {
                break;
            }
            ListUrlBuilder builder = new ListUrlBuilder();
            builder.set(tag.name);
            GalleryListParser.Result page = EhEngine.getGalleryList(
                    null, client, builder.build(), ListUrlBuilder.MODE_TAG);
            int perTagRemaining = Math.min(Settings.getTrackingPerTagLimit(), 100);
            long newestGid = tag.baselineGid;
            List<GalleryInfo> candidates = page.galleryInfoList;
            for (GalleryInfo info : candidates) {
                newestGid = Math.max(newestGid, info.gid);
                if (tag.baselineGid == 0 || info.gid <= tag.baselineGid
                        || perTagRemaining <= 0 || globalRemaining <= 0) {
                    continue;
                }
                if (upsertUnread(info, tag.name)) {
                    added++;
                    globalRemaining--;
                }
                perTagRemaining--;
            }
            EhDB.getRawDatabase().execSQL(
                    "UPDATE TRACKED_TAG SET BASELINE_GID = ?, LAST_SCAN = ? WHERE NAME = ?",
                    new Object[]{newestGid, System.currentTimeMillis(), tag.name});
        }
        return added;
    }

    private static boolean upsertUnread(GalleryInfo info, String tag) {
        SQLiteDatabase db = EhDB.getRawDatabase();
        String matched = null;
        int read = 0;
        boolean exists;
        try (Cursor cursor = db.rawQuery(
                "SELECT MATCHED_TAGS, IS_READ FROM TRACKED_GALLERY WHERE GID = ?",
                new String[]{Long.toString(info.gid)})) {
            exists = cursor.moveToFirst();
            if (exists) {
                matched = cursor.getString(0);
                read = cursor.getInt(1);
            }
        }
        Set<String> tags = new LinkedHashSet<>();
        if (!TextUtils.isEmpty(matched)) {
            for (String item : matched.split("\\n")) {
                tags.add(item);
            }
        }
        tags.add(tag);
        String joined = TextUtils.join("\n", tags);
        db.execSQL("INSERT OR REPLACE INTO TRACKED_GALLERY " +
                        "(GID, DATA, MATCHED_TAGS, DISCOVERED, IS_READ) VALUES (?, ?, ?, ?, ?)",
                new Object[]{info.gid, info.toJson().toJSONString(), joined,
                        System.currentTimeMillis(), read});
        return !exists;
    }

    private static void notifySuccess(@Nullable Callback callback, int added) {
        if (callback != null) {
            SimpleHandler.getInstance().post(() -> callback.onSuccess(added));
        }
    }

    private static void notifyFailure(@Nullable Callback callback, Throwable error) {
        if (callback != null) {
            SimpleHandler.getInstance().post(() -> callback.onFailure(error));
        }
    }

    public static final class TrackedTag {
        public final String name;
        public final int source;
        public final boolean enabled;
        public final long baselineGid;
        public final long lastScan;

        TrackedTag(String name, int source, boolean enabled, long baselineGid, long lastScan) {
            this.name = name;
            this.source = source;
            this.enabled = enabled;
            this.baselineGid = baselineGid;
            this.lastScan = lastScan;
        }
    }

    public static final class TrackedGallery {
        public final GalleryInfo info;
        public final String matchedTags;

        TrackedGallery(GalleryInfo info, String matchedTags) {
            this.info = info;
            this.matchedTags = matchedTags;
        }
    }

    public interface Callback {
        void onSuccess(int added);

        void onFailure(Throwable error);
    }
}
