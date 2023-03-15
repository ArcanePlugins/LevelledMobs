/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;

/**
 * Holds any rule information relating to leveled tiers
 *
 * @author stumper66
 * @since 3.1.0
 */
public class LevelTierMatching {

    List<String> names;
    public int[] valueRanges;
    public String sourceTierName;
    public Integer minLevel;
    public Integer maxLevel;
    String mobName;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean hasLevelRestriction() {
        return (minLevel != null || maxLevel != null);
    }

    boolean isApplicableToMobLevel(final int mobLevel) {
        if (!this.hasLevelRestriction()) {
            return true;
        }

        final boolean meetsMin = minLevel == null || mobLevel >= minLevel;
        final boolean meetsMax = maxLevel == null || mobLevel <= maxLevel;

        return meetsMin && meetsMax;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean setRangeFromString(final String range) {
        final int[] result = getRangeFromString(range);

        if (result[0] == -1 && result[1] == -1) {
            return false;
        }

        if (result[0] >= 0) {
            this.minLevel = result[0];
        }
        if (result[1] >= 0) {
            this.maxLevel = result[1];
        }

        return true;
    }

    static int @NotNull [] getRangeFromString(final String range) {
        final int[] result = {-1, -1};

        if (range == null || range.isEmpty()) {
            return result;
        }

        if (!range.contains("-")) {
            if (!Utils.isInteger(range)) {
                return result;
            }

            result[0] = Integer.parseInt(range);
            result[1] = result[0];
            return result;
        }

        final String[] nums = range.split("-");
        if (nums.length != 2) {
            return result;
        }

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) {
            return result;
        }
        result[0] = Integer.parseInt(nums[0].trim());
        result[1] = Integer.parseInt(nums[1].trim());

        return result;
    }

    public String toString() {
        if (!hasLevelRestriction()) {
            if (names != null && !names.isEmpty()) {
                return names.toString();
            } else if (sourceTierName != null) {
                if (valueRanges == null) {
                    return sourceTierName;
                } else {
                    return String.format("%s: %s", sourceTierName, Arrays.toString(valueRanges));
                }
            } else {
                return "(empty)";
            }
        }

        if (minLevel != null && maxLevel != null) {
            return String.format("%s-%s %s", minLevel, maxLevel,
                names == null ? Arrays.toString(valueRanges) : names);
        }
        if (minLevel != null) {
            return String.format("%s- %s", minLevel,
                names == null ? Arrays.toString(valueRanges) : names);
        } else {
            return String.format("-%s %s", maxLevel,
                names == null ? Arrays.toString(valueRanges) : names);
        }
    }

    @Override
    public boolean equals(final Object obj){
        if (obj == null) return false;
        if (!(obj instanceof LevelTierMatching)) return false;

        return this.toString().equals(obj.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.toString());
    }
}
