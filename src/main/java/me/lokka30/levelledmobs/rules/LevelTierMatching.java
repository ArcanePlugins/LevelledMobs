package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.misc.Utils;

import java.util.Arrays;
import java.util.List;

public class LevelTierMatching {
    public List<String> names;
    public int[] valueRanges;
    public Integer minLevel;
    public Integer maxLevel;
    public String mobName;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasLevelRestriction(){
        return (minLevel != null || maxLevel != null);
    }

    public boolean isApplicableToMobLevel(final int mobLevel){
        if (!this.hasLevelRestriction()) return true;

        final boolean meetsMin = minLevel == null || mobLevel >= minLevel;
        final boolean meetsMax = maxLevel == null || mobLevel <= maxLevel;

        return meetsMin && meetsMax;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean setRangeFromString(final String range){
        final int[] result = getRangeFromString(range);

        if (result[0] == -1 && result[1] == -1)
            return false;

        if (result[0] >= 0) this.minLevel = result[0];
        if (result[1] >= 0) this.maxLevel = result[1];

        return true;
    }

    public static int[] getRangeFromString(final String range){
        final int[] result = new int[]{ -1, -1};

        if (range == null || range.isEmpty()) return result;

        if (!range.contains("-")){
            if (!Utils.isInteger(range)) return result;

            result[0] = Integer.parseInt(range);
            result[1] = result[0];
            return result;
        }

        final String[] nums = range.split("-");
        if (nums.length != 2) return result;

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) return result;
        result[0] = Integer.parseInt(nums[0].trim());
        result[1] = Integer.parseInt(nums[1].trim());

        return result;
    }

    public String toString(){
        if (!hasLevelRestriction()) return names.toString();

        if (minLevel != null && maxLevel != null)
            return String.format("%s-%s %s", minLevel, maxLevel, names == null ? Arrays.toString(valueRanges) : names);
        if (minLevel != null)
            return String.format("%s- %s", minLevel, names == null ? Arrays.toString(valueRanges) : names);
        else
            return String.format("-%s %s", maxLevel, names == null ? Arrays.toString(valueRanges) : names);
    }
}
