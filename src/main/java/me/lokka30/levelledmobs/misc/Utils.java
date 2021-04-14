package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.microlib.MessageUtils;
import me.lokka30.microlib.MicroLogger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

/**
 * @author lokka30
 * @contributors stumper66, Hugo5551
 */
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
    @NotNull
    public static List<String> getSupportedServerVersions() {
        return Arrays.asList("1.14", "1.15", "1.16");
    }

    @NotNull
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
    public static String replaceEx(@NotNull final String message, @NotNull final String replaceWhat, @NotNull final String replaceTo) {
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

    /**
     * Check if str is an integer
     *
     * @param str str to check
     * @return if str is an integer (e.g. "1234" = true, "hello" = false)
     */
    public static boolean isInteger(@Nullable final String str) {
        if (isNullOrEmpty(str)) return false;

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean isDouble(@Nullable final String str) {
        if (isNullOrEmpty(str)) return false;

        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static boolean isNullOrEmpty(@Nullable final String str) {
        return (str == null || str.isEmpty());
    }

    public static int getDefaultIfNull(@NotNull final YamlConfiguration cfg, @NotNull final String path, final int def) {
        return cfg.contains(path) ? cfg.getInt(path) : def;
    }

    public static int getDefaultIfNull(@NotNull final TreeMap<String, Integer> map, @NotNull final String item, final int def) {
        return map.getOrDefault(item, def);
    }

    @NotNull
    public static final List<String> oneToNine = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9");

    @NotNull
    public static List<String> replaceAllInList(@NotNull final List<String> oldList, @NotNull final String replaceWhat, @NotNull final String replaceTo) {
        final List<String> newList = new ArrayList<>();
        for (final String string : oldList) {
            newList.add(string.replace(replaceWhat, replaceTo));
        }
        return newList;
    }

    @NotNull
    public static List<String> colorizeAllInList(@NotNull final List<String> oldList) {
        final List<String> newList = new ArrayList<>(oldList.size());

        for (final String string : oldList) {
            newList.add(MessageUtils.colorizeAll(string));
        }

        return newList;
    }

    public static boolean isBabyMob(@NotNull final LivingEntity livingEntity) {

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
        } else if (livingEntity instanceof Ageable){
            return !(((Ageable) livingEntity).isAdult());
        }

        return false;
    }

    /**
     * Sends a debug message to console if enabled in settings
     *
     * @param instance  LevelledMobs class
     * @param debugType Reference to whereabouts the debug log is called so that it can be traced back easily
     * @param msg       Message to help de-bugging
     */
    public static void debugLog(@NotNull final LevelledMobs instance, @NotNull final DebugType debugType, @NotNull final String msg) {
        if (instance.settingsCfg.getStringList("debug-misc").contains(debugType.toString())) {
            logger.info("&8[&bDebug: " + debugType + "&8]&7 " + msg);
        }
    }

    /**
     * If object1 is null, return object2
     *
     * @param object1 a nullable object
     * @param object2 a non-nullable object
     * @return object2 if object1 is null, otherwise, object1
     */
    public static Object getNonNull(@Nullable Object object1, @NotNull Object object2) {
        return object1 == null ? object2 : object1;
    }
}
