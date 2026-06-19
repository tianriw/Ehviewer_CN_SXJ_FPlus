package com.hippo.ehviewer.stats;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSONObject;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.GalleryTags;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class StatisticsManager {

    private StatisticsManager() {
    }

    public static void recordView(@NonNull GalleryInfo info) {
        try {
            String tags = TextUtils.join("\n", getTags(info));
            EhDB.getRawDatabase().execSQL(
                    "INSERT INTO READING_EVENT (GID, TIME, DATA, TAGS) VALUES (?, ?, ?, ?)",
                    new Object[]{info.gid, System.currentTimeMillis(),
                            info.toJson().toJSONString(), tags});
        } catch (Exception e) {
            android.util.Log.e("StatisticsManager", "recordView failed", e);
        }
    }

    public static void clear() {
        EhDB.getRawDatabase().execSQL("DELETE FROM READING_EVENT");
    }

    public static Snapshot getSnapshot(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        long start = calendar.getTimeInMillis();
        calendar.add(Calendar.YEAR, 1);
        long end = calendar.getTimeInMillis();

        int totalViews = 0;
        long latestTime = 0L;
        Set<Long> unique = new HashSet<>();
        Map<Long, Integer> galleryCounts = new HashMap<>();
        Map<Long, String> galleryData = new HashMap<>();
        Map<String, Integer> tagCounts = new HashMap<>();
        Map<String, Set<Long>> tagUniqueGalleries = new HashMap<>();
        Map<Integer, Integer> dailyCounts = new HashMap<>();

        SQLiteDatabase db = EhDB.getRawDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT GID, TIME, DATA, TAGS FROM READING_EVENT " +
                        "WHERE TIME >= ? AND TIME < ? ORDER BY TIME ASC",
                new String[]{Long.toString(start), Long.toString(end)})) {
            Calendar eventCalendar = Calendar.getInstance();
            while (cursor.moveToNext()) {
                long gid = cursor.getLong(0);
                long time = cursor.getLong(1);
                String data = cursor.getString(2);
                String tags = cursor.getString(3);
                totalViews++;
                latestTime = Math.max(latestTime, time);
                unique.add(gid);
                galleryCounts.put(gid, increment(galleryCounts.get(gid)));
                galleryData.put(gid, data);
                eventCalendar.setTimeInMillis(time);
                int day = eventCalendar.get(Calendar.DAY_OF_YEAR);
                dailyCounts.put(day, increment(dailyCounts.get(day)));
                if (!TextUtils.isEmpty(tags)) {
                    for (String tag : tags.split("\\n")) {
                        if (tag.isEmpty()) {
                            continue;
                        }
                        tagCounts.put(tag, increment(tagCounts.get(tag)));
                        Set<Long> tagGalleries = tagUniqueGalleries.get(tag);
                        if (tagGalleries == null) {
                            tagGalleries = new HashSet<>();
                            tagUniqueGalleries.put(tag, tagGalleries);
                        }
                        tagGalleries.add(gid);
                    }
                }
            }
        }

        long favoriteGid = -1L;
        int favoriteCount = 0;
        for (Map.Entry<Long, Integer> entry : galleryCounts.entrySet()) {
            if (entry.getValue() > favoriteCount) {
                favoriteGid = entry.getKey();
                favoriteCount = entry.getValue();
            }
        }
        GalleryInfo favorite = null;
        if (favoriteGid >= 0 && galleryData.containsKey(favoriteGid)) {
            favorite = GalleryInfo.galleryInfoFromJson(
                    JSONObject.parseObject(galleryData.get(favoriteGid)));
        }

        List<TagStat> topTags = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tagCounts.entrySet()) {
            topTags.add(new TagStat(entry.getKey(), entry.getValue(),
                    tagUniqueGalleries.get(entry.getKey()).size()));
        }
        Collections.sort(topTags, (left, right) -> Integer.compare(right.views, left.views));
        if (topTags.size() > 10) {
            topTags = new ArrayList<>(topTags.subList(0, 10));
        }
        return new Snapshot(year, totalViews, unique.size(), latestTime,
                favorite, favoriteCount, topTags, dailyCounts);
    }

    private static int increment(@Nullable Integer value) {
        return value != null ? value + 1 : 1;
    }

    private static Set<String> getTags(GalleryInfo info) {
        Set<String> result = new LinkedHashSet<>();
        GalleryTags full = EhDB.queryGalleryTags(info.gid);
        if (full != null) {
            addNamespaced(result, "artist", full.artist);
            addNamespaced(result, "cosplayer", full.cosplayer);
            addNamespaced(result, "character", full.character);
            addNamespaced(result, "female", full.female);
            addNamespaced(result, "group", full.group);
            addNamespaced(result, "language", full.language);
            addNamespaced(result, "male", full.male);
            addNamespaced(result, "misc", full.misc);
            addNamespaced(result, "mixed", full.mixed);
            addNamespaced(result, "other", full.other);
            addNamespaced(result, "parody", full.parody);
            addNamespaced(result, "reclass", full.reclass);
        }
        if (result.isEmpty() && info.simpleTags != null) {
            Collections.addAll(result, info.simpleTags);
        }
        return result;
    }

    private static void addNamespaced(Set<String> result, String namespace, @Nullable String raw) {
        if (TextUtils.isEmpty(raw)) {
            return;
        }
        String cleaned = raw.replace("[", "").replace("]", "").replace("\"", "");
        for (String value : cleaned.split(",")) {
            String tag = value.trim();
            if (!tag.isEmpty()) {
                result.add(namespace + ":" + tag);
            }
        }
    }

    public static final class Snapshot {
        public final int year;
        public final int totalViews;
        public final int uniqueGalleries;
        public final long latestTime;
        @Nullable
        public final GalleryInfo favoriteGallery;
        public final int favoriteGalleryViews;
        public final List<TagStat> topTags;
        public final Map<Integer, Integer> dailyCounts;

        Snapshot(int year, int totalViews, int uniqueGalleries, long latestTime,
                 @Nullable GalleryInfo favoriteGallery, int favoriteGalleryViews,
                 List<TagStat> topTags, Map<Integer, Integer> dailyCounts) {
            this.year = year;
            this.totalViews = totalViews;
            this.uniqueGalleries = uniqueGalleries;
            this.latestTime = latestTime;
            this.favoriteGallery = favoriteGallery;
            this.favoriteGalleryViews = favoriteGalleryViews;
            this.topTags = topTags;
            this.dailyCounts = dailyCounts;
        }
    }

    public static final class TagStat {
        public final String tag;
        public final int views;
        public final int uniqueGalleries;

        TagStat(String tag, int views, int uniqueGalleries) {
            this.tag = tag;
            this.views = views;
            this.uniqueGalleries = uniqueGalleries;
        }
    }
}
