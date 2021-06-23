package me.lokka30.levelledmobs.misc;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class YmlParsingHelper {
    public static boolean getBoolean(final ConfigurationSection cs, @NotNull final String name){
        return getBoolean(cs, name, false);
    }

    public static boolean getBoolean(final ConfigurationSection cs, @NotNull final String name, final boolean defaultValue){
        if (cs == null) return defaultValue;
        for (final String key : cs.getKeys(false)){
            if (name.equalsIgnoreCase(key)) return cs.getBoolean(key, defaultValue);
        }

        return defaultValue;
    }

    @Nullable
    public static Boolean getBoolean2(final ConfigurationSection cs, @NotNull final String name, final Boolean defaultValue){
        if (cs == null) return defaultValue;
        for (final String key : cs.getKeys(false)){
            if (name.equalsIgnoreCase(key)) return cs.getBoolean(key);
        }

        return defaultValue;
    }

    @Nullable
    public static String getString(final ConfigurationSection cs, @NotNull final String name){
        return getString(cs, name, null);
    }

    @Nullable
    public static String getString(final ConfigurationSection cs, @NotNull final String name, final String defaultValue){
        if (cs == null) return defaultValue;
        for (final String key : cs.getKeys(false)){
            if (name.equalsIgnoreCase(key)) return cs.getString(key, defaultValue);
        }

        return defaultValue;
    }

    public static int getInt(final ConfigurationSection cs, @NotNull final String name){
        return getInt(cs, name, 0);
    }

    public static int getInt(final ConfigurationSection cs, @NotNull final String name, final int defaultValue){
        if (cs == null) return defaultValue;
        for (final String key : cs.getKeys(false)){
            if (name.equalsIgnoreCase(key)) return cs.getInt(key, defaultValue);
        }

        return defaultValue;
    }

    @Nullable
    public static Integer getInt2(final ConfigurationSection cs, @NotNull final String name, final Integer defaultValue){
        if (cs == null) return defaultValue;
        for (final String key : cs.getKeys(false)){
            if (name.equalsIgnoreCase(key)) return cs.getInt(key);
        }

        return defaultValue;
    }

    public static double getDouble(final ConfigurationSection cs, @NotNull final String name){
        return getDouble(cs, name, 0);
    }

    public static double getDouble(final ConfigurationSection cs, @NotNull final String name, final double defaultValue){
        if (cs == null) return defaultValue;
        for (final String key : cs.getKeys(false)){
            if (name.equalsIgnoreCase(key)) return cs.getDouble(key, defaultValue);
        }

        return defaultValue;
    }

    @Nullable
    public static Double getDouble2(final ConfigurationSection cs, @NotNull final String name, final Double defaultValue){
        if (cs == null) return defaultValue;
        for (final String key : cs.getKeys(false)){
            if (name.equalsIgnoreCase(key)) return cs.getDouble(key);
        }

        return defaultValue;
    }
}
