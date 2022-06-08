package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.util.Utils;

/**
 * Used as a placeholder for when a number or a number-range is passed from a user argument
 *
 * @author stumper66
 * @since 3.2.0
 */
public class RequestedLevel {

    public int level;
    public int levelRangeMin;
    public int levelRangeMax;
    public boolean hasLevelRange;
    public boolean hadInvalidArguments;

    public void setMinAllowedLevel(final int level) {
        if (this.hasLevelRange && this.levelRangeMin < level) {
            this.levelRangeMin = level;
        } else if (!this.hasLevelRange && this.level < levelRangeMax) {
            this.level = level;
        }
    }

    public void setMaxAllowedLevel(final int level) {
        if (this.hasLevelRange && this.levelRangeMax > level) {
            this.levelRangeMax = level;
        } else if (!this.hasLevelRange && this.level > level) {
            this.level = level;
        }
    }

    public int getLevelMin() {
        if (this.hasLevelRange) {
            return this.levelRangeMin;
        } else {
            return this.level;
        }
    }

    public int getLevelMax() {
        if (this.hasLevelRange) {
            return this.levelRangeMax;
        } else {
            return this.level;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean setLevelFromString(final String numberOrNumberRange) {
        if (numberOrNumberRange == null || numberOrNumberRange.isEmpty()) {
            return false;
        }

        if (!numberOrNumberRange.contains("-")) {
            if (!Utils.isInteger(numberOrNumberRange)) {
                return false;
            }

            this.level = Integer.parseInt(numberOrNumberRange);
            this.hasLevelRange = false;
            return true;
        }

        final String[] nums = numberOrNumberRange.split("-");
        if (nums.length != 2) {
            return false;
        }

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) {
            return false;
        }
        this.levelRangeMin = Integer.parseInt(nums[0].trim());
        this.levelRangeMax = Integer.parseInt(nums[1].trim());
        this.hasLevelRange = true;

        return true;
    }

    public String toString() {
        if (this.hasLevelRange) {
            return String.format("%s-%s", this.levelRangeMin, this.levelRangeMax);
        } else {
            return String.valueOf(this.level);
        }
    }
}
