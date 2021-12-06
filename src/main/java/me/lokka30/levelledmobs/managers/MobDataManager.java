/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages data related to various mob levelling
 *
 * @author lokka30, stumper66
 * @since 2.6.0
 */
public class MobDataManager {

    private final LevelledMobs main;

    public MobDataManager(final LevelledMobs main) {
        this.main = main;
    }

    @Nullable
    private Object getAttributeDefaultValue(@NotNull final LivingEntityWrapper lmEntity, final Attribute attribute) {
        if (lmEntity.isMobTamed()){
            // if the tamed variant in the cfg then use it, otherwise check for untamed path
            final String tamedPath = "TAMED_" + lmEntity.getTypeName() + "." + attribute;
            if (main.attributesCfg.contains(tamedPath)) return main.attributesCfg.get(tamedPath);
        }

        final String path = lmEntity.getTypeName() + "." + attribute;

        return main.attributesCfg.contains(path) ?
                main.attributesCfg.get(path) :
                null;
    }

    final boolean isLevelledDropManaged(final EntityType entityType, @NotNull final Material material) {
        // Head drops
        if (material.toString().endsWith("_HEAD") || material.toString().endsWith("_SKULL")) {
            if (!main.helperSettings.getBoolean(main.settingsCfg, "mobs-multiply-head-drops"))
                return false;
        }

        // Check list
        return main.dropsCfg.getStringList(entityType.toString()).contains(material.toString());
    }

    void setAdditionsForLevel(@NotNull final LivingEntityWrapper lmEntity, final @NotNull Attribute attribute, final Addition addition) {
        final boolean useStaticValues = main.helperSettings.getBoolean(main.settingsCfg, "attributes-use-preset-base-values");
        final double defaultValue = useStaticValues ?
                (double) Objects.requireNonNull(getAttributeDefaultValue(lmEntity, attribute)) :
                Objects.requireNonNull(lmEntity.getLivingEntity().getAttribute(attribute)).getBaseValue();
        final double additionValue = getAdditionsForLevel(lmEntity, addition, defaultValue);

        final AttributeModifier mod = new AttributeModifier(attribute.name(), additionValue, AttributeModifier.Operation.ADD_NUMBER);
        final AttributeInstance attrib = lmEntity.getLivingEntity().getAttribute(attribute);

        if (attrib == null) return;

        double existingDamage = 0;
        if (attribute == Attribute.GENERIC_MAX_HEALTH && lmEntity.getLivingEntity().getAttribute(attribute) != null)
            existingDamage = Objects.requireNonNull(lmEntity.getLivingEntity().getAttribute(attribute)).getValue() - lmEntity.getLivingEntity().getHealth();

        if (attrib.getModifiers().size() > 0){
            final List<AttributeModifier> existingMods = new ArrayList<>(attrib.getModifiers().size());
            existingMods.addAll(attrib.getModifiers());

            for (final AttributeModifier existingMod : existingMods)
                attrib.removeModifier(existingMod);
        }

        if (additionValue == 0.0) return;

        if (useStaticValues) {
            Utils.debugLog(main, DebugType.ATTRIBUTE_MULTIPLIERS, String.format("%s (%s): attrib: %s, base: %s, new base value: %s",
                    lmEntity.getNameIfBaby(), lmEntity.getMobLevel(), attribute.name(), Utils.round(attrib.getBaseValue(), 3), Utils.round(defaultValue, 3)));
            attrib.setBaseValue(defaultValue);
        }
        else {
            Utils.debugLog(main, DebugType.ATTRIBUTE_MULTIPLIERS, String.format("%s (%s): attrib: %s, base: %s, addtion: %s",
                    lmEntity.getNameIfBaby(), lmEntity.getMobLevel(), attribute.name(), Utils.round(attrib.getBaseValue(), 3), Utils.round(additionValue, 3)));
            attrib.addModifier(mod);
        }

        // MAX_HEALTH specific: set health to max health
        if (attribute == Attribute.GENERIC_MAX_HEALTH) {
            double newHealth = attrib.getValue() - existingDamage;
            if (newHealth < 0.0) newHealth = 0.0;
            try {
                if (lmEntity.getLivingEntity().getHealth() <= 0.0) return;
                lmEntity.getLivingEntity().setHealth(newHealth);
            } catch (final IllegalArgumentException ignored) { }
        }
    }

    public final double getAdditionsForLevel(final LivingEntityWrapper lmEntity, final Addition addition, final double defaultValue) {
        final double maxLevel = main.rulesManager.getRule_MobMaxLevel(lmEntity);

        double attributeValue = 0;
        double attributeMax = 0;

        if (lmEntity.getFineTuningAttributes() != null){
            switch (addition){
                case CUSTOM_XP_DROP:
                    if (lmEntity.getFineTuningAttributes().xpDrop != null) attributeValue = lmEntity.getFineTuningAttributes().xpDrop;
                    break;
                case CUSTOM_ITEM_DROP:
                    if (lmEntity.getFineTuningAttributes().itemDrop != null) attributeValue = lmEntity.getFineTuningAttributes().itemDrop;
                    break;
                case ATTRIBUTE_MAX_HEALTH:
                    if (lmEntity.getFineTuningAttributes().maxHealth != null) attributeValue = lmEntity.getFineTuningAttributes().maxHealth;
                    break;
                case ATTRIBUTE_ATTACK_DAMAGE:
                    if (lmEntity.getFineTuningAttributes().attackDamage != null) attributeValue = lmEntity.getFineTuningAttributes().attackDamage;
                    break;
                case ATTRIBUTE_MOVEMENT_SPEED:
                    if (lmEntity.getFineTuningAttributes().movementSpeed != null) attributeValue = lmEntity.getFineTuningAttributes().movementSpeed;
                    break;
                case CUSTOM_RANGED_ATTACK_DAMAGE:
                    if (lmEntity.getFineTuningAttributes().rangedAttackDamage != null) attributeValue = lmEntity.getFineTuningAttributes().rangedAttackDamage;
                    break;
                case CREEPER_BLAST_DAMAGE:
                    if (lmEntity.getFineTuningAttributes().creeperExplosionRadius != null) attributeValue = lmEntity.getFineTuningAttributes().creeperExplosionRadius;
                    break;
                case ATTRIBUTE_HORSE_JUMP_STRENGTH:
                    if (lmEntity.getFineTuningAttributes().horseJumpStrength != null) attributeValue = lmEntity.getFineTuningAttributes().horseJumpStrength;
                    break;
                case ATTRIBUTE_ARMOR_BONUS:
                    attributeMax = 30.0;
                    if (lmEntity.getFineTuningAttributes().armorBonus != null) attributeValue = lmEntity.getFineTuningAttributes().armorBonus;
                    break;
                case ATTRIBUTE_ARMOR_TOUGHNESS:
                    attributeMax = 50.0;
                    if (lmEntity.getFineTuningAttributes().armorToughness != null) attributeValue = lmEntity.getFineTuningAttributes().armorToughness;
                    break;
                case ATTRIBUTE_ATTACK_KNOCKBACK:
                    attributeMax = 5.0;
                    if (lmEntity.getFineTuningAttributes().attackKnockback != null) attributeValue = lmEntity.getFineTuningAttributes().attackKnockback;
                    break;
                case ATTRIBUTE_FLYING_SPEED:
                    if (lmEntity.getFineTuningAttributes().flyingSpeed != null) attributeValue = lmEntity.getFineTuningAttributes().flyingSpeed;
                    break;
                case ATTRIBUTE_KNOCKBACK_RESISTANCE:
                    attributeMax = 1.0;
                    if (lmEntity.getFineTuningAttributes().knockbackResistance != null) attributeValue = lmEntity.getFineTuningAttributes().knockbackResistance;
                    break;
                case ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS:
                    attributeMax = 1.0;
                    if (lmEntity.getFineTuningAttributes().zombieReinforcements != null) attributeValue = lmEntity.getFineTuningAttributes().zombieReinforcements;
                    break;
                case ATTRIBUTE_FOLLOW_RANGE:
                    if (lmEntity.getFineTuningAttributes().followRange != null) attributeValue = lmEntity.getFineTuningAttributes().followRange;
                    break;
            }
        }

        if (maxLevel == 0) return 0.0;

        // only used for 5 specific attributes
        if (attributeMax > 0.0)
            return (lmEntity.getMobLevel() / maxLevel) * (attributeMax * attributeValue);
        else
            // normal formula for most attributes
            return (defaultValue * attributeValue) * ((lmEntity.getMobLevel()) / maxLevel);
    }
}
