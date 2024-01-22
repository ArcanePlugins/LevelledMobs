/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.util;

import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.customdrops.DeathCause;
import me.lokka30.levelledmobs.managers.DebugManager;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.result.PlayerNetherOrWorldSpawnResult;
import me.lokka30.levelledmobs.rules.LevelledMobSpawnReason;
import me.lokka30.levelledmobs.rules.MinAndMax;
import me.lokka30.levelledmobs.rules.RulesManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds common utilities
 *
 * @author lokka30, stumper66
 * @since 2.5.0
 */
public final class Utils {

    /**
     * Use static methods, e.g. Utils.round, not new Utils().round for example.
     */
    private Utils() {
        throw new UnsupportedOperationException();
    }

    @NotNull public static final MicroLogger logger = new MicroLogger("&bLevelledMobs:&7 ");
    private static final Pattern timeUnitPattern = Pattern.compile("(\\d+\\.?\\d+|\\d+)?(\\w+)");

    /**
     * Rounds value to 2 decimal points.
     *
     * @param value value to round
     * @return rounded value
     */
    public static double round(final double value) {
        return Math.round(value * 100) / 100.00;
    }

    public static double round(final double value, final int digits) {
        final double scale = Math.pow(10, digits);
        return Math.round(value * scale) / scale;
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
    @NotNull public static String replaceEx(@NotNull final String message, @NotNull final String replaceWhat,
        @NotNull final String replaceTo) {
        int count, position0, position1;
        count = position0 = 0;
        final String upperString = message.toUpperCase();
        final String upperPattern = replaceWhat.toUpperCase();
        final int inc = (message.length() / replaceWhat.length()) *
            (replaceTo.length() - replaceWhat.length());
        final char[] chars = new char[message.length() + Math.max(0, inc)];
        while ((position1 = upperString.indexOf(upperPattern, position0)) != -1) {
            for (int i = position0; i < position1; ++i) {
                chars[count++] = message.charAt(i);
            }
            for (int i = 0; i < replaceTo.length(); ++i) {
                chars[count++] = replaceTo.charAt(i);
            }
            position0 = position1 + replaceWhat.length();
        }
        if (position0 == 0) {
            return message;
        }
        for (int i = position0; i < message.length(); ++i) {
            chars[count++] = message.charAt(i);
        }

        return new String(chars, 0, count);
    }

    /**
     * Check if str is an integer
     *
     * @param str str to check
     * @return if str is an integer (e.g. "1234" = true, "hello" = false)
     */
    public static boolean isInteger(@Nullable final String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }

        try {
            Integer.parseInt(str);
            return true;
        } catch (final NumberFormatException ex) {
            return false;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isDouble(@Nullable final String str) {
        if (isNullOrEmpty(str)) {
            return false;
        }

        try {
            Double.parseDouble(str);
            return true;
        } catch (final NumberFormatException ex) {
            return false;
        }
    }

    public static boolean isNullOrEmpty(@Nullable final String str) {
        return (str == null || str.isEmpty());
    }

    @NotNull public static final List<String> oneToNine = List.of("1", "2", "3", "4", "5", "6", "7", "8",
        "9");

    @NotNull public static List<String> replaceAllInList(@NotNull final List<String> oldList,
        @NotNull final String replaceWhat, @NotNull final String replaceTo) {
        final List<String> newList = new ArrayList<>(oldList.size());
        for (final String string : oldList) {
            newList.add(string.replace(replaceWhat, replaceTo));
        }
        return newList;
    }

    @NotNull public static List<String> colorizeAllInList(@NotNull final List<String> oldList) {
        final List<String> newList = new ArrayList<>(oldList.size());

        for (final String string : oldList) {
            newList.add(MessageUtils.colorizeAll(string));
        }

        return newList;
    }

    /**
     * Puts the string into lowercase and makes every character that starts a word a capital
     * letter.
     * <p>
     * e.g. from: wiTheR sKeLeTOn to: Wither Skeleton
     *
     * @param str string to capitalize
     * @return a string with each word capitalized
     */
    @NotNull public static String capitalize(@NotNull final String str) {
        final StringBuilder builder = new StringBuilder();
        final String[] words = str.toLowerCase().split(" "); // each word separated from str
        for (int i = 0; i < words.length; i++) {
            final String word = words[i];
            if (word.isEmpty()) {
                continue;
            }

            builder.append(String.valueOf(word.charAt(0)).toUpperCase()); // capitalize first letter
            if (word.length() > 1) {
                builder.append(word.substring(1)); // append the rest of the word
            }

            // if there is another word to capitalize, then add a space
            if (i < words.length - 1) {
                builder.append(" ");
            }
        }

        return builder.toString();
    }

    public static boolean isLivingEntityInModalList(@NotNull final CachedModalList<String> list,
        final LivingEntityWrapper lmEntity, final boolean checkBabyMobs) {
        if (list.allowAll) {
            return true;
        }
        if (list.excludeAll) {
            return false;
        }
        if (list.isEmpty()) {
            return true;
        }

        final String checkName = checkBabyMobs ?
            lmEntity.getNameIfBaby() :
            lmEntity.getTypeName();

        for (final String group : lmEntity.getApplicableGroups()) {
            if (list.excludedGroups.contains(group)) {
                return false;
            }
        }

        // for denies we'll check for both baby and adult variants regardless of baby-mobs-inherit-adult-setting
        if (list.excludedList.contains(lmEntity.getTypeName()) || list.excludedList.contains(
            lmEntity.getNameIfBaby()) ||
            lmEntity.isBabyMob() && list.excludedList.contains("baby_")) {
            return false;
        }

        for (final String group : lmEntity.getApplicableGroups()) {
            if (list.allowedGroups.contains(group)) {
                return true;
            }
        }

        return list.isBlacklist() || list.allowedList.contains(checkName) ||
            lmEntity.isBabyMob() && list.allowedList.contains("baby_");
    }

    public static boolean isIntegerInModalList(@NotNull final CachedModalList<MinAndMax> list,
        final int checkNum) {
        if (list.allowAll) {
            return true;
        }
        if (list.excludeAll) {
            return false;
        }
        if (list.isEmpty()) {
            return true;
        }

        for (final MinAndMax exclude : list.excludedList) {
            if (checkNum >= exclude.min && checkNum <= exclude.max) {
                return false;
            }
        }

        if (list.isBlacklist()) {
            return true;
        }

        for (final MinAndMax include : list.allowedList) {
            if (checkNum >= include.min && checkNum <= include.max) {
                return true;
            }
        }

        return false;
    }


    public static boolean isBiomeInModalList(@NotNull final CachedModalList<Biome> list,
        final Biome biome, final RulesManager rulesManager) {
        if (list.allowAll) {
            return true;
        }
        if (list.excludeAll) {
            return false;
        }
        if (list.isEmpty()) {
            return true;
        }

        for (final String group : list.excludedGroups) {
            if (rulesManager.biomeGroupMappings.containsKey(group) &&
                rulesManager.biomeGroupMappings.get(group).contains(biome.toString())) {
                return false;
            }
        }

        if (list.excludedList.contains(biome)) {
            return false;
        }

        for (final String group : list.allowedGroups) {
            if (rulesManager.biomeGroupMappings.containsKey(group) &&
                rulesManager.biomeGroupMappings.get(group).contains(biome.toString())) {
                return true;
            }
        }

        return list.isBlacklist() || list.allowedList.contains(biome);
    }

    public static boolean isDamageCauseInModalList(
        @NotNull final CachedModalList<DeathCause> list, final DeathCause cause) {
        if (list.allowAll) {
            return true;
        }
        if (list.excludeAll) {
            return false;
        }
        if (list.isEmpty()) {
            return true;
        }

        // note: no group support

        if (list.excludedList.contains(cause)) {
            return false;
        }

        return list.isBlacklist() || list.allowedList.contains(cause);
    }

    public static long getMillisecondsFromInstant(final Instant instant) {
        return Duration.between(instant, Instant.now()).toMillis();
    }

    @NotNull public static PlayerNetherOrWorldSpawnResult getPortalOrWorldSpawn(
        final @NotNull LevelledMobs main, final @NotNull Player player) {
        Location location = null;
        boolean isNetherPortalCoord = false;
        boolean isWorldPortalCoord = false;

        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            location = main.companion.getPlayerNetherPortalLocation(player);
            isNetherPortalCoord = true;
        } else if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
            location = main.companion.getPlayerWorldPortalLocation(player);
            isWorldPortalCoord = true;
        }

        if (location == null) {
            location = player.getWorld().getSpawnLocation();
            isNetherPortalCoord = false;
            isWorldPortalCoord = false;
        }

        return new PlayerNetherOrWorldSpawnResult(location, isNetherPortalCoord,
            isWorldPortalCoord);
    }

    public static long getChunkKey(final @NotNull Chunk chunk) {
        if (LevelledMobs.getInstance().getVerInfo().getIsRunningPaper()) {
            return chunk.getChunkKey();
        }

        final int x = chunk.getX() >> 4, z = chunk.getZ() >> 4;
        return (long) x & 0xffffffffL | ((long) z & 0xffffffffL) << 32;
    }

    public static String displayChunkLocation(final @NotNull Location location) {
        return String.format("%s,%s", location.getChunk().getX(), location.getChunk().getZ());
    }

    // take from https://www.techiedelight.com/five-alternatives-pair-class-java/
    @Contract(value = "_, _ -> new", pure = true)
    public static <T, U> Map.@NotNull Entry<T, U> getPair(T first, U second) {
        return new AbstractMap.SimpleEntry<>(first, second);
    }

    public static LevelledMobSpawnReason adaptVanillaSpawnReason(
            final CreatureSpawnEvent.@NotNull SpawnReason spawnReason) {
        return LevelledMobSpawnReason.valueOf(spawnReason.toString());
    }

    public static boolean matchWildcardString(final @NotNull String input, final @NotNull String match){
        if (!match.contains("*")){
            return input.equalsIgnoreCase(match);
        }

        final String[] chopped = match.split("\\*");
        // 0 = *, 1 = text, 2 = *
        if (chopped.length > 3){
            Utils.logger.warning("Invalid wildcard pattern: " + match);
            return input.equalsIgnoreCase(match);
        }

        final String inputL = input.toLowerCase();
        final String matchL = match.toLowerCase();

        String useSearch;
        if (matchL.startsWith("*") && matchL.endsWith("*")){
            useSearch = matchL.substring(1, matchL.length() - 1);
            return inputL.contains(useSearch);
        }
        else if (matchL.startsWith("*")){
            useSearch = matchL.substring(1);
            return inputL.endsWith(useSearch);
        }
        else{
            useSearch = matchL.substring(0, matchL.length() - 1);
            return inputL.startsWith(useSearch);
        }
    }

    public static String removeColorCodes(final @NotNull String input){
        String formatted = input.replace("§", "&");

        if (input.contains("&")){
            formatted = input.replaceAll("&.", "");
        }
        return formatted;
    }

    public static @NotNull String showLocation(final @NotNull Location location){
        return String.format("%s at %s,%s,%s",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    public static boolean checkIfMobHashChanged(final @NotNull LivingEntityWrapper lmEntity){
        final LevelledMobs main = LevelledMobs.getInstance();

        if (!lmEntity.getPDC().has(main.namespacedKeys.mobHash, PersistentDataType.STRING)) {
            return true;
        }

        boolean hadHash = false;
        String mobHash = null;
        if (lmEntity.getPDC().has(main.namespacedKeys.mobHash, PersistentDataType.STRING)) {
            mobHash = lmEntity.getPDC().get(main.namespacedKeys.mobHash, PersistentDataType.STRING);
            hadHash = true;
        }

        final boolean hashChanged = !main.rulesManager.getCurrentRulesHash().equals(mobHash);
        if (hashChanged) {
            if (hadHash){
                DebugManager.log(DebugType.MOB_HASH, lmEntity, false, () -> String.format("Invalid hash for %s %s"
                        , lmEntity.getNameIfBaby(), Utils.showLocation(lmEntity.getLocation())));
            }
            else{
                DebugManager.log(DebugType.MOB_HASH, lmEntity, false, () -> String.format("Hash missing for %s %s"
                        , lmEntity.getNameIfBaby(), Utils.showLocation(lmEntity.getLocation())));
            }

            // also setting the PDC key here because if the mob is not eligable for levelling then it will
            // run this same code repeatidly
            lmEntity.getPDC().set(main.namespacedKeys.mobHash, PersistentDataType.STRING, main.rulesManager.getCurrentRulesHash());
        }
        else {
            DebugManager.log(DebugType.MOB_HASH, lmEntity, true, () -> String.format("Hash missing for %s %s"
                    , lmEntity.getNameIfBaby(), Utils.showLocation(lmEntity.getLocation())));
        }

        return hashChanged;
    }

    public static Long parseTimeUnit(final @Nullable String input, final Long defaultTime,
                                     final boolean useMS, final @Nullable CommandSender sender) {
        if (input == null) {
            return defaultTime;
        }
        if ("0".equals(input)) {
            return 0L;
        }

        final Matcher match = timeUnitPattern.matcher(input);

        if (!match.matches() || match.groupCount() != 2) {
            if (sender != null)
                sender.sendMessage("Invalid time: " + input);
            else
                Utils.logger.warning("Invalid time: " + input);

            return defaultTime;
        }

        long time;
        double remainder = 0.0;
        String numberPart = match.group(1) != null ? match.group(1) : match.group(2);
        String unit = match.group(1) != null ? match.group(2).toLowerCase() : "";

        if (Utils.isInteger(input)){
            // number only, no time unit was specified
            numberPart = input;
            unit = "";
        }

        if (numberPart.contains(".")) {
            final String[] split = numberPart.split("\\.");
            try {
                remainder = 1.0 - Double.parseDouble("0." + split[1]);
                numberPart = split[0];
            } catch (Exception e) {
                if (sender != null)
                    sender.sendMessage("Invalid time: " + input);
                else
                    Utils.logger.warning("Invalid time: " + input);

                return defaultTime;
            }
        }

        try {
            time = Long.parseLong(numberPart);
        } catch (Exception e) {
            if (sender != null)
                sender.sendMessage("Invalid time: " + input);
            else
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
                if (sender != null)
                    sender.sendMessage("Invalid time unit specified: " + input + " (" + unit + ")");
                else
                    Utils.logger.warning("Invalid time unit specified: " + input + " (" + unit + ")");
            }
        }

        if (duration != null) {
            return useMS ?
                    duration.toMillis() :
                    duration.getSeconds();
        }

        return defaultTime;
    }
}
