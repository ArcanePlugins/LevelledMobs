/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Holds any custom multipliers values parsed from rules.yml
 *
 * @author stumper66
 * @since 3.0.0
 */
public class FineTuningAttributes implements Cloneable {
    public Double attackDamage;
    public Double creeperExplosionRadius;
    public Double maxHealth;
    public Double movementSpeed;
    public Double rangedAttackDamage;
    public Integer itemDrop;
    public Double armorBonus;
    public Double armorToughness;
    public Double attackKnockback;
    public Double flyingSpeed;
    public Double knockbackResistance;
    public Double horseJumpStrength;
    public Double zombieReinforcements;
    public Double followRange;
    public Integer xpDrop;

    void mergeAttributes(final @Nullable FineTuningAttributes attributes){
        if (attributes == null) return;

        if (attributes.maxHealth != null) this.maxHealth = attributes.maxHealth;
        if (attributes.attackDamage != null) this.attackDamage = attributes.attackDamage;
        if (attributes.itemDrop != null) this.itemDrop = attributes.itemDrop;
        if (attributes.xpDrop != null) this.xpDrop = attributes.xpDrop;
        if (attributes.movementSpeed != null) this.movementSpeed = attributes.movementSpeed;
        if (attributes.rangedAttackDamage != null) this.rangedAttackDamage = attributes.rangedAttackDamage;
        if (attributes.creeperExplosionRadius != null) this.creeperExplosionRadius = attributes.creeperExplosionRadius;
    }

    public String toString(){
        final StringBuilder sb = new StringBuilder();
        final List<String> list = new LinkedList<>();
        if (maxHealth != null) list.add("maxHlth: " + maxHealth);
        if (attackDamage != null) list.add("attkDamage: " + attackDamage);
        if (itemDrop != null) list.add("itemDrp: " + itemDrop);
        if (xpDrop != null) list.add("xpDrp: " + xpDrop);
        if (movementSpeed != null) list.add("moveSpd: " + movementSpeed);
        if (rangedAttackDamage != null) list.add("rangdAtkDmg: " + rangedAttackDamage);
        if (creeperExplosionRadius != null) list.add("creeperDmg: " + creeperExplosionRadius);

        for (final String item : list){
            if (sb.length() > 0) sb.append(", ");
            sb.append(item);
        }

        if (sb.length() == 0)
            return "No items";
        else
            return sb.toString();
    }

    public FineTuningAttributes cloneItem() {
        FineTuningAttributes copy = null;
        try {
            copy = (FineTuningAttributes) super.clone();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return copy;
    }
}
