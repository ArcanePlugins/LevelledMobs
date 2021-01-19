package io.github.lokka30.levelledmobs.utils;

import me.lokka30.microlib.MicroLogger;
import me.lokka30.microlib.MicroUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

public final class Utils {

    private Utils() {
        throw new UnsupportedOperationException();
    }

    public static List<String> getSupportedServerVersions() {
        return Arrays.asList("1.15", "1.16");
    }

    public static final MicroLogger logger = new MicroLogger("&b&lLevelledMobs: &7");

    /**
     * Rounds value to 2dp
     *
     * @param value value to round
     * @return rounded value
     */
    public static double round(double value) {
        return Math.round(value * 100) / 100.00;
    }

    //Integer check
    public static boolean isInteger(String str) {
        if (!Utils.isNotNullOrEmpty(str)) return false;

        // taken from https://www.edureka.co/community/2301/what-the-best-way-check-string-represents-an-integer-in-java

        int length = str.length();
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Case insensitive alternative to String#replace
     *
     * @param originalString the original string which may contain the item to replace
     * @param replaceWhat    the item that should be replaced
     * @param replaceWith    what to replace the item with
     * @return the original string with replaced content
     */
    public static String replaceEx(String originalString, String replaceWhat, String replaceWith) {
        if (originalString == null || replaceWhat == null) return null;

        int count, position0, position1;
        count = position0 = 0;
        String upperString = originalString.toUpperCase();
        String upperPattern = replaceWhat.toUpperCase();
        int inc = (originalString.length() / replaceWhat.length()) *
                (replaceWith.length() - replaceWhat.length());
        char[] chars = new char[originalString.length() + Math.max(0, inc)];
        while ((position1 = upperString.indexOf(upperPattern, position0)) != -1) {
            for (int i = position0; i < position1; ++i)
                chars[count++] = originalString.charAt(i);
            for (int i = 0; i < replaceWith.length(); ++i)
                chars[count++] = replaceWith.charAt(i);
            position0 = position1 + replaceWhat.length();
        }
        if (position0 == 0) return originalString;
        for (int i = position0; i < originalString.length(); ++i)
            chars[count++] = originalString.charAt(i);

        return new String(chars, 0, count);
    }

    public static boolean isNotNullOrEmpty(String str) {
        return (str != null && !str.isEmpty());
    }

    public static int getDefaultIfNull(YamlConfiguration cfg, String path, int def) {
        return cfg.contains(path) ? cfg.getInt(path) : def;
    }

    public static int getDefaultIfNull(TreeMap<String, Integer> map, String item, int def) {
        return map.getOrDefault(item, def);
    }

    public static final List<String> oneToNine = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9");

    public static List<String> replaceAllInList(List<String> oldList, String replaceWhat, String replaceTo) {
        List<String> newList = new ArrayList<>();

        for (String string : oldList) {
            newList.add(string.replace(replaceWhat, replaceTo));
        }

        return newList;
    }

    public static List<String> colorizeAllInList(List<String> oldList) {
        List<String> newList = new ArrayList<>();

        for (String string : oldList) {
            newList.add(MicroUtils.colorize(string));
        }

        return newList;
    }
}
