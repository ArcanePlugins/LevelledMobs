/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author stumper66
 * @since 3.1.0
 */
public class YmlParsingHelper {
    public boolean getBoolean(final ConfigurationSection cs, @NotNull final String name) {
        return getBoolean(cs, name, false);
    }

    public boolean getBoolean(final ConfigurationSection cs, @NotNull final String name,
        final boolean defaultValue) {
        if (cs == null) {
            return defaultValue;
        }
        final String useName = getKeyNameFromConfig(cs, name);

        return cs.getBoolean(useName, defaultValue);
    }

    @Nullable public Boolean getBoolean2(final ConfigurationSection cs, @NotNull final String name,
        final Boolean defaultValue) {
        if (cs == null) {
            return defaultValue;
        }
        final String useName = getKeyNameFromConfig(cs, name);

        if (cs.get(useName) != null) {
            return cs.getBoolean(useName);
        } else {
            return defaultValue;
        }
    }

    @Nullable public String getString(final ConfigurationSection cs, @NotNull final String name) {
        return getString(cs, name, null);
    }

    @Nullable public String getString(final ConfigurationSection cs, @NotNull final String name,
        final String defaultValue) {
        if (cs == null) {
            return defaultValue;
        }
        final String useName = getKeyNameFromConfig(cs, name);

        return cs.getString(useName, defaultValue);
    }

    @NotNull public Set<String> getStringSet(final ConfigurationSection cs, @NotNull final String name) {
        final String useName = getKeyNameFromConfig(cs, name);

        final Set<String> results = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        // rather than use addAll we'll make sure there no empty strings
        for (final String item : cs.getStringList(useName)) {
            if (!item.isEmpty()) {
                results.add(item);
            }
        }

        return results;
    }

    public int getInt(final ConfigurationSection cs, @NotNull final String name) {
        return getInt(cs, name, 0);
    }

    public int getInt(final ConfigurationSection cs, @NotNull final String name,
        final int defaultValue) {
        if (cs == null) {
            return defaultValue;
        }

        final String useName = getKeyNameFromConfig(cs, name);
        return cs.getInt(useName, defaultValue);
    }

    @Nullable public Integer getInt2(final ConfigurationSection cs, @NotNull final String name,
        final Integer defaultValue) {
        if (cs == null) {
            return defaultValue;
        }

        final String useName = getKeyNameFromConfig(cs, name);

        if (cs.get(useName) != null) {
            return cs.getInt(useName);
        } else {
            return defaultValue;
        }
    }

    @SuppressWarnings("unused")
    private double getDouble(final ConfigurationSection cs, @NotNull final String name) {
        if (cs == null) {
            return 0;
        }
        final String useName = getKeyNameFromConfig(cs, name);

        return cs.getDouble(useName, 0);
    }

    @Nullable public Double getDouble2(final ConfigurationSection cs, @NotNull final String name,
        final Double defaultValue) {
        if (cs == null) {
            return defaultValue;
        }
        final String useName = getKeyNameFromConfig(cs, name);

        if (cs.get(useName) != null) {
            return cs.getDouble(useName);
        } else {
            return defaultValue;
        }
    }

    public float getFloat(final ConfigurationSection cs, @NotNull final String name) {
        return getFloat(cs, name, 0.0F);
    }

    public float getFloat(final ConfigurationSection cs, @NotNull final String name,
        final float defaultValue) {
        if (cs == null) {
            return defaultValue;
        }
        final String useName = getKeyNameFromConfig(cs, name);

        return (float) cs.getDouble(useName, defaultValue);
    }

    @Nullable public Float getFloat2(final ConfigurationSection cs, @NotNull final String name,
        final Float defaultValue) {
        if (cs == null) {
            return defaultValue;
        }
        final String useName = getKeyNameFromConfig(cs, name);

        if (cs.get(useName) != null) {
            return (float) cs.getDouble(useName);
        } else {
            return defaultValue;
        }
    }

    @Nullable public Integer getIntTimeUnit(final ConfigurationSection cs, @NotNull final String name,
        final Integer defaultValue) {
        if (cs == null) {
            return defaultValue;
        }
        final String useName = getKeyNameFromConfig(cs, name);

        if (cs.get(useName) != null) {
            if (cs.getInt(useName) > 0) {
                return cs.getInt(useName);
            }

            final String temp = cs.getString(useName);
            final Long useDefaultValue = defaultValue != null ? Long.valueOf(defaultValue) : null;
            Long result = Utils.parseTimeUnit(temp, useDefaultValue, false, null);
            return result != null ?
                Math.toIntExact(result) :
                null;
        } else {
            return defaultValue;
        }
    }

    public Long getIntTimeUnitMS(final ConfigurationSection cs, @NotNull final String name,
        final Long defaultValue) {
        if (cs == null) {
            return defaultValue;
        }
        final String useName = getKeyNameFromConfig(cs, name);

        if (cs.get(useName) != null) {
            if (cs.getLong(useName) > 0) {
                return cs.getLong(useName);
            }
            final String temp = cs.getString(useName);
            return Utils.parseTimeUnit(temp, defaultValue, true, null);
        } else {
            return defaultValue;
        }
    }

    @NotNull public static List<String> getListFromConfigItem(@NotNull final ConfigurationSection cs,
        final String key) {
        String foundKeyName = null;
        for (final String enumeratedKey : cs.getKeys(false)) {
            if (key.equalsIgnoreCase(enumeratedKey)) {
                foundKeyName = enumeratedKey;
                break;
            }
        }

        if (foundKeyName == null) {
            return new LinkedList<>();
        }

        final List<String> result = cs.getStringList(foundKeyName);
        if (result.isEmpty() && cs.getString(foundKeyName) != null && !"".equals(
            cs.getString(foundKeyName))) {
            result.add(cs.getString(foundKeyName));
        }

        return result;
    }

    public @NotNull List<String> getStringOrList(final @Nullable ConfigurationSection cs, final @NotNull String key){
        final List<String> results = new LinkedList<>();
        if (cs == null) {
            return results;
        }

        String foundKeyName = null;
        for (final String enumeratedKey : cs.getKeys(false)) {
            if (key.equalsIgnoreCase(enumeratedKey)) {
                foundKeyName = enumeratedKey;
                break;
            }
        }

        if (foundKeyName == null) {
            return results;
        }

        final List<?> lst = cs.getList(foundKeyName);
        if (lst != null && !lst.isEmpty()){
            for (final Object item : lst){
                if (!item.toString().isEmpty()) results.add(item.toString());
            }

            return results;
        }

        final String temp = cs.getString(foundKeyName);
        if (temp != null && !temp.isEmpty())
            results.add(temp);

        return results;
    }

    @NotNull public String getKeyNameFromConfig(final @NotNull ConfigurationSection cs,
        final @NotNull String key) {
        if (!key.contains(".")) {
            for (final String enumeratedKey : cs.getKeys(false)) {
                if (key.equalsIgnoreCase(enumeratedKey)) {
                    return enumeratedKey;
                }
            }

            return key;
        }

        // key contains one or more periods

        final String[] periodSplit = (key.split("\\."));
        final StringBuilder sb = new StringBuilder(periodSplit.length);
        int keysFound = 0;

        for (final String thisKey : periodSplit) {
            boolean foundKey = false;
            final String checkKeyName = sb.isEmpty() ? thisKey : sb.toString();
            final ConfigurationSection useCS = keysFound == 0 ? cs : objTo_CS(cs, checkKeyName);

            if (useCS == null) {
                break;
            }

            for (final String enumeratedKey : useCS.getKeys(false)) {
                if (thisKey.equalsIgnoreCase(enumeratedKey)) {
                    if (!sb.isEmpty()) {
                        sb.append(".");
                    }
                    sb.append(enumeratedKey);
                    foundKey = true;
                    keysFound++;
                    break;
                }
            }
            if (!foundKey) {
                break;
            }
        }

        // if only some of the keys were found then add the remaining ones
        for (int i = keysFound; i < periodSplit.length; i++) {
            if (!sb.isEmpty()) {
                sb.append(".");
            }
            sb.append(periodSplit[i]);
        }

        return sb.toString();
    }

    @Nullable public ConfigurationSection objTo_CS(final ConfigurationSection cs, final String path) {
        if (cs == null) {
            return null;
        }
        final String useKey = getKeyNameFromConfig(cs, path);
        final Object object = cs.get(useKey);

        if (object == null) {
            return null;
        }

        if (object instanceof ConfigurationSection) {
            return (ConfigurationSection) object;
        } else if (object instanceof Map) {
            final MemoryConfiguration result = new MemoryConfiguration();
            //noinspection unchecked
            result.addDefaults((Map<String, Object>) object);
            return result.getDefaultSection();
        } else {
            final String currentPath = Utils.isNullOrEmpty(cs.getCurrentPath()) ?
                path : cs.getCurrentPath() + "." + path;
            Utils.logger.warning(
                currentPath + ": couldn't parse Config of type: " + object.getClass()
                    .getSimpleName() + ", value: " + object);
            return null;
        }
    }
}
