package io.github.arcaneplugins.levelledmobs.rules.strategies

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.managers.MobDataManager
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper

/**
 * Allows custom levelling stratgies based on a supplied
 * formula
 *
 * @author stumper66
 * @since 4.0
 */
class CustomStrategy : LevellingStrategy, Cloneable {
    var formula: String? = null

    override fun generateLevel(
        lmEntity: LivingEntityWrapper?,
        minLevel: Int,
        maxLevel: Int
    ): Int {
        if (formula.isNullOrEmpty()){
            if (lmEntity != null)
                DebugManager.log(DebugType.CUSTOM_STRATEGY, lmEntity) { "no formula supplied, generating random level, min: $minLevel, max: $maxLevel" }
            else
                DebugManager.log(DebugType.CUSTOM_STRATEGY) { "no formula supplied, generating random level, min: $minLevel, max: $maxLevel" }
            return RandomLevellingStrategy().generateLevel(minLevel, maxLevel)
        }

        var useFormula = formula!!

        if (lmEntity != null) {
            useFormula = LevelledMobs.instance.levelManager.replaceStringPlaceholders(
                formula!!, lmEntity, true, lmEntity.associatedPlayer, true
            )
        }

        var result = MobDataManager.evaluateExpression(useFormula)
        result = result.coerceAtLeast(minLevel.toDouble())
        result = result.coerceAtMost(maxLevel.toDouble())
        val finalResult = Math.round(result).toInt()

        if (lmEntity != null)
            DebugManager.log(DebugType.CUSTOM_STRATEGY, lmEntity) {
                "formulaPre: '$formula', formulaPost: '$useFormula', maxlvl: $maxLevel, result: $finalResult" }
        else
            DebugManager.log(DebugType.CUSTOM_STRATEGY) {
                "formulaPre: '$formula', formulaPost: '$useFormula', maxlvl: $maxLevel, result: $finalResult" }

        return finalResult
    }

    override fun mergeRule(levellingStrategy: LevellingStrategy) {
        if (levellingStrategy !is CustomStrategy) return

        this.formula = levellingStrategy.formula
    }

    override fun cloneItem(): LevellingStrategy {
        var copy: CustomStrategy? = null
        try {
            copy = super.clone() as CustomStrategy
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy as LevellingStrategy
    }

    override fun toString(): String {
        return if (formula.isNullOrEmpty())
            "Custom Strategy"
        else
            "Custom Strategy: $formula"
    }
}