package io.github.arcaneplugins.levelledmobs.rules.strategies

import java.util.concurrent.ThreadLocalRandom
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Holds the configuration and logic for applying a levelling system that is based upon the distance
 * y level height
 *
 * @author stumper66
 * @since 3.0.0
 */
class YDistanceStrategy : LevellingStrategy, Cloneable {
    var startingYLevel: Int? = null
    var endingYLevel: Int? = null
    var yPeriod: Int? = null

    override val strategyType = StrategyType.Y_COORDINATE

    override fun mergeRule(levellingStrategy: LevellingStrategy?) {
        if (levellingStrategy is YDistanceStrategy) {
            mergeYDistanceStrategy(levellingStrategy as YDistanceStrategy?)
        }
    }

    private fun mergeYDistanceStrategy(yds: YDistanceStrategy?) {
        if (yds == null) {
            return
        }

        if (yds.startingYLevel != null) {
            this.startingYLevel = yds.startingYLevel
        }
        if (yds.endingYLevel != null) {
            this.endingYLevel = yds.endingYLevel
        }
        if (yds.yPeriod != null) {
            this.yPeriod = yds.yPeriod
        }
    }

    override fun toString(): String {
        return String.format(
            "y coord, start: %s, end: %s, yPeriod: %s",
            if (startingYLevel == null) 0 else startingYLevel,
            if (endingYLevel == null) 0 else endingYLevel,
            if (yPeriod == null) 0 else yPeriod
        )
    }

    override fun generateNumber(
        lmEntity: LivingEntityWrapper,
        minLevel: Int,
        maxLevel: Int
    ): Float {
        var mobYLocation = lmEntity.livingEntity.location.blockY
        val yStart = if (this.startingYLevel == null) 0 else startingYLevel!!
        val yEnd = if (this.endingYLevel == null) 0 else endingYLevel!!
        val yPeriod = if (this.yPeriod == null) 0.0 else yPeriod!!.toDouble()
        val useLevel: Float
        val diff = (yEnd - yStart).toDouble()
        val isDecending = (yStart > yEnd)

        // make sure the mob location isn't past the end or start
        if (isDecending && mobYLocation < yEnd)
            mobYLocation = yEnd
        else if (!isDecending && mobYLocation > yEnd)
            mobYLocation = yStart

        if (yPeriod != 0.0) {
            val lvlPerPeriod = (maxLevel - minLevel) / (diff / yPeriod)
            useLevel = floor(
                minLevel + (lvlPerPeriod * (mobYLocation - yStart) / yPeriod)
            ).toFloat()
        } else {
            val useMobYLocation = (mobYLocation - yStart).toDouble()
            val percent = useMobYLocation / diff
            useLevel = minLevel + ceil((maxLevel - minLevel) * percent).toFloat()
        }

        return useLevel
    }

    private fun getVariance(
        lmEntity: LivingEntityWrapper,
        isAtMaxLevel: Boolean
    ): Int {
        val variance = LevelledMobs.instance.rulesManager.getRuleMaxRandomVariance(
            lmEntity
        )
        if (variance == null || variance == 0) {
            return 0
        }

        val change = ThreadLocalRandom.current().nextInt(0, variance + 1)

        // Start variation. First check if variation is positive or negative towards the original level amount.
        return if (!isAtMaxLevel || ThreadLocalRandom.current().nextBoolean()) {
            // Positive. Add the variation to the final level
            change
        } else {
            // Negative. Subtract the variation from the final level
            -change
        }
    }

    override fun cloneItem(): YDistanceStrategy {
        var copy: YDistanceStrategy? = null
        try {
            copy = super.clone() as YDistanceStrategy
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy!!
    }
}