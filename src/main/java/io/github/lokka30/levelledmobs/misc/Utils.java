package io.github.lokka30.levelledmobs.misc;

import io.github.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.microlib.MessageUtils;
import me.lokka30.microlib.MicroLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

public final class Utils {

    /**
     * Use static methods, e.g. Utils.round, not new Utils().round for example.
     */
    private Utils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get a list of major Minecraft versions supported by LM.
     *
     * @return list
     */
    public static List<String> getSupportedServerVersions() {
        return Arrays.asList("1.14", "1.15", "1.16");
    }

    public static final MicroLogger logger = new MicroLogger("&b&lLevelledMobs: &7");

    /**
     * Rounds value to 2 decimal points.
     *
     * @param value value to round
     * @return rounded value
     */
    public static double round(final double value) {
        return Math.round(value * 100) / 100.00;
    }

    /**
     * Replaces content of a message with case insensitivity.
     *
     * @param message     message that should be edited
     * @param replaceWhat the text to be replaced
     * @param replaceTo   the text to replace with
     * @return modified message
     * @author stumper66
     */
    public static String replaceEx(final String message, final String replaceWhat, final String replaceTo) {
        if (message == null || replaceWhat == null) return null;

        int count, position0, position1;
        count = position0 = 0;
        String upperString = message.toUpperCase();
        String upperPattern = replaceWhat.toUpperCase();
        int inc = (message.length() / replaceWhat.length()) *
                (replaceTo.length() - replaceWhat.length());
        char[] chars = new char[message.length() + Math.max(0, inc)];
        while ((position1 = upperString.indexOf(upperPattern, position0)) != -1) {
            for (int i = position0; i < position1; ++i)
                chars[count++] = message.charAt(i);
            for (int i = 0; i < replaceTo.length(); ++i)
                chars[count++] = replaceTo.charAt(i);
            position0 = position1 + replaceWhat.length();
        }
        if (position0 == 0) return message;
        for (int i = position0; i < message.length(); ++i)
            chars[count++] = message.charAt(i);

        return new String(chars, 0, count);
    }

    //Integer check
    public static boolean isInteger(final String str) {
        if (isNullOrEmpty(str)) return false;

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean isDouble(final String str) {
        if (isNullOrEmpty(str)) return false;

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
            newList.add(MessageUtils.colorizeAll(string));
        }

        return newList;
    }

    public static boolean isBabyZombie(final LivingEntity livingEntity) {

        if (livingEntity instanceof Zombie) {
            // for backwards compatibility
            Zombie zombie = (Zombie) livingEntity;
            try {
                zombie.isAdult();
                return !zombie.isAdult();
            } catch (NoSuchMethodError err) {
                //noinspection deprecation
                return zombie.isBaby();
            }
        }
        else if (livingEntity instanceof Ageable){
            return !(((Ageable) livingEntity).isAdult());
        }

        return false;
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
