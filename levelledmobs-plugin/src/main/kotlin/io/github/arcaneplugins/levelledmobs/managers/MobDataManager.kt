package io.github.arcaneplugins.levelledmobs.managers

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.enums.VanillaBonusEnum
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.result.MultiplierResult
import io.github.arcaneplugins.levelledmobs.rules.FineTuningAttributes
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import java.util.Collections
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.EntityType
import redempt.crunch.Crunch
import redempt.crunch.functional.EvaluationEnvironment
import kotlin.math.max
import kotlin.math.min

/**
 * Manages data related to various mob levelling
 *
 * @author lokka30, stumper66
 * @since 2.6.0
 */
class MobDataManager {
    val vanillaMultiplierNames = mutableMapOf<String, VanillaBonusEnum>()

    companion object {
        private val crunchEvalEnv = EvaluationEnvironment()

        fun evaluateExpression(
            expression: String
        ): Double {
            return Crunch.compileExpression(
                expression, crunchEvalEnv
            ).evaluate()
        }
    }

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
        /*
        Syntax: between(num, lower, upper)

        Returns `1` if `num` is between (inclusive) `lower` and `upper`, else, returning `0`.

        Example: between(7, 1, 5) -> 0
            (7 is not between 1 and 5 (inclusive), so 0 (false) was returned.)
         */
        crunchEvalEnv.addFunction("between", 3) { d: DoubleArray ->
            if ((d[0] >= d[1] && d[0] <= d[2])
            ) 1.0 else 0.0
        }

        /*
        Syntax: clamp(num, lower, upper)

        Returns `num` although increasing it / reducing it to be in between (inclusive) the
        `lower` and `upper` bounds.

        Example: clamp(7, 1, 5) -> 5
            (7 is larger than the upper bound of 5, so 5 was returned.)
         */
        crunchEvalEnv.addFunction("clamp", 3) { d: DoubleArray ->
            min(
                d[2],
                max(d[0], d[1])
            )
        }
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
        val multiplierResult = getAdditionsForLevel(lmEntity, addition, defaultValue)
        val additionValue = multiplierResult.amount
        if (additionValue == 0.0f) {
            return
        }

        val modifierOperation = if (multiplierResult.isAddition)
            AttributeModifier.Operation.ADD_NUMBER
        else
            AttributeModifier.Operation.MULTIPLY_SCALAR_1

        val mod = AttributeModifier(
            attribute.name, additionValue.toDouble(), modifierOperation
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
                if (allowedVanillaBonusEnums.isEmpty() || allowedVanillaBonusEnums.isIncludedInList(
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
                        lmEntity.getMobLevel,
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
            String.format(
                "%s (%s): attrib: %s, base: %s, addtion: %s",
                lmEntity.nameIfBaby,
                lmEntity.getMobLevel,
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
            } catch (ignored: IllegalArgumentException) {}
        }
    }

    fun getAdditionsForLevel(
        lmEntity: LivingEntityWrapper,
        addition: Addition,
        defaultValue: Float
    ): MultiplierResult {
        val maxLevel = LevelledMobs.instance.rulesManager.getRuleMobMaxLevel(lmEntity).toFloat()
        val fineTuning = lmEntity.fineTuningAttributes
        var multiplier: FineTuningAttributes.Multiplier? = null
        var attributeMax = 0f
        var multiplierValue = 0f
        var isAddition = true

        if (fineTuning != null) {
            multiplier = fineTuning.getItem(addition)
            if (multiplier?.hasFormula == true){
                isAddition = multiplier.isAddition
                val formulaStr = LevelledMobs.instance.levelManager.replaceStringPlaceholders(
                    multiplier.formula!!,
                    lmEntity,
                    true,
                    null,
                    true
                )
                try{ multiplierValue = evaluateExpression(formulaStr).toFloat() }
                catch (e: Exception){
                    Log.war("Error evaluating formula: '$formulaStr', ${e.message}")
                }
                DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity) {
                    "%${lmEntity.nameIfBaby} (${lmEntity.getMobLevel}):formula: '${multiplier.formula}', result: '$multiplierValue'" }
            }
            else if (multiplier != null)
                multiplierValue = multiplier.value

            attributeMax = when (addition) {
                Addition.ATTRIBUTE_ARMOR_BONUS -> 30.0f
                Addition.ATTRIBUTE_ARMOR_TOUGHNESS -> 50.0f
                Addition.ATTRIBUTE_ATTACK_KNOCKBACK -> 5.0f
                Addition.ATTRIBUTE_KNOCKBACK_RESISTANCE, Addition.ATTRIBUTE_ZOMBIE_SPAWN_REINFORCEMENTS -> 1.0f
                else -> 0.0f
            }
        }

        if (maxLevel == 0f || multiplierValue == 0.0f) {
            DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity) {
                lmEntity.nameIfBaby +
                        ", maxLevel was 0 or multiplier was 0; returning 0 for " + addition
            }
            return MultiplierResult(0.0f, isAddition)
        }

        if (multiplier?.hasFormula == true)
            return MultiplierResult(multiplierValue, isAddition)

        //val multiplierValue: Float = multiplier.value
        if ((addition == Addition.CUSTOM_ITEM_DROP || addition == Addition.CUSTOM_XP_DROP)
            && multiplierValue == -1f
        ) return MultiplierResult(Float.MIN_VALUE, isAddition)

        if (fineTuning!!.getUseStacked() || multiplier!!.useStacked) {
            DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity) {
                String.format(
                    "%s (%s): using stacked formula, multiplier: %s",
                    lmEntity.nameIfBaby, lmEntity.getMobLevel, multiplier!!.value
                )
            }
            return MultiplierResult(lmEntity.getMobLevel.toFloat() * multiplierValue, isAddition)
        } else {
            DebugManager.log(DebugType.APPLY_MULTIPLIERS, lmEntity) {
                String.format(
                    "%s (%s): using standard formula, multiplier: %s",
                    lmEntity.nameIfBaby, lmEntity.getMobLevel, multiplier.value
                )
            }

            multiplierValue = if (attributeMax > 0.0) {
                // only used for 5 specific attributes
                lmEntity.getMobLevel / maxLevel * (attributeMax * multiplierValue)
            } else {
                // normal formula for most attributes
                defaultValue * multiplierValue * ((lmEntity.getMobLevel) / maxLevel)
            }
            return MultiplierResult(multiplierValue, isAddition)
        }
    }
}