package io.github.arcaneplugins.levelledmobs.rules

import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import kotlin.math.ceil

/**
 * Holds any rule information regarding the health indicator
 *
 * @author stumper66
 * @since 3.1.0
 */
class HealthIndicator : MergableRule, Cloneable {
    var indicator: String? = null
    var indicatorHalf: String? = null
    var scale: Double? = null
    var maxIndicators: Int? = null
    var tiers: MutableMap<Int, String>? = null
    var merge: Boolean? = null
    var maintainSpace: Boolean? = null
    companion object{
        private const val SPACE = " "
    }

    override fun cloneItem(): Any {
        var copy: HealthIndicator? = null
        try {
            copy = super.clone() as HealthIndicator
            if (this.tiers != null) {
                copy.tiers = mutableMapOf()
                copy.tiers!!.putAll(tiers!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy as Any
    }

    class HealthIndicatorResult {
        constructor() {
            this.formattedString = ""
            this.colorOnly = ""
        }

        constructor(formattedString: String, colorOnly: String) {
            this.formattedString = formattedString
            this.colorOnly = colorOnly
        }

        val formattedString: String
        val colorOnly: String
    }

    fun formatHealthIndicator(lmEntity: LivingEntityWrapper): HealthIndicatorResult {
        val mobHealth = lmEntity.livingEntity.health

        if (mobHealth == 0.0) return HealthIndicatorResult()

        val maxIndicators = if (this.maxIndicators != null) maxIndicators!! else 10
        val indicatorStr = if (this.indicator != null) this.indicator else "▐"
        val scale = if (this.scale != null) scale!! else 5.0
        val shouldMaintainSpace = (maintainSpace != null && maintainSpace!!)

        var indicatorsToUse = if (scale == 0.0) ceil(mobHealth).toInt() else ceil(mobHealth / scale)
            .toInt()
        val tiersToUse = ceil(indicatorsToUse.toDouble() / maxIndicators.toDouble()).toInt()
        var toRecolor = 0
        if (tiersToUse > 0)
            toRecolor = indicatorsToUse % maxIndicators

        var primaryColor: String? = ""
        var secondaryColor: String? = ""

        if (this.tiers != null) {
            val useTiers = this.tiers!!

            if (useTiers.containsKey(tiersToUse))
                primaryColor = useTiers[tiersToUse]
            else if (useTiers.containsKey(0))
                primaryColor = useTiers[0]

            if (tiersToUse > 0 && useTiers.containsKey(tiersToUse - 1))
                secondaryColor = useTiers[tiersToUse - 1]
            else if (useTiers.containsKey(0))
                secondaryColor = useTiers[0]
        }

        val result = StringBuilder()
        result.append(primaryColor)

        if (tiersToUse < 2) {
            var indicatorsUsed = 0
            var useHalf = false
            if (this.indicatorHalf != null && indicatorsToUse < maxIndicators) {
                useHalf = scale / 2.0 <= (indicatorsToUse * scale) - mobHealth
                if (useHalf && indicatorsToUse > 0)
                    indicatorsToUse--
            }

            result.append(indicatorStr!!.repeat(indicatorsToUse))
            indicatorsUsed = indicatorsToUse
            if (useHalf) {
                result.append(this.indicatorHalf)
                indicatorsUsed++
            }

            if (shouldMaintainSpace && indicatorsUsed < maxIndicators) {
                val amountToAdd = maxIndicators - indicatorsUsed
                if (amountToAdd > 0)
                    result.append(SPACE.repeat(amountToAdd))
            }
        } else {
            if (toRecolor == 0)
                result.append(indicatorStr!!.repeat(maxIndicators))
            else {
                result.append(indicatorStr!!.repeat(toRecolor))
                result.append(secondaryColor)
                result.append(indicatorStr.repeat(maxIndicators - toRecolor))
            }
        }

        return HealthIndicatorResult(result.toString(), primaryColor!!)
    }

    override val doMerge: Boolean
        get() = this.merge != null && merge!!

    override fun merge(mergableRule: MergableRule?) {
        if (mergableRule !is HealthIndicator) return

        if (mergableRule.indicator != null)
            this.indicator = mergableRule.indicator
        if (mergableRule.indicatorHalf != null)
            this.indicatorHalf = mergableRule.indicatorHalf
        if (mergableRule.scale != null)
            this.scale = mergableRule.scale
        if (mergableRule.maxIndicators != null)
            this.maxIndicators = mergableRule.maxIndicators

        if (mergableRule.tiers == null) return

        if (this.tiers == null)
            this.tiers = mutableMapOf()

        tiers!!.putAll(mergableRule.tiers!!)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (indicator != null) {
            sb.append("ind: ")
            sb.append(indicator)
        }

        if (indicatorHalf != null) {
            if (sb.isNotEmpty()) sb.append(", ")

            sb.append("ind.5: ").append(indicatorHalf)
        }

        if (scale != null) {
            if (sb.isNotEmpty()) sb.append(", ")

            sb.append("scl: ").append(scale)
        }

        if (maxIndicators != null) {
            if (sb.isNotEmpty()) sb.append(", ")

            sb.append("max: ").append(maxIndicators)
        }

        if (tiers != null) {
            if (sb.isNotEmpty()) sb.append(", ")

            sb.append(tiers)
        }

        if (doMerge) {
            if (sb.isNotEmpty()) sb.append("&r, ")

            sb.append("merge: true")
        }

        return if (sb.isNotEmpty())
            sb.toString()
        else
            super.toString()
    }
}