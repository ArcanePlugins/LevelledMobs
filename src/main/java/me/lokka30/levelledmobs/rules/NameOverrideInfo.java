package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.misc.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class NameOverrideInfo {
    public NameOverrideInfo() {
        this.names = new LinkedList<>();
    }

    @NotNull
    public List<String> names;
    public Integer minLevel;
    public Integer maxLevel;

    public boolean hasLevelRestriction(){
        return (minLevel != null || maxLevel != null);
    }

    public boolean isApplicableToMobLevel(final int mobLevel){
        if (!this.hasLevelRestriction()) return true;

        final boolean meetsMin = minLevel == null || mobLevel >= minLevel;
        final boolean meetsMax = maxLevel == null || mobLevel <= maxLevel;

        return meetsMin && meetsMax;
    }

    public boolean setRangeFromString(final String range){
        if (range == null || range.isEmpty()) return false;

        if (!range.contains("-")){
            if (!Utils.isInteger(range)) return false;

            this.minLevel = Integer.parseInt(range);
            this.maxLevel = this.minLevel;
            return true;
        }

        String[] nums = range.split("-");
        if (nums.length != 2) return false;

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) return false;
        this.minLevel = Integer.parseInt(nums[0].trim());
        this.maxLevel = Integer.parseInt(nums[1].trim());

        return true;
    }

    public String toString(){
        if (!hasLevelRestriction()) return names.toString();

        if (minLevel != null && maxLevel != null)
            return String.format("%s-%s %s", minLevel, maxLevel, names);
        if (minLevel != null)
            return String.format("%s- %s", minLevel, names);
        else
            return String.format("-%s %s", maxLevel, names);
    }
}
