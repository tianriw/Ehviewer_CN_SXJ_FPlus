package com.hippo.ehviewer;

import androidx.annotation.NonNull;

import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.GalleryTags;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

/**
 * Client-side namespace-based gallery filtering.
 * Complements the server-side xns uconfig parameter (which only works for tag searches).
 * Call {@link #filter(List)} before passing any gallery list to the UI.
 */
public final class NamespaceFilter {

    public static final int BIT_RECLASS = 0x1;
    public static final int BIT_LANGUAGE = 0x2;
    public static final int BIT_PARODY = 0x4;
    public static final int BIT_CHARACTER = 0x8;
    public static final int BIT_GROUP = 0x10;
    public static final int BIT_ARTIST = 0x20;
    public static final int BIT_MALE = 0x40;
    public static final int BIT_FEMALE = 0x80;
    public static final int BIT_COSPLAYER = 0x100;
    public static final int BIT_MIXED = 0x200;
    public static final int BIT_OTHER = 0x400;
    public static final int BIT_MISC = 0x800;
    public static final int BIT_ROWS = 0x1000;

    private static final int SERVER_NAMESPACE_MASK =
            BIT_RECLASS | BIT_LANGUAGE | BIT_PARODY | BIT_CHARACTER |
                    BIT_GROUP | BIT_ARTIST | BIT_MALE | BIT_FEMALE;

    private NamespaceFilter() {}

    public static boolean hasExcludedNamespaces() {
        return Settings.getExcludedTagNamespaces() != 0;
    }

    public static boolean needTags() {
        return hasExcludedNamespaces() || Settings.getMinTagCount() > 0;
    }

    public static int toServerMask(int excludedMask) {
        return excludedMask & SERVER_NAMESPACE_MASK;
    }

    public static boolean isKnownNamespace(String ns) {
        return namespaceToBit(ns) != 0;
    }

    public static boolean isNamespaceExcluded(String ns) {
        int bit = namespaceToBit(ns);
        return bit != 0 && (Settings.getExcludedTagNamespaces() & bit) != 0;
    }

    public static boolean toggleNamespaceExclusion(String ns) {
        int bit = namespaceToBit(ns);
        if (bit == 0) {
            return false;
        }
        int current = Settings.getExcludedTagNamespaces();
        boolean nowExcluded = (current & bit) == 0;
        Settings.putExcludedTagNamespaces(nowExcluded ? (current | bit) : (current & ~bit));
        return nowExcluded;
    }

    @NonNull
    public static List<GalleryInfo> filter(@NonNull List<GalleryInfo> list) {
        int excluded = Settings.getExcludedTagNamespaces();
        if (!needTags() || list.isEmpty()) {
            return list;
        }
        List<GalleryInfo> result = new ArrayList<>(list.size());
        for (GalleryInfo gi : list) {
            if (!isExcluded(gi, excluded)) {
                result.add(gi);
            }
        }
        return result;
    }

    public static void filterInPlace(@NonNull List<GalleryInfo> list) {
        int excluded = Settings.getExcludedTagNamespaces();
        if (!needTags() || list.isEmpty()) {
            return;
        }
        for (int i = 0, n = list.size(); i < n; i++) {
            if (isExcluded(list.get(i), excluded)) {
                list.remove(i);
                i--;
                n--;
            }
        }
    }

    public static boolean isExcluded(GalleryInfo gi) {
        return isExcluded(gi, Settings.getExcludedTagNamespaces());
    }

    static boolean isExcluded(GalleryInfo gi, int excludedMask) {
        if (gi == null) {
            return false;
        }
        GalleryTags tags = EhDB.queryGalleryTags(gi.gid);
        if (excludedMask != 0) {
            if (hasExcludedTag(gi.simpleTags, excludedMask)) {
                return true;
            }
            if (gi.tgList != null && hasExcludedTag(gi.tgList, excludedMask)) {
                return true;
            }
            if (tags != null && hasExcludedGalleryTags(tags, excludedMask)) {
                return true;
            }
        }
        int minTagCount = Settings.getMinTagCount();
        return minTagCount > 0 && countTags(gi, tags) < minTagCount;
    }

    private static boolean hasExcludedTag(String[] tags, int excludedMask) {
        if (tags == null) {
            return false;
        }
        for (String tag : tags) {
            if (hasExcludedTag(tag, excludedMask)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExcludedTag(Iterable<String> tags, int excludedMask) {
        for (String tag : tags) {
            if (hasExcludedTag(tag, excludedMask)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExcludedTag(String tag, int excludedMask) {
        if (tag != null) {
            int colon = tag.indexOf(':');
            if (colon > 0) {
                int bit = namespaceToBit(tag.substring(0, colon));
                if ((excludedMask & bit) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasExcludedGalleryTags(GalleryTags tags, int excludedMask) {
        return hasNamespaceValue(excludedMask, BIT_RECLASS, tags.reclass) ||
                hasNamespaceValue(excludedMask, BIT_LANGUAGE, tags.language) ||
                hasNamespaceValue(excludedMask, BIT_PARODY, tags.parody) ||
                hasNamespaceValue(excludedMask, BIT_CHARACTER, tags.character) ||
                hasNamespaceValue(excludedMask, BIT_GROUP, tags.group) ||
                hasNamespaceValue(excludedMask, BIT_ARTIST, tags.artist) ||
                hasNamespaceValue(excludedMask, BIT_MALE, tags.male) ||
                hasNamespaceValue(excludedMask, BIT_FEMALE, tags.female) ||
                hasNamespaceValue(excludedMask, BIT_COSPLAYER, tags.cosplayer) ||
                hasNamespaceValue(excludedMask, BIT_MIXED, tags.mixed) ||
                hasNamespaceValue(excludedMask, BIT_OTHER, tags.other) ||
                hasNamespaceValue(excludedMask, BIT_MISC, tags.misc) ||
                hasNamespaceValue(excludedMask, BIT_ROWS, tags.rows);
    }

    private static boolean hasNamespaceValue(int excludedMask, int bit, String value) {
        return (excludedMask & bit) != 0 && value != null && !value.trim().isEmpty();
    }

    private static int countTags(GalleryInfo gi, GalleryTags tags) {
        if (gi.simpleTags != null && gi.simpleTags.length > 0) {
            return countNonEmpty(gi.simpleTags);
        }
        if (gi.tgList != null && !gi.tgList.isEmpty()) {
            return countNonEmpty(gi.tgList);
        }
        return tags != null ? countGalleryTags(tags) : 0;
    }

    private static int countGalleryTags(GalleryTags tags) {
        return countCsv(tags.reclass) +
                countCsv(tags.language) +
                countCsv(tags.parody) +
                countCsv(tags.character) +
                countCsv(tags.group) +
                countCsv(tags.artist) +
                countCsv(tags.male) +
                countCsv(tags.female) +
                countCsv(tags.cosplayer) +
                countCsv(tags.mixed) +
                countCsv(tags.other) +
                countCsv(tags.misc) +
                countCsv(tags.rows);
    }

    private static int countNonEmpty(String[] values) {
        int count = 0;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countNonEmpty(Iterable<String> values) {
        int count = 0;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static int countCsv(String values) {
        if (values == null || values.trim().isEmpty()) {
            return 0;
        }
        int count = 0;
        String cleaned = values.replace("[", "").replace("]", "").replace("\"", "");
        for (String value : cleaned.split(",")) {
            if (!value.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public static int namespaceToBit(String ns) {
        if (ns == null) {
            return 0;
        }
        String normalized = ns.trim().toLowerCase(Locale.US);
        int colon = normalized.indexOf(':');
        if (colon >= 0) {
            normalized = normalized.substring(0, colon);
        }
        switch (normalized) {
            case "r":
            case "reclass":   return BIT_RECLASS;
            case "l":
            case "language":  return BIT_LANGUAGE;
            case "p":
            case "parody":    return BIT_PARODY;
            case "c":
            case "character": return BIT_CHARACTER;
            case "g":
            case "group":     return BIT_GROUP;
            case "a":
            case "artist":    return BIT_ARTIST;
            case "m":
            case "male":      return BIT_MALE;
            case "f":
            case "female":    return BIT_FEMALE;
            case "cos":
            case "cosplayer": return BIT_COSPLAYER;
            case "x":
            case "mixed":     return BIT_MIXED;
            case "o":
            case "other":     return BIT_OTHER;
            case "misc":      return BIT_MISC;
            case "n":
            case "rows":      return BIT_ROWS;
            default:          return 0;
        }
    }
}
