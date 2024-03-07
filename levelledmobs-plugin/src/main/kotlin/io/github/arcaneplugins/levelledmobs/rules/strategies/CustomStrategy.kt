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
    override val strategyType = StrategyType.CUSTOM

    override fun generateNumber(
        lmEntity: LivingEntityWrapper,
        minLevel: Int,
        maxLevel: Int
    ): Float {
        if (formula.isNullOrEmpty()){
            DebugManager.log(DebugType.CUSTOM_STRATEGY) { "no formula supplied, using 1" }
            return 1f
        }

        val useFormula = LevelledMobs.instance.levelManager.replaceStringPlaceholders(
            formula!!, lmEntity, true, lmEntity.associatedPlayer, true
        )

        val result = MobDataManager.evaluateExpression(useFormula)

        DebugManager.log(DebugType.CUSTOM_STRATEGY) {
            "formulaPre: '$formula', formulaPost: '$useFormula', result: $result" }

        return result.toFloat()
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