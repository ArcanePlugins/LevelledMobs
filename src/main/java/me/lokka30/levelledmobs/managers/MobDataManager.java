/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import static me.lokka30.levelledmobs.misc.DebugType.ATTRIBUTE_MULTIPLIERS;
import static me.lokka30.levelledmobs.util.Utils.debugLog;

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
        final float defaultValue = (float) Objects.requireNonNull(lmEntity.getLivingEntity()
            .getAttribute(attribute)).getBaseValue();
        final float additionValue = getAdditionsForLevel(lmEntity, addition, defaultValue);

        if (additionValue == 0.0f) {
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
                debugLog(main, DebugType.MULTIPLIER_REMOVED, String.format(
                        "Removing %s from (lvl %s) %s at %s,%s,%s", existingMod.getName(), lmEntity.getMobLevel(), lmEntity.getNameIfBaby(),
                        lmEntity.getLocation().getBlockX(), lmEntity.getLocation().getBlockY(), lmEntity.getLocation().getBlockZ()));
            }

            attrib.removeModifier(existingMod);
        }
        debugLog(main, ATTRIBUTE_MULTIPLIERS,
            String.format("%s (%s): attrib: %s, base: %s, addtion: %s",
                lmEntity.getNameIfBaby(), lmEntity.getMobLevel(), attribute.name(),
                Utils.round(attrib.getBaseValue(), 3), Utils.round(additionValue, 3)));
        attrib.addModifier(mod);


        // MAX_HEALTH specific: set health to max health
        if (attribute == Attribute.GENERIC_MAX_HEALTH) {
            try {
                if (lmEntity.getLivingEntity().getHealth() <= 0.0) return;
                lmEntity.getLivingEntity().setHealth(
                    Math.max(
                        0.0d,
                        attrib.getValue() - existingDamage
                    )
                );
            } catch (final IllegalArgumentException ignored) {}
        }
    }

    public final float getAdditionsForLevel(
        final LivingEntityWrapper lmEntity,
        final Addition addition,
        final float defaultValue
    ) {
        final float maxLevel = main.rulesManager.getRuleMobMaxLevel(lmEntity);

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

        if (maxLevel == 0 || multiplier == null || multiplier.value() == 0.0f) {
            debugLog(main, ATTRIBUTE_MULTIPLIERS, lmEntity.getNameIfBaby() +
                    ", maxLevel=0 / multiplier=null; returning 0 for " + addition);
            return 0.0f;
        }

        final float multiplierValue = multiplier.value();

        if (fineTuning.useStacked != null && fineTuning.useStacked || multiplier.useStacked()) {
            debugLog(main, ATTRIBUTE_MULTIPLIERS, multiplier +
                    ", using stacked formula");
            return (float) lmEntity.getMobLevel() * multiplierValue;
        } else {
            debugLog(main, ATTRIBUTE_MULTIPLIERS,  multiplier +
                    ", using standard formula");

            if (attributeMax > 0.0) {
                // only used for 5 specific attributes
                return (lmEntity.getMobLevel() / maxLevel) * (attributeMax * multiplierValue);
            } else {
                // normal formula for most attributes
                return (defaultValue * multiplierValue) * ((lmEntity.getMobLevel()) / maxLevel);
            }
        }
    }
}
