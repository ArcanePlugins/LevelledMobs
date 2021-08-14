/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import me.lokka30.levelledmobs.misc.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.TreeSet;

/**
 * Base class for all custom drops including custom commands
 *
 * @author stumper66
 * @since 3.0.0
 */
public class CustomDropBase {

    public CustomDropBase(@NotNull final CustomDropsDefaults defaults){
        this.amount = defaults.amount;
    }

    int amount;
    int amountRangeMin;
    int amountRangeMax;
    boolean hasAmountRange;
    public int minLevel;
    public int maxLevel;
    public int priority;
    public int maxDropGroup;
    public double chance;
    public boolean playerCausedOnly;
    public boolean noSpawner;
    public String groupId;
    public final Set<String> excludedMobs = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    public int getAmount(){
        return this.amount;
    }

    public void setAmount(int amount){
        this.amount = amount;
        if (this.amount > 64) this.amount = 64;
        if (this.amount < 1) this.amount = 1;
        this.hasAmountRange = false;
    }

    public int getAmountRangeMin(){
        return this.amountRangeMin;
    }

    public int getAmountRangeMax(){
        return this.amountRangeMax;
    }

    public boolean getHasAmountRange(){
        return this.hasAmountRange;
    }

    public String getAmountAsString(){
        if (this.hasAmountRange)
            return String.format("%s-%s", this.amountRangeMin, this.amountRangeMax);
        else
            return String.valueOf(this.amount);
    }

    public boolean setAmountRangeFromString(final String numberOrNumberRange){
        if (numberOrNumberRange == null || numberOrNumberRange.isEmpty()) return false;

        if (!numberOrNumberRange.contains("-")){
            if (!Utils.isInteger(numberOrNumberRange)) return false;

            this.amount = Integer.parseInt(numberOrNumberRange);
            this.hasAmountRange = false;
            return true;
        }

        final String[] nums = numberOrNumberRange.split("-");
        if (nums.length != 2) return false;

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())) return false;
        this.amountRangeMin = Integer.parseInt(nums[0].trim());
        this.amountRangeMax = Integer.parseInt(nums[1].trim());
        this.hasAmountRange = true;

        return true;
    }
}
