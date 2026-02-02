package io.github.arcaneplugins.levelledmobs.rules.strategies

import java.util.concurrent.ThreadLocalRandom
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
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
    var increasePerLevel: Float? = null

    override val strategyType = StrategyType.Y_COORDINATE
    override var shouldMerge: Boolean = false

    override fun mergeRule(levellingStrategy: LevellingStrategy?) {
        if (levellingStrategy is YDistanceStrategy)
            mergeYDistanceStrategy(levellingStrategy as YDistanceStrategy?)
    }

    private fun mergeYDistanceStrategy(yds: YDistanceStrategy?) {
        if (yds == null) return

        if (yds.startingYLevel != null)
            this.startingYLevel = yds.startingYLevel

        if (yds.endingYLevel != null)
            this.endingYLevel = yds.endingYLevel

        if (yds.yPeriod != null)
            this.yPeriod = yds.yPeriod

        if (yds.increasePerLevel != null)
            this.increasePerLevel = yds.increasePerLevel
    }

    override fun toString(): String {
        return String.format(
            "y coord, start: %s, end: %s, yPeriod: %s, increasePerLvl: %s",
            if (startingYLevel == null) 0 else startingYLevel,
            if (endingYLevel == null) 0 else endingYLevel,
            if (yPeriod == null) 0 else yPeriod,
            if (increasePerLevel == null) 0 else increasePerLevel
        )
    }

    override fun generateNumber(
        lmEntity: LivingEntityWrapper,
        minLevel: Int,
        maxLevel: Int
    ): Float {
        var mobYLocation = lmEntity.livingEntity.location.blockY.toFloat()
        val yStart = if (this.startingYLevel == null) 0f else startingYLevel!!.toFloat()
        val yEnd = if (this.endingYLevel == null) 0f else endingYLevel!!.toFloat()
        val yPeriod = if (this.yPeriod == null) 0f else yPeriod!!.toFloat()
        val useLevel: Float
        val isDescending = (yStart > yEnd)

        val highest = if (isDescending) yStart else yEnd
        val lowest = if (isDescending) yEnd else yStart
        val diff = if (isDescending) yStart - yEnd else yEnd - yStart

        // make sure the mob location isn't past the end or start
        if (isDescending && yPeriod == 0f && increasePerLevel == null)
            mobYLocation = mobYLocation.coerceAtMost(highest)

        if (yPeriod == 0f && increasePerLevel == null)
            mobYLocation = mobYLocation.coerceAtLeast(lowest)

        val distanceBelow = if (isDescending)
            highest - mobYLocation
        else
            mobYLocation - lowest

        if (increasePerLevel != null){
            val firstStep = if (isDescending)
                yStart - mobYLocation
            else
                mobYLocation - yStart

            useLevel = floor(firstStep / increasePerLevel!!)
        }
        else if (yPeriod != 0f) {
            val lvlPerPeriod = (maxLevel - minLevel) / yPeriod
            val periodBelow = distanceBelow / yPeriod
            useLevel = minLevel + (lvlPerPeriod * periodBelow)
        } else {
            val useMobYLocation = distanceBelow.toDouble()
            val percent = (useMobYLocation / diff).toFloat()
            useLevel = minLevel + ((maxLevel - minLevel) * percent)
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
        if (variance == null || variance == 0)
            return 0

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