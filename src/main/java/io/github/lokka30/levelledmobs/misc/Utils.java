package io.github.lokka30.levelledmobs.misc;

import io.github.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.microlib.MicroLogger;
import me.lokka30.microlib.MicroUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
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
        return Arrays.asList("1.14", "1.15", "1.16");
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

    public static boolean isEntityBaby(final LivingEntity livingEntity) {

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

    public static Enchantment getEnchantmentFromName(final LevelledMobs main, final String enchantmentName) {

        switch (enchantmentName.replace(" ", "_").toLowerCase()) {
            case "arrow_damage":
            case "power":
                return Enchantment.ARROW_DAMAGE;
            case "arrow_fire":
                return Enchantment.ARROW_FIRE;
            case "arrow_infinity":
            case "infinity":
                return Enchantment.ARROW_INFINITE;
            case "binding":
            case "binding_curse":
                return Enchantment.BINDING_CURSE;
            case "arrow_knockback":
            case "punch":
                return Enchantment.ARROW_KNOCKBACK;
            case "channeling":
                return Enchantment.CHANNELING;
            case "damage_all":
            case "sharpness":
                return Enchantment.DAMAGE_ALL;
            case "damage_arthropods":
            case "bane_of_arthropods":
                return Enchantment.DAMAGE_ARTHROPODS;
            case "damage_undead":
            case "smite":
                return Enchantment.DAMAGE_UNDEAD;
            case "depth_strider":
                return Enchantment.DEPTH_STRIDER;
            case "dig_speed":
            case "efficiency":
                return Enchantment.DIG_SPEED;
            case "durability":
            case "unbreaking":
                return Enchantment.DURABILITY;
            case "fire_aspect":
                return Enchantment.FIRE_ASPECT;
            case "frost_walker":
                return Enchantment.FROST_WALKER;
            case "impaling":
                return Enchantment.IMPALING;
            case "knockback":
                return Enchantment.KNOCKBACK;
            case "loot_bonus_blocks":
            case "looting":
                return Enchantment.LOOT_BONUS_BLOCKS;
            case "loyalty":
                return Enchantment.LOYALTY;
            case "luck":
            case "luck_of_the_sea":
                return Enchantment.LUCK;
            case "lure":
                return Enchantment.LURE;
            case "mending":
                return Enchantment.MENDING;
            case "multishot":
                return Enchantment.MULTISHOT;
            case "piercing":
                return Enchantment.PIERCING;
            case "protection_environmental":
            case "protection":
                return Enchantment.PROTECTION_ENVIRONMENTAL;
            case "protection_explosions":
            case "blast_protection":
                return Enchantment.PROTECTION_EXPLOSIONS;
            case "protection_fall":
            case "feather_falling":
                return Enchantment.PROTECTION_FALL;
            case "projectile_protection":
                return Enchantment.PROTECTION_PROJECTILE;
            case "quick_charge":
                return Enchantment.QUICK_CHARGE;
            case "riptide":
                return Enchantment.RIPTIDE;
            case "silk_touch":
                return Enchantment.SILK_TOUCH;
            case "soul_speed":
                return Enchantment.SOUL_SPEED;
            case "sweeping_edge":
                return Enchantment.SWEEPING_EDGE;
            case "thorns":
                return Enchantment.THORNS;
            case "vanishing_curse":
            case "curse_of_vanishing":
                return Enchantment.VANISHING_CURSE;
            case "water_worker":
            case "respiration":
                return Enchantment.WATER_WORKER;
            default:
                try {
                    final NamespacedKey namespacedKey = new NamespacedKey(main, enchantmentName);
                    return Enchantment.getByKey(namespacedKey);
                } catch (Exception e) {
                    Utils.logger.warning("Invalid enchantment: " + enchantmentName);
                }
                return null;
        }
    }
}
