/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Holds any custom multipliers values parsed from rules.yml
 *
 * @author stumper66
 * @since 3.0.0
 */
public class FineTuningAttributes implements Cloneable {
    public FineTuningAttributes(){
        this.shortNameMappings = Map.ofEntries(
                Map.entry("maxHealth", "maxHlth"),
                Map.entry("attackDamage", "attkDamage"),
                Map.entry("itemDrop", "itemDrp"),
                Map.entry("xpDrop", "xpDrp"),
                Map.entry("movementSpeed", "moveSpd"),
                Map.entry("rangedAttackDamage", "rangdAtkDmg"),
                Map.entry("creeperExplosionRadius", "creeperDmg"),
                Map.entry("armorBonus", "armorBns"),
                Map.entry("armorToughness", "armorTfs"),
                Map.entry("attackKnockback", "attackKb"),
                Map.entry("flyingSpeed", "flyingSpd"),
                Map.entry("knockbackResistance", "kbResist"),
                Map.entry("horseJumpStrength", "horseJump"),
                Map.entry("zombieReinforcements", "zombieRe")
        );
    }

    private final Map<String, String> shortNameMappings;
    public EntityType applicableEntity;
    public Double attackDamage;
    public Double creeperExplosionRadius;
    public Double maxHealth;
    public Double movementSpeed;
    public Double rangedAttackDamage;
    public Integer itemDrop;
    public Double armorBonus;
    public Double armorToughness;
    public Double attackKnockback;
    public Double attackSpeed;
    public Double flyingSpeed;
    public Double knockbackResistance;
    public Double luck;
    public Double horseJumpStrength;
    public Double zombieReinforcements;
    public Integer xpDrop;

    public void mergeAttributes(final @Nullable FineTuningAttributes attributes){
        if (attributes == null) return;

        try {
            for (final Field f : attributes.getClass().getDeclaredFields()) {
                if (!Modifier.isPublic(f.getModifiers())) continue;
                if (f.getName().equals("applicableEntity")) continue;

                final Object presetValue = f.get(attributes);
                if (presetValue == null) continue;

                this.getClass().getDeclaredField(f.getName()).set(this, presetValue);
            }
        }
        catch (IllegalAccessException | NoSuchFieldException e){
            e.printStackTrace();
        }
    }

    public String toString(){
        final StringBuilder sb = new StringBuilder();

        try {
            for (final Field f : this.getClass().getDeclaredFields()) {
                if (!Modifier.isPublic(f.getModifiers())) continue;
                if (f.getName().equals("applicableEntity")) continue;

                final Object presetValue = f.get(this);
                if (presetValue == null) continue;

                if (sb.length() > 0) sb.append(", ");
                if (this.shortNameMappings.containsKey(f.getName()))
                    sb.append(this.shortNameMappings.get(f.getName()));
                else
                    sb.append(f.getName());

                sb.append(": ");
                sb.append(presetValue);
            }
        }
        catch (IllegalAccessException e){
            e.printStackTrace();
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return copy;
    }
}
