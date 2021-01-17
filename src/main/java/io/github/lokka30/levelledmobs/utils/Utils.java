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

    public static int getDefaultIfNull(TreeMap<String, Integer> map, String item, int def) {
        return map.containsKey(item) ? map.get(item) : def;
    }

    public static List<String> oneToNine = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9");

    public static List<String> mobs = Arrays.asList(
            "bat",
            "bee",
            "blaze",
            "cat",
            "cave_spider",
            "chicken",
            "cod",
            "cow",
            "creeper",
            "dolphin",
            "donkey",
            "drowned",
            "elder_guardian",
            "ender_dragon",
            "enderman",
            "endermite",
            "evoker",
            "evoker_fangs",
            "fox",
            "ghast",
            "giant",
            "guardian",
            "hoglin",
            "horse",
            "husk",
            "illusioner",
            "iron_golem",
            "llama",
            "magma_cube",
            "mule",
            "mushroom_cow",
            "ocelot",
            "panda",
            "parrot",
            "phantom",
            "pig",
            "piglin",
            "piglin_brute",
            "pillager",
            "polar_bear",
            "pufferfish",
            "rabbit",
            "ravager",
            "salmon",
            "sheep",
            "shulker",
            "silverfish",
            "skeleton",
            "skeleton_horse",
            "slime",
            "snowball",
            "snowman",
            "spider",
            "squid",
            "stray",
            "strider",
            "tropical_fish",
            "turtle",
            "vex",
            "villager",
            "vindicator",
            "witch",
            "wither",
            "wither_skeleton",
            "wolf",
            "zoglin",
            "zombie",
            "zombie_horse",
            "zombie_villager",
            "zombified_piglin"
    );

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
