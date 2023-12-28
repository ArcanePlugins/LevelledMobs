/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import java.util.LinkedHashMap;
import java.util.Map;

import me.lokka30.levelledmobs.misc.Addition;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds any custom multipliers values parsed from rules.yml
 *
 * @author stumper66
 * @since 3.0.0
 */
public class FineTuningAttributes implements MergableRule, Cloneable {
    public FineTuningAttributes(){
        this.multipliers = new LinkedHashMap<>();
    }

    private Map<Addition, Multiplier> multipliers;
    public boolean doNotMerge;
    public Boolean useStacked;

    public boolean getUseStacked(){
        return useStacked != null && useStacked;
    }

    public boolean isEmpty(){
        return this.multipliers.isEmpty();
    }

    public void merge(final @Nullable MergableRule mergableRule) {
        if (!(mergableRule instanceof final FineTuningAttributes attributes)) {
            return;
        }

        this.multipliers.putAll(attributes.copyMultipliers());
    }

    public void addItem(final Addition addition, final Multiplier multiplier){
        this.multipliers.put(addition, multiplier);
    }

    public boolean doMerge(){
        return !this.doNotMerge;
    }

    public @Nullable Multiplier getItem(final @NotNull Addition addition){
        return this.multipliers.get(addition);
    }

    static @NotNull String getShortName(final @NotNull Addition addition){
        switch (addition){
            case ATTRIBUTE_ATTACK_DAMAGE -> { return "attkDmg"; }
            case CREEPER_BLAST_DAMAGE -> { return "creeperDmg"; }
            case ATTRIBUTE_MAX_HEALTH -> { return "maxHlth"; }
            case ATTRIBUTE_MOVEMENT_SPEED -> { return "moveSpd"; }
            case CUSTOM_RANGED_ATTACK_DAMAGE -> { return "rangdAtkDmg"; }
            case CUSTOM_ITEM_DROP -> { return "itemDrp"; }
            case ATTRIBUTE_ARMOR_BONUS -> { return "armrBns"; }
            case ATTRIBUTE_ARMOR_TOUGHNESS -> { return "armrTuf"; }
            case ATTRIBUTE_ATTACK_KNOCKBACK -> { return "attkKnbk"; }
            case ATTRIBUTE_FLYING_SPEED -> { return "flySpd"; }
            case ATTRIBUTE_KNOCKBACK_RESISTANCE -> { return "knbkRst"; }
            case ATTRIBUTE_HORSE_JUMP_STRENGTH -> { return "horseJump"; }
            case ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS -> { return "zmbRnfrce"; }
            case ATTRIBUTE_FOLLOW_RANGE -> { return "flwRng"; }
            case CUSTOM_XP_DROP -> { return "xpDrp"; }
        }

        throw new UnsupportedOperationException("No short name was added for " + addition);
    }

    public @NotNull Addition getAdditionFromLMMultiplier(final @NotNull LMMultiplier lmMultiplier){
        switch (lmMultiplier){
            case ATTACK_DAMAGE -> { return Addition.ATTRIBUTE_ATTACK_DAMAGE; }
            case CREEPER_BLAST_DAMAGE -> { return Addition.CREEPER_BLAST_DAMAGE; }
            case MAX_HEALTH -> { return Addition.ATTRIBUTE_MAX_HEALTH; }
            case MOVEMENT_SPEED -> { return Addition.ATTRIBUTE_MOVEMENT_SPEED; }
            case RANGED_ATTACK_DAMAGE -> { return Addition.CUSTOM_RANGED_ATTACK_DAMAGE; }
            case ITEM_DROP -> { return Addition.CUSTOM_ITEM_DROP; }
            case ARMOR_BONUS -> { return Addition.ATTRIBUTE_ARMOR_BONUS; }
            case ARMOR_TOUGHNESS -> { return Addition.ATTRIBUTE_ARMOR_TOUGHNESS; }
            case ATTACK_KNOCKBACK -> { return Addition.ATTRIBUTE_ATTACK_KNOCKBACK; }
            case FLYING_SPEED -> { return Addition.ATTRIBUTE_FLYING_SPEED; }
            case KNOCKBACK_RESISTANCE -> { return Addition.ATTRIBUTE_KNOCKBACK_RESISTANCE; }
            case HORSE_JUMP_STRENGTH -> { return Addition.ATTRIBUTE_HORSE_JUMP_STRENGTH; }
            case ZOMBIE_SPAWN_REINFORCEMENTS -> { return Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS; }
            case FOLLOW_RANGE -> { return Addition.ATTRIBUTE_FOLLOW_RANGE; }
            case XP_DROP -> { return Addition.CUSTOM_XP_DROP; }
        }

        throw new UnsupportedOperationException("No addition mapping for: " + lmMultiplier);
    }

    public record Multiplier(Addition addition, boolean useStacked, float value){
        @Contract(pure = true)
        public @NotNull String toString(){
            final StringBuilder sb = new StringBuilder();
            sb.append(FineTuningAttributes.getShortName(addition()));
            if (useStacked()) sb.append(" (stkd): ");
            else sb.append(": ");
            sb.append(value());

            return sb.toString();
        }
    }

    public FineTuningAttributes cloneItem() {
        FineTuningAttributes copy = null;
        try {
            copy = (FineTuningAttributes) super.clone();
            copy.cloneMultipliers();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return copy;
    }

    private void cloneMultipliers(){
        final Map<Addition, Multiplier> copy = copyMultipliers();
        this.multipliers = new LinkedHashMap<>(copy.size());
        this.multipliers.putAll(copy);
    }

    private @NotNull Map<Addition, Multiplier> copyMultipliers(){
        final Map<Addition, Multiplier> copy = new LinkedHashMap<>(this.multipliers.size());

        for (final Addition addition : this.multipliers.keySet()){
            final Multiplier old = this.multipliers.get(addition);
            copy.put(addition, new Multiplier(addition, old.useStacked, old.value));
        }

        return copy;
    }

    public String toString() {
        if (this.isEmpty())
            return "No items";

        final StringBuilder sb = new StringBuilder();

        if (this.getUseStacked())
            sb.append("(all stk)");

        for (final Multiplier item : this.multipliers.values()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(getShortName(item.addition()));
            sb.append(": ");
            sb.append(item.value());
            if (item.useStacked()){
                sb.append(" (");
                sb.append("stk)");
            }
        }

        return sb.toString();
    }

}
