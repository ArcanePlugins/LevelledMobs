package io.github.arcaneplugins.levelledmobs.rules

import java.util.Objects

/**
 * Holds any rule information relating to leveled tiers
 *
 * @author stumper66
 * @since 3.1.0
 */
class LevelTierMatching {
    var names: MutableList<String>? = null
    var valueRanges: MinAndMax? = null
    var sourceTierName: String? = null
    var minLevel: Float? = null
    var maxLevel: Float? = null
    var mobName: String? = null

    private val hasLevelRestriction: Boolean
        get () = minLevel != null || maxLevel != null

    fun isApplicableToMobLevel(mobLevel: Int): Boolean {
        if (!this.hasLevelRestriction) {
            return true
        }

        val meetsMin = minLevel == null || mobLevel >= minLevel!!
        val meetsMax = maxLevel == null || mobLevel <= maxLevel!!

        return meetsMin && meetsMax
    }

    companion object{
        fun withRangeFromString(range: String?): LevelTierMatching?{
            val result = LevelTierMatching()
            return if (result.setRangeFromString(range))
                result
            else
                null
        }
    }

    fun setRangeFromString(range: String?): Boolean {
        val result = MinAndMax.setAmountRangeFromString(range) ?: return false

        this.minLevel = result.min
        this.maxLevel = result.max

        return true
    }

    override fun toString(): String {
        if (!hasLevelRestriction) {
            return if (names != null && names!!.isNotEmpty()) {
                names.toString()
            } else if (sourceTierName != null) {
                if (valueRanges == null) {
                    sourceTierName!!
                } else {
                    "$sourceTierName: $valueRanges"
                }
            } else {
                "(empty)"
            }
        }

        if (minLevel != null && maxLevel != null) {
            val msg = if (names == null) valueRanges.toString() else names.toString()
            return "$minLevel-$maxLevel $msg"
        }
        return if (minLevel != null) {
            val msg = if (names == null) valueRanges.toString() else names.toString()
            "$minLevel- $msg"
        } else {
            val msg =  if (names == null) valueRanges.toString() else names.toString()
            "-$maxLevel $msg"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is LevelTierMatching) return false

        return this.toString() == other.toString()
    }

    override fun hashCode(): Int {
        return Objects.hash(this.toString())
    }
}