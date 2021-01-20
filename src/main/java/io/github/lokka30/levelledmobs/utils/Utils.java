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

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
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
