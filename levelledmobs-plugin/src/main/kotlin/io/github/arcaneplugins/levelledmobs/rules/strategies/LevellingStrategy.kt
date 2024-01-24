package io.github.arcaneplugins.levelledmobs.rules.strategies

import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper

/**
 * Interface for the various levelling systems
 *
 * @author lokka30
 * @since 3.0.0
 */
interface LevellingStrategy {
    fun generateLevel(lmEntity: LivingEntityWrapper?, minLevel: Int, maxLevel: Int): Int

    fun mergeRule(levellingStrategy: LevellingStrategy)

    fun cloneItem(): LevellingStrategy?
}