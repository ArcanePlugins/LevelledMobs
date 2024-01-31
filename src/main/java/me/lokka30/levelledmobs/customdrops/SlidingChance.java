package me.lokka30.levelledmobs.customdrops;

import me.lokka30.levelledmobs.rules.MinAndMax;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Holds values that are used for a variable chance based on either
 * a float number or a defined set of tiers
 *
 * @author stumper66
 * @since 3.15.0
 */
public class SlidingChance implements Cloneable {
    public SlidingChance(){}

    public SlidingChance(final float chance){
        this.chance = chance;
    }

    public float chance;
    public @Nullable Map<MinAndMax, MinAndMax> changeRange;
    private @Nullable MinAndMax lastMatchedTier;
    private float lastResult;
    public @Nullable SlidingChance defaults;

    public boolean isDefault(){
        return chance == 0.0f && (changeRange == null || changeRange.isEmpty());
    }

    public boolean isAssuredChance(){
        return chance >= 1.0f && !isDefault();
    }

    public float getSlidingChance(final int mobLevel){
        Float result = null;

        for (int i = 0; i < 2; i++){
            // first check with this class settings
            // if no tiers are matched then check against defaults
            final SlidingChance slidingChance = i == 0 ?
                    this : defaults;

            if (slidingChance == null) continue;
            result = getSlidingChance2(mobLevel, slidingChance);
            if (result != null) break;
        }

        return result != null ?
                result : 0.0f;
    }

    private @Nullable Float getSlidingChance2(final int mobLevel,
            final @NotNull SlidingChance slidingChance){
        this.lastMatchedTier = null;
        if (slidingChance.changeRange == null || slidingChance.changeRange.isEmpty())
            return slidingChance.chance;

        for (final MinAndMax levelRanges : slidingChance.changeRange.keySet()){
            if (mobLevel >= levelRanges.min && mobLevel <= levelRanges.max){
                final MinAndMax assignments = slidingChance.changeRange.get(levelRanges);
                if (mobLevel == levelRanges.getMinAsInt()) {
                    this.lastMatchedTier = assignments;
                    this.lastResult = assignments.min;
                    return assignments.min;
                }
                if (mobLevel == levelRanges.getMaxAsInt()) {
                    this.lastMatchedTier = assignments;
                    this.lastResult = assignments.max;
                    return assignments.max;
                }
                return calculateChanceFromRange(mobLevel, levelRanges, assignments);
            }
        }

        return null;
    }

    private float calculateChanceFromRange(final int mobLevel,
                                          final @NotNull MinAndMax levelRanges,
                                          final @NotNull MinAndMax assignments){
        this.lastMatchedTier = assignments;
        if (assignments.min == assignments.max){
            this.lastResult = assignments.min;
            return assignments.min;
        }

        final float levelsRangePercent = 1.0f - ((levelRanges.max - mobLevel) / levelRanges.max);
        final float assignmentsDiff = assignments.max - assignments.min;
        final float result = levelsRangePercent * assignmentsDiff + assignments.min;
        this.lastResult = result;

        return result;
    }

    public void setFromInstance(final @Nullable SlidingChance slidingChance){
        if (slidingChance == null) return;
        final SlidingChance copy = (SlidingChance) slidingChance.clone();
        this.chance = copy.chance;
        this.changeRange = copy.changeRange;
    }

    public String showMatchedChance(){
        if (this.lastMatchedTier != null) {
            if (this.lastMatchedTier.min == this.lastMatchedTier.max) {
                return String.valueOf(this.lastResult);
            }
            else {
                return this.lastMatchedTier + ": " + this.lastResult;
            }
        }
        else {
            return String.valueOf(this.chance);
        }
    }

    public Object clone(){
        SlidingChance copy = null;
        try {
            copy = (SlidingChance) super.clone();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return copy;
    }

    public String toString(){
        if (changeRange == null || changeRange.isEmpty())
            return String.valueOf(this.chance);
        else
            return changeRange.toString();
    }
}
