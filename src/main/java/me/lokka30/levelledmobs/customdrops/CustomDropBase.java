/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrops;

import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Base class for all custom drops including custom commands
 *
 * @author stumper66
 * @since 3.0.0
 */
public class CustomDropBase implements Cloneable {

    CustomDropBase(@NotNull final CustomDropsDefaults defaults) {
        this.amount = defaults.amount;
        this.permissions = new LinkedList<>();
    }

    private int amount;
    int amountRangeMin;
    private int amountRangeMax;
    private boolean hasAmountRange;
    public int minLevel;
    public int maxLevel;
    public int priority;
    int maxDropGroup;
    int minPlayerLevel;
    int maxPlayerLevel;
    public float chance;
    boolean playerCausedOnly;
    boolean noSpawner;
    String groupId;
    String playerLevelVariable;
    final public List<String> permissions;
    final Set<String> excludedMobs = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    CachedModalList<EntityDamageEvent.DamageCause> causeOfDeathReqs;

    public int getAmount(){
        return this.amount;
    }

    public void setAmount(final int amount){
        this.amount = amount;
        if (this.amount > 64) this.amount = 64;
        if (this.amount < 1) this.amount = 1;
        this.hasAmountRange = false;
    }

    int getAmountRangeMin(){
        return this.amountRangeMin;
    }

    int getAmountRangeMax(){
        return this.amountRangeMax;
    }

    boolean getHasAmountRange(){
        return this.hasAmountRange;
    }

    String getAmountAsString(){
        if (this.hasAmountRange)
            return String.format("%s-%s", this.amountRangeMin, this.amountRangeMax);
        else
            return String.valueOf(this.amount);
    }

    boolean setAmountRangeFromString(final String numberOrNumberRange){
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

    public CustomDropBase cloneItem() {
        CustomDropBase copy = null;
        try {
            copy = (CustomDropBase) super.clone();
            if (this.causeOfDeathReqs != null)
                copy.causeOfDeathReqs = (CachedModalList<EntityDamageEvent.DamageCause>) this.causeOfDeathReqs.clone();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return copy;
    }
}
