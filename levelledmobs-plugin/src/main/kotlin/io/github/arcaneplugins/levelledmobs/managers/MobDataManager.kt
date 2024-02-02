package io.github.arcaneplugins.levelledmobs.managers

import java.util.Collections
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.rules.FineTuningAttributes
import io.github.arcaneplugins.levelledmobs.enums.VanillaBonusEnum
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.EntityType
import kotlin.math.max

/**
 * Manages data related to various mob levelling
 *
 * @author lokka30, stumper66
 * @since 2.6.0
 */
class MobDataManager {
    val vanillaMultiplierNames = mutableMapOf<String, VanillaBonusEnum>()

    init {
        this.vanillaMultiplierNames.putAll(mapOf(
            "Armor modifier" to VanillaBonusEnum.ARMOR_MODIFIER,
            "Armor toughness" to VanillaBonusEnum.ARMOR_TOUGHNESS,
            "Attacking speed boost" to VanillaBonusEnum.ATTACKING_SPEED_BOOST,
            "Baby speed boost" to VanillaBonusEnum.BABY_SPEED_BOOST,
            "Covered armor bonus" to VanillaBonusEnum.COVERED_ARMOR_BONUS,
            "Drinking speed penalty" to VanillaBonusEnum.DRINKING_SPEED_PENALTY,
            "Fleeing speed boost" to VanillaBonusEnum.FLEEING_SPEED_BOOST,
            "Horse armor bonus" to VanillaBonusEnum.HORSE_ARMOR_BONUS,
            "Knockback resistance" to VanillaBonusEnum.KNOCKBACK_RESISTANCE,
            "Leader zombie bonus" to VanillaBonusEnum.LEADER_ZOMBIE_BONUS,
            "Random spawn bonus" to VanillaBonusEnum.RANDOM_SPAWN_BONUS,
            "Random zombie-spawn bonus" to VanillaBonusEnum.RANDOM_ZOMBIE_SPAWN_BONUS,
            "Sprinting speed boost" to VanillaBonusEnum.SPRINTING_SPEED_BOOST,
            "Tool modifier" to VanillaBonusEnum.TOOL_MODIFIER,
            "Weapon modifier" to VanillaBonusEnum.WEAPON_MODIFIER,
            "Zombie reinforcement caller charge" to VanillaBonusEnum.ZOMBIE_REINFORCE_CALLER,
            "Zombie reinforcement callee charge" to VanillaBonusEnum.ZOMBIE_REINFORCE_CALLEE
        ))
    }

    fun isLevelledDropManaged(
        entityType: EntityType,
        material: Material
    ): Boolean {
        // Head drops
        val main = LevelledMobs.instance
        if (material.toString().endsWith("_HEAD") || material.toString().endsWith("_SKULL")) {
            if (!main.helperSettings.getBoolean( "mobs-multiply-head-drops")) {
                return false
            }
        }

        // Check list
        return main.dropsCfg.getStringList(entityType.toString()).contains(material.toString())
    }

    fun setAdditionsForLevel(
        lmEntity: LivingEntityWrapper,
        attribute: Attribute,
        addition: Addition
    ) {
        val defaultValue = lmEntity.livingEntity
                .getAttribute(attribute)!!.baseValue.toFloat()
        val additionValue = getAdditionsForLevel(lmEntity, addition, defaultValue)

        if (additionValue == 0.0f) {
            return
        }

        val mod = AttributeModifier(
            attribute.name, additionValue.toDouble(),
            AttributeModifier.Operation.ADD_NUMBER
        )
        val attrib = lmEntity.livingEntity.getAttribute(attribute) ?: return

        // if zombified piglins get this attribute applied, they will spawn in zombies in the nether
        if (attribute == Attribute.ZOMBIE_SPAWN_REINFORCEMENTS
            && lmEntity.entityType == EntityType.ZOMBIFIED_PIGLIN
        ) {
            return
        }

        var existingDamage = 0.0
        if (attribute == Attribute.GENERIC_MAX_HEALTH
            && lmEntity.livingEntity.getAttribute(attribute) != null
        ) {
            existingDamage =
                lmEntity.livingEntity.getAttribute(attribute)!!.value - lmEntity.livingEntity.health
        }

        val allowedVanillaBonusEnums: CachedModalList<VanillaBonusEnum> =
            LevelledMobs.instance.rulesManager.getAllowedVanillaBonuses(lmEntity)
        val existingMods = Collections.enumeration(attrib.modifiers)
        while (existingMods.hasMoreElements()) {
            val existingMod = existingMods.nextElement()
            val vanillaBonusEnum = vanillaMultiplierNames[existingMod.name]
            if (vanillaBonusEnum != null) {
                if (allowedVanillaBonusEnums.isEmpty() || allowedVanillaBonusEnums.isEnabledInList(
                        vanillaBonusEnum,
                        lmEntity
                    )
                ) {
                    continue
                }
            }

            if (!existingMod.name.startsWith("GENERIC_")) {
                DebugManager.log(DebugType.REMOVED_MULTIPLIERS, lmEntity) {
                    String.format(
                        "Removing %s from (lvl %s) %s at %s,%s,%s",
                        existingMod.name,
                        lmEntity.getMobLevel(),
                        lmEntity.nameIfBaby,
                        lmEntity.location.blockX,
                        lmEntity.location.blockY,
                        lmEntity.location.blockZ
                    )
                }
            }

            attrib.removeModifier(existingMod)
        }
        DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity) {
            java.lang.String.format(
                "%s (%s): attrib: %s, base: %s, addtion: %s",
                lmEntity.nameIfBaby,
                lmEntity.getMobLevel(),
                attribute.name,
                Utils.round(attrib.baseValue, 3),
                Utils.round(additionValue.toDouble(), 3)
            )
        }
        attrib.addModifier(mod)


        // MAX_HEALTH specific: set health to max health
        if (attribute == Attribute.GENERIC_MAX_HEALTH) {
            try {
                if (lmEntity.livingEntity.health <= 0.0) return
                lmEntity.livingEntity.health = max(
                    1.0,
                    attrib.value - existingDamage
                )
            } catch (ignored: IllegalArgumentException) {
            }
        }
    }

    fun getAdditionsForLevel(
        lmEntity: LivingEntityWrapper,
        addition: Addition,
        defaultValue: Float
    ): Float {
        val maxLevel = LevelledMobs.instance.rulesManager.getRuleMobMaxLevel(lmEntity).toFloat()
        val fineTuning = lmEntity.getFineTuningAttributes()
        val multiplier: FineTuningAttributes.Multiplier?
        var attributeMax = 0f

        if (fineTuning != null) {
            multiplier = fineTuning.getItem(addition)

            attributeMax = when (addition) {
                Addition.ATTRIBUTE_ARMOR_BONUS -> 30.0f
                Addition.ATTRIBUTE_ARMOR_TOUGHNESS -> 50.0f
                Addition.ATTRIBUTE_ATTACK_KNOCKBACK -> 5.0f
                Addition.ATTRIBUTE_KNOCKBACK_RESISTANCE, Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS -> 1.0f
                else -> 0.0f
            }
        } else {
            multiplier = null
        }

        if (maxLevel == 0f || multiplier == null || multiplier.value == 0.0f) {
            DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity) {
                lmEntity.nameIfBaby +
                        ", maxLevel=0 / multiplier=null; returning 0 for " + addition
            }
            return 0.0f
        }

        val multiplierValue: Float = multiplier.value
        if ((addition == Addition.CUSTOM_ITEM_DROP || addition == Addition.CUSTOM_XP_DROP)
            && multiplierValue == -1f
        ) {
            return Float.MIN_VALUE
        }

        if (fineTuning!!.getUseStacked() || multiplier.useStacked) {
            DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity) {
                java.lang.String.format(
                    "%s (%s): using stacked formula, multiplier: %s",
                    lmEntity.nameIfBaby, lmEntity.getMobLevel(), multiplier.value
                )
            }
            return lmEntity.getMobLevel().toFloat() * multiplierValue
        } else {
            DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity) {
                java.lang.String.format(
                    "%s (%s): using standard formula, multiplier: %s",
                    lmEntity.nameIfBaby, lmEntity.getMobLevel(), multiplier.value
                )
            }

            return if (attributeMax > 0.0) {
                // only used for 5 specific attributes
                lmEntity.getMobLevel() / maxLevel * (attributeMax * multiplierValue)
            } else {
                // normal formula for most attributes
                defaultValue * multiplierValue * ((lmEntity.getMobLevel()) / maxLevel)
            }
        }
    }
}