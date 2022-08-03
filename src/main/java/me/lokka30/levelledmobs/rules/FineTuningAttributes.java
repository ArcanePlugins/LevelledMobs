/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

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
    public boolean doNotMerge;

    void mergeAttributes(final @Nullable FineTuningAttributes attributes) {
        if (attributes == null) {
            return;
        }

        try {
            for (final Field f : attributes.getClass().getDeclaredFields()) {
                if (!Modifier.isPublic(f.getModifiers())) {
                    continue;
                }
                if (f.get(attributes) == null) {
                    continue;
                }
                final Object presetValue = f.get(attributes);

                this.getClass().getDeclaredField(f.getName()).set(this, presetValue);
            }
        } catch (final IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final List<String> list = new LinkedList<>();
        if (maxHealth != null) {
            list.add("maxHlth: " + maxHealth);
        }
        if (attackDamage != null) {
            list.add("attkDmg: " + attackDamage);
        }
        if (itemDrop != null) {
            list.add("itemDrp: " + itemDrop);
        }
        if (xpDrop != null) {
            list.add("xpDrp: " + xpDrop);
        }
        if (movementSpeed != null) {
            list.add("moveSpd: " + movementSpeed);
        }
        if (rangedAttackDamage != null) {
            list.add("rangdAtkDmg: " + rangedAttackDamage);
        }
        if (creeperExplosionRadius != null) {
            list.add("creeperDmg: " + creeperExplosionRadius);
        }
        if (armorBonus != null) {
            list.add("armrBns: " + armorBonus);
        }
        if (armorToughness != null) {
            list.add("armrTuf: " + armorToughness);
        }
        if (attackKnockback != null) {
            list.add("attkKnbk: " + attackKnockback);
        }
        if (flyingSpeed != null) {
            list.add("flySpd: " + flyingSpeed);
        }
        if (knockbackResistance != null) {
            list.add("knbkRst: " + knockbackResistance);
        }
        if (horseJumpStrength != null) {
            list.add("horseJump: " + horseJumpStrength);
        }
        if (zombieReinforcements != null) {
            list.add("zmbRnfrce: " + zombieReinforcements);
        }
        if (followRange != null) {
            list.add("flwRng: " + followRange);
        }

        for (final String item : list) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(item);
        }

        if (sb.length() == 0) {
            return "No items";
        } else {
            return sb.toString();
        }
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
