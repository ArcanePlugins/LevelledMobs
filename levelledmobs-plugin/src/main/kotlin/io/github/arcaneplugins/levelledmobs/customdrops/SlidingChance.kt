package io.github.arcaneplugins.levelledmobs.customdrops

import io.github.arcaneplugins.levelledmobs.rules.MinAndMax

/**
 * Holds values that are used for a variable chance based on either
 * a float number or a defined set of tiers
 *
 * @author stumper66
 * @since 3.15.0
 */
class SlidingChance : Cloneable {
    var chance = 0f
    var changeRange: MutableMap<MinAndMax, MinAndMax>? = null
    private var lastMatchedTier: MinAndMax? = null
    private var lastResult = 0f
    var defaults: SlidingChance? = null

    val isDefault: Boolean
        get() = chance == 0.0f && (changeRange == null || changeRange!!.isEmpty())

    val isAssuredChance: Boolean
        get() = chance >= 1.0f && !isDefault

    fun getSlidingChance(
        mobLevel: Int
    ): Float {
        var result: Float? = null

        for (i in 0..1) {
            // first check with this class settings
            // if no tiers are matched then check against defaults
            val slidingChance = if (i == 0) this else defaults

            if (slidingChance == null) continue
            result = getSlidingChance2(mobLevel, slidingChance)
            if (result != null) break
        }

        return result ?: 0.0f
    }

    private fun getSlidingChance2(
        mobLevel: Int,
        slidingChance: SlidingChance
    ): Float? {
        this.lastMatchedTier = null
        if (slidingChance.changeRange == null || slidingChance.changeRange!!.isEmpty())
            return slidingChance.chance

        for (levelRanges in slidingChance.changeRange!!.keys) {
            if (mobLevel >= levelRanges.minAsInt && mobLevel <= levelRanges.maxAsInt) {
                val assignments = slidingChance.changeRange!![levelRanges]!!
                if (mobLevel == levelRanges.minAsInt) {
                    this.lastMatchedTier = assignments
                    this.lastResult = assignments.min
                    return assignments.min
                }
                if (mobLevel == levelRanges.maxAsInt) {
                    this.lastMatchedTier = assignments
                    this.lastResult = assignments.max
                    return assignments.max
                }
                return calculateChanceFromRange(mobLevel, levelRanges, assignments)
            }
        }

        return null
    }

    private fun calculateChanceFromRange(
        mobLevel: Int,
        levelRanges: MinAndMax,
        assignments: MinAndMax
    ): Float {
        this.lastMatchedTier = assignments
        if (assignments.min == assignments.max) {
            this.lastResult = assignments.min
            return assignments.min
        }

        val levelsRangePercent = 1.0f - ((levelRanges.max - mobLevel) / levelRanges.max)
        val assignmentsDiff = assignments.max - assignments.min
        val result = levelsRangePercent * assignmentsDiff + assignments.min
        this.lastResult = result

        return result
    }

    fun setFromInstance(
        slidingChance: SlidingChance?
    ) {
        if (slidingChance == null) return
        val copy = slidingChance.clone() as SlidingChance
        this.chance = copy.chance
        this.changeRange = copy.changeRange
    }

    fun showMatchedChance(): String {
        return if (this.lastMatchedTier != null) {
            if (lastMatchedTier!!.min == lastMatchedTier!!.max) {
                lastResult.toString()
            } else {
                "$lastMatchedTier: $lastResult"
            }
        } else {
            chance.toString()
        }
    }

    public override fun clone(): Any {
        var copy: SlidingChance? = null
        try {
            copy = super.clone() as SlidingChance
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy as Any
    }

    override fun toString(): String {
        return if (changeRange == null || changeRange!!.isEmpty())
            chance.toString()
        else
            changeRange.toString()
    }
}