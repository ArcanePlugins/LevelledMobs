/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public YmlParsingHelper() {
        this.timeUnitPattern = Pattern.compile("(\\d+\\.?\\d+|\\d+)?(\\w+)");
    }

    private final Pattern timeUnitPattern;

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
            Long result = parseTimeUnit(temp, useDefaultValue, false);
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
            return parseTimeUnit(temp, defaultValue, true);
        } else {
            return defaultValue;
        }
    }

    private Long parseTimeUnit(final @Nullable String input, final Long defaultTime,
        final boolean useMS) {
        if (input == null) {
            return defaultTime;
        }
        if ("0".equals(input)) {
            return 0L;
        }

        final Matcher match = timeUnitPattern.matcher(input);

        if (!match.matches() || match.groupCount() != 2) {
            Utils.logger.warning("Invalid time: " + input);
            return defaultTime;
        }

        long time;
        double remainder = 0.0;
        String numberPart = match.group(1) != null ? match.group(1) : match.group(2);
        final String unit = match.group(1) != null ? match.group(2).toLowerCase() : "";

        if (numberPart.contains(".")) {
            final String[] split = numberPart.split("\\.");
            try {
                remainder = 1.0 - Double.parseDouble("0." + split[1]);
                numberPart = split[0];
            } catch (Exception e) {
                Utils.logger.warning("Invalid time: " + input);
                return defaultTime;
            }
        }

        try {
            time = Long.parseLong(numberPart);
        } catch (Exception e) {
            Utils.logger.warning("Invalid time: " + input);
            return defaultTime;
        }

        Duration duration = null;
        switch (unit) {
            case "ms", "millisecond", "milliseconds" -> duration = Duration.ofMillis(time);
            case "s", "second", "seconds" -> {
                duration = Duration.ofSeconds(time);
                if (remainder > 0.0) {
                    duration = duration.plusMillis((long) (1000.0 * remainder));
                }
            }
            case "m", "minute", "minutes" -> {
                duration = Duration.ofMinutes(time);
                if (remainder > 0.0) {
                    duration = duration.plusMillis((long) (60000.0 * remainder));
                }
            }
            case "h", "hour", "hours" -> {
                duration = Duration.ofHours(time);
                if (remainder > 0.0) {
                    duration = duration.plusMillis((long) (3600000.0 * remainder));
                }
            }
            case "d", "day", "days" -> {
                duration = Duration.ofDays(time);
                if (remainder > 0.0) {
                    duration = duration.plusSeconds((long) (86400.0 * remainder));
                }
            }
            case "" -> duration = useMS ? Duration.ofMillis(time) : Duration.ofSeconds(time);
            default -> {
                Utils.logger.warning("Invalid time unit specified: " + input + " (" + unit + ")");
                Utils.logger.info(String.format("%s, %s", match.group(1), match.group(1)));
            }
        }

        if (duration != null) {
            return useMS ?
                duration.toMillis() :
                duration.getSeconds();
        }

        return defaultTime;
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
            final String checkKeyName = sb.length() == 0 ? thisKey : sb.toString();
            final ConfigurationSection useCS = keysFound == 0 ? cs : objTo_CS(cs, checkKeyName);

            if (useCS == null) {
                break;
            }

            for (final String enumeratedKey : useCS.getKeys(false)) {
                if (thisKey.equalsIgnoreCase(enumeratedKey)) {
                    if (sb.length() > 0) {
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
            if (sb.length() > 0) {
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
