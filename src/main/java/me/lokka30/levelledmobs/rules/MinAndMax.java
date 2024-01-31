package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds two int or float values that are usually used to
 * define a min and max value
 *
 * @author stumper66
 * @since 3.2.0
 */
public class MinAndMax implements Comparable<MinAndMax> {

    public float min;
    public float max;
    public boolean showAsInt;

    public int getMinAsInt(){
        return (int) min;
    }

    public int getMaxAsInt(){
        return (int) max;
    }

    public static @Nullable MinAndMax setAmountRangeFromString(final @Nullable String numberOrNumberRange) {
        if (numberOrNumberRange == null || numberOrNumberRange.isEmpty()) {
            return null;
        }

        if (!numberOrNumberRange.contains("-")) {
            if (!Utils.isDouble(numberOrNumberRange)) {
                return null;
            }

            final MinAndMax result = new MinAndMax();
            result.min = (float) Double.parseDouble(numberOrNumberRange);
            result.max = result.min;
            return result;
        }

        final String[] nums = numberOrNumberRange.split("-");
        if (nums.length != 2) {
            return null;
        }

        if (!Utils.isDouble(nums[0].trim()) || !Utils.isDouble(nums[1].trim())) {
            return null;
        }

        final MinAndMax result = new MinAndMax();
        result.min = (float) Double.parseDouble(nums[0].trim());
        result.max = (float) Double.parseDouble(nums[1].trim());

        return result;
    }

    public boolean isEmpty() {
        return (min == 0.0f && max == 0.0f);
    }

    public String toString() {
        if (this.min == this.max){
            if (showAsInt)
                return String.valueOf(getMinAsInt());
            else
                return String.valueOf(this.min);
        }
        else{
            if (showAsInt)
                return String.format("%s-%s", getMinAsInt(), getMaxAsInt());
            else
                return String.format("%s-%s", this.min, this.max);
        }
    }

    @Override
    public int compareTo(@NotNull final MinAndMax o) {
        if (o.min == this.min && o.max == this.max) {
            return 0;
        } else {
            return 1;
        }
    }
}
