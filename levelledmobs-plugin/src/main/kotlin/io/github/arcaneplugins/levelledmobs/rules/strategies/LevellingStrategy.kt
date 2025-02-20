package io.github.arcaneplugins.levelledmobs.rules.strategies

import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper

/**
 * Interface for the various levelling systems
 *
 * @author lokka30
 * @since 3.0.0
 */
interface LevellingStrategy {
    fun generateNumber(
        lmEntity: LivingEntityWrapper,
        minLevel: Int,
        maxLevel: Int
    ): Float

    fun mergeRule(levellingStrategy: LevellingStrategy?)

    fun cloneItem(): LevellingStrategy

    val strategyType: StrategyType

    var shouldMerge: Boolean
}