package io.github.lokka30.levelledmobs.utils;

import me.lokka30.microlib.MicroLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Arrays;
import java.util.List;

public final class Utils {

    private Utils() {
        throw new UnsupportedOperationException();
    }

    public static List<String> getSupportedServerVersions() {
        return Arrays.asList("1.15", "1.16");
    }

    public static final MicroLogger logger = new MicroLogger("&b&lLevelledMobs: &7");

    //TODO Replace usages with other round method.
    //This is a method created by Jonik & Mustapha Hadid at StackOverflow.
    //It simply grabs 'value', being a double, and rounds it, leaving 'places' decimal places intact.
    //Created by Jonik & Mustapha Hadid @ stackoverflow
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

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
        if (Utils.isNotNullOrEmpty(str)) {
            for (int i = 0; i < str.length(); i++) {
                if (i == 0 && str.charAt(i) == '-') {
                    if (str.length() == 1) {
                        return false;
                    } else {
                        continue;
                    }
                }

                if (Character.digit(str.charAt(i), 10) < 0) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
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
}
