package me.lokka30.levelledmobs.misc;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

    @NotNull
    public static Set<String> getStringSet(final ConfigurationSection cs, @NotNull final String name){
        final Set<String> results = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        results.addAll(cs.getStringList(name));
        return results;
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

    @NotNull
    public static String getKeyNameFromConfig(final ConfigurationSection cs, final String key){
        if (!key.contains(".")){
            for (final String enumeratedKey : cs.getKeys(false)) {
                if (key.equalsIgnoreCase(enumeratedKey))
                    return enumeratedKey;
            }

            return key;
        }

        // key contains one or more periods

        final String[] periodSplit = (key.split("\\."));
        final StringBuilder sb = new StringBuilder(periodSplit.length);
        int keysFound = 0;

        for (final String thisKey : periodSplit) {
            boolean foundKey = false;
            final String checkKeyName = sb.length() == 0 ? thisKey : sb.toString();
            final ConfigurationSection useCS = keysFound == 0 ? cs : objectToConfigurationSection(cs.get(checkKeyName));

            if (useCS == null) break;

            for (final String enumeratedKey : useCS.getKeys(false)) {
                if (thisKey.equalsIgnoreCase(enumeratedKey)) {
                    if (sb.length() > 0) sb.append(".");
                    sb.append(enumeratedKey);
                    foundKey = true;
                    keysFound++;
                    break;
                }
            }
            if (!foundKey) break;
        }

        // if only some of the keys were found then add the remaining ones
        for (int i = keysFound; i < periodSplit.length; i++){
            if (sb.length() > 0) sb.append(".");
            sb.append(periodSplit[i]);
        }

        return sb.toString();
    }

    @Nullable
    private static ConfigurationSection objectToConfigurationSection(final Object object){
        if (object == null) return null;

        if (object instanceof ConfigurationSection) {
            return (ConfigurationSection) object;
        } else if (object instanceof Map){
            final MemoryConfiguration result = new MemoryConfiguration();
            result.addDefaults((Map<String, Object>) object);
            return result.getDefaultSection();
        } else {
            Utils.logger.warning("couldn't parse Config of type: " + object.getClass().getSimpleName() + ", value: " + object);
            return null;
        }
    }
}
