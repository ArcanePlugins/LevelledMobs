package io.github.lokka30.levelledmobs.utils;

import io.github.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.microlib.MicroLogger;
import me.lokka30.microlib.MicroUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;

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
    public static double round(final double value) {
        return Math.round(value * 100) / 100.00;
    }

    public static String replaceEx(final String original, final String pattern, final String replacement) {
        if (original == null || pattern == null) return null;

        int count, position0, position1;
        count = position0 = 0;
        String upperString = original.toUpperCase();
        String upperPattern = pattern.toUpperCase();
        int inc = (original.length() / pattern.length()) *
                (replacement.length() - pattern.length());
        char[] chars = new char[original.length() + Math.max(0, inc)];
        while ((position1 = upperString.indexOf(upperPattern, position0)) != -1) {
            for (int i = position0; i < position1; ++i)
                chars[count++] = original.charAt(i);
            for (int i = 0; i < replacement.length(); ++i)
                chars[count++] = replacement.charAt(i);
            position0 = position1 + pattern.length();
        }
        if (position0 == 0) return original;
        for (int i = position0; i < original.length(); ++i)
            chars[count++] = original.charAt(i);

        return new String(chars, 0, count);
    }

    //Integer check
    public static boolean isInteger(final String str) {
        if (Utils.isNullOrEmpty(str)) return false;

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean isDouble(final String str) {
        if (Utils.isNullOrEmpty(str)) return false;

        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean isNullOrEmpty(final String str) {
        return (str == null || str.isEmpty());
    }

    public static int getDefaultIfNull(final YamlConfiguration cfg, final  String path, final int def) {
        return cfg.contains(path) ? cfg.getInt(path) : def;
    }

    public static int getDefaultIfNull(final TreeMap<String, Integer> map, final String item, final int def) {
        return map.getOrDefault(item, def);
    }

    public static final List<String> oneToNine = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9");

    public static List<String> replaceAllInList(final List<String> oldList, final String replaceWhat, final String replaceTo) {
        final List<String> newList = new ArrayList<>();

        for (final String string : oldList) {
            newList.add(string.replace(replaceWhat, replaceTo));
        }

        return newList;
    }

    public static List<String> colorizeAllInList(final List<String> oldList) {
        final List<String> newList = new ArrayList<>(oldList.size());

        for (final String string : oldList) {
            newList.add(MicroUtils.colorize(string));
        }

        return newList;
    }

    public static boolean isZombieBaby(final Zombie zombie) {
        try {
            zombie.isAdult();
            return !zombie.isAdult();
        } catch (NoSuchMethodError err) {
            //noinspection deprecation
            return zombie.isBaby();
        }
    }

    /**
     * Sends a debug message to console if enabled in settings
     *
     * @param instance LevelledMobs class
     * @param location Reference to whereabouts the debug log is called so that it can be traced back easily
     * @param msg      Message to help de-bugging
     */
    public static void debugLog(final LevelledMobs instance, final String location, final String msg) {
        if (instance.settingsCfg.getStringList("debug-misc").contains(location)) {
            logger.info("&f&l[DEBUG]&7 &8[" + location + "&8]&7 " + msg);
        }
    }
}
