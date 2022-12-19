/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.CachedModalList;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.rules.FineTuningAttributes;
import me.lokka30.levelledmobs.rules.VanillaBonusEnum;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages data related to various mob levelling
 *
 * @author lokka30, stumper66
 * @since 2.6.0
 */
public class MobDataManager {
    public MobDataManager(final LevelledMobs main) {
        this.main = main;
        this.vanillaMultiplierNames = Map.ofEntries(
                Map.entry("Armor modifier", VanillaBonusEnum.ARMOR_MODIFIER),
                Map.entry("Armor toughness", VanillaBonusEnum.ARMOR_TOUGHNESS),
                Map.entry("Attacking speed boost", VanillaBonusEnum.ATTACKING_SPEED_BOOST),
                Map.entry("Baby speed boost", VanillaBonusEnum.BABY_SPEED_BOOST),
                Map.entry("Covered armor bonus", VanillaBonusEnum.COVERED_ARMOR_BONUS),
                Map.entry("Drinking speed penalty", VanillaBonusEnum.DRINKING_SPEED_PENALTY),
                Map.entry("Fleeing speed boost", VanillaBonusEnum.FLEEING_SPEED_BOOST),
                Map.entry("Horse armor bonus", VanillaBonusEnum.HORSE_ARMOR_BONUS),
                Map.entry("Knockback resistance", VanillaBonusEnum.KNOCKBACK_RESISTANCE),
                Map.entry("Leader zombie bonus", VanillaBonusEnum.LEADER_ZOMBIE_BONUS),
                Map.entry("Random spawn bonus", VanillaBonusEnum.RANDOM_SPAWN_BONUS),
                Map.entry("Random zombie-spawn bonus", VanillaBonusEnum.RANDOM_ZOMBIE_SPAWN_BONUS),
                Map.entry("Sprinting speed boost", VanillaBonusEnum.SPRINTING_SPEED_BOOST),
                Map.entry("Tool modifier", VanillaBonusEnum.TOOL_MODIFIER),
                Map.entry("Weapon modifier", VanillaBonusEnum.WEAPON_MODIFIER),
                Map.entry("Zombie reinforcement caller charge", VanillaBonusEnum.ZOMBIE_REINFORCE_CALLER),
                Map.entry("Zombie reinforcement callee charge", VanillaBonusEnum.ZOMBIE_REINFORCE_CALLEE)
        );
    }

    private final LevelledMobs main;
    public final Map<String, VanillaBonusEnum> vanillaMultiplierNames;

    @Nullable private Object getAttributeDefaultValue(@NotNull final LivingEntityWrapper lmEntity,
        final Attribute attribute) {
        if (lmEntity.isMobTamed()) {
            // if the tamed variant in the cfg then use it, otherwise check for untamed path
            final String tamedPath = "TAMED_" + lmEntity.getTypeName() + "." + attribute;
            if (main.attributesCfg.contains(tamedPath)) {
                return main.attributesCfg.get(tamedPath);
            }
        }

        final String path = lmEntity.getTypeName() + "." + attribute;

        return main.attributesCfg.contains(path) ?
            main.attributesCfg.get(path) :
            null;
    }

    final boolean isLevelledDropManaged(final EntityType entityType,
        @NotNull final Material material) {
        // Head drops
        if (material.toString().endsWith("_HEAD") || material.toString().endsWith("_SKULL")) {
            if (!main.helperSettings.getBoolean(main.settingsCfg, "mobs-multiply-head-drops")) {
                return false;
            }
        }

        // Check list
        return main.dropsCfg.getStringList(entityType.toString()).contains(material.toString());
    }

    void setAdditionsForLevel(@NotNull final LivingEntityWrapper lmEntity,
        final @NotNull Attribute attribute, final Addition addition) {
        final boolean useStaticValues = main.helperSettings.getBoolean(main.settingsCfg,
            "attributes-use-preset-base-values");
        final float defaultValue = useStaticValues ?
            (float) Objects.requireNonNull(getAttributeDefaultValue(lmEntity, attribute)) :
                (float) Objects.requireNonNull(lmEntity.getLivingEntity().getAttribute(attribute))
                .getBaseValue();
        final double additionValue = getAdditionsForLevel(lmEntity, addition, defaultValue);

        if (additionValue == 0.0) {
            return;
        }

        final AttributeModifier mod = new AttributeModifier(attribute.name(), additionValue,
            AttributeModifier.Operation.ADD_NUMBER);
        final AttributeInstance attrib = lmEntity.getLivingEntity().getAttribute(attribute);

        if (attrib == null) {
            return;
        }

        // if zombified piglins get this attribute applied, they will spawn in zombies in the nether
        if (attribute == Attribute.ZOMBIE_SPAWN_REINFORCEMENTS
            && lmEntity.getEntityType() == EntityType.ZOMBIFIED_PIGLIN) {
            return;
        }

        double existingDamage = 0;
        if (attribute == Attribute.GENERIC_MAX_HEALTH
            && lmEntity.getLivingEntity().getAttribute(attribute) != null) {
            existingDamage =
                Objects.requireNonNull(lmEntity.getLivingEntity().getAttribute(attribute))
                    .getValue() - lmEntity.getLivingEntity().getHealth();
        }

        final CachedModalList<VanillaBonusEnum> allowedVanillaBonusEnums = main.rulesManager.getAllowedVanillaBonuses(lmEntity);
        final Enumeration<AttributeModifier> existingMods = Collections.enumeration(attrib.getModifiers());
        while (existingMods.hasMoreElements()){
            final AttributeModifier existingMod = existingMods.nextElement();
            final VanillaBonusEnum vanillaBonusEnum = this.vanillaMultiplierNames.get(existingMod.getName());
            if (vanillaBonusEnum != null){
                if (allowedVanillaBonusEnums.isEmpty() || allowedVanillaBonusEnums.isEnabledInList(vanillaBonusEnum, lmEntity)) {
                    continue;
                }
            }

            if (!existingMod.getName().startsWith("GENERIC_")) {
                Utils.debugLog(main, DebugType.MULTIPLIER_REMOVED, String.format(
                        "Removing %s from (lvl %s) %s at %s,%s,%s", existingMod.getName(), lmEntity.getMobLevel(), lmEntity.getNameIfBaby(),
                        lmEntity.getLocation().getBlockX(), lmEntity.getLocation().getBlockY(), lmEntity.getLocation().getBlockZ()));
            }

            attrib.removeModifier(existingMod);
        }

        if (useStaticValues) {
            Utils.debugLog(main, DebugType.ATTRIBUTE_MULTIPLIERS,
                String.format("%s (%s): attrib: %s, base: %s, new base value: %s",
                    lmEntity.getNameIfBaby(), lmEntity.getMobLevel(), attribute.name(),
                    Utils.round(attrib.getBaseValue(), 3), Utils.round(defaultValue, 3)));
            attrib.setBaseValue(defaultValue);
        } else {
            Utils.debugLog(main, DebugType.ATTRIBUTE_MULTIPLIERS,
                String.format("%s (%s): attrib: %s, base: %s, addtion: %s",
                    lmEntity.getNameIfBaby(), lmEntity.getMobLevel(), attribute.name(),
                    Utils.round(attrib.getBaseValue(), 3), Utils.round(additionValue, 3)));
            attrib.addModifier(mod);
        }

        // MAX_HEALTH specific: set health to max health
        if (attribute == Attribute.GENERIC_MAX_HEALTH) {
            double newHealth = attrib.getValue() - existingDamage;
            if (newHealth < 0.0) {
                newHealth = 0.0;
            }
            try {
                if (lmEntity.getLivingEntity().getHealth() <= 0.0) {
                    return;
                }
                lmEntity.getLivingEntity().setHealth(newHealth);
            } catch (final IllegalArgumentException ignored) {
            }
        }
    }

    public final float getAdditionsForLevel(final LivingEntityWrapper lmEntity,
        final Addition addition, final float defaultValue) {
        final float maxLevel = main.rulesManager.getRuleMobMaxLevel(lmEntity);

        //double attributeValue = 0;
        final FineTuningAttributes fineTuning = lmEntity.getFineTuningAttributes();
        FineTuningAttributes.Multiplier multiplier = null;
        float attributeMax = 0;

        if (fineTuning != null) {
            multiplier = fineTuning.getItem(addition);
            switch (addition){
                case ATTRIBUTE_ARMOR_BONUS -> attributeMax = 30.0f;
                case ATTRIBUTE_ARMOR_TOUGHNESS -> attributeMax = 50.0f;
                case ATTRIBUTE_ATTACK_KNOCKBACK -> attributeMax = 5.0f;
                case ATTRIBUTE_KNOCKBACK_RESISTANCE,
                     ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS -> attributeMax = 1.0f;
            }
        }

        if (maxLevel == 0 || multiplier == null) {
            return 0.0f;
        }

        final float multiplierValue = multiplier.value();

        if (lmEntity.getUseStackedMultipliers() || multiplier.useStacked()){
            return (float) lmEntity.getMobLevel() * multiplierValue;
        }
        else {
            // only used for 5 specific attributes
            if (attributeMax > 0.0) {
                return (lmEntity.getMobLevel() / maxLevel) * (attributeMax * multiplierValue);
            } else
            // normal formula for most attributes
            {
                return (defaultValue * multiplierValue) * ((lmEntity.getMobLevel()) / maxLevel);
            }
        }
    }
}
