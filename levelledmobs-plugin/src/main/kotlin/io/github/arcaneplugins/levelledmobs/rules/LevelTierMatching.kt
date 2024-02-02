package io.github.arcaneplugins.levelledmobs.rules

import java.util.Objects
import io.github.arcaneplugins.levelledmobs.util.Utils.isInteger

/**
 * Holds any rule information relating to leveled tiers
 *
 * @author stumper66
 * @since 3.1.0
 */
class LevelTierMatching {
    var names: MutableList<String>? = null
    var valueRanges: IntArray? = null
    var sourceTierName: String? = null
    var minLevel: Int? = null
    var maxLevel: Int? = null
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

    fun setRangeFromString(range: String?): Boolean {
        val result: IntArray = getRangeFromString(range)

        if (result[0] == -1 && result[1] == -1) {
            return false
        }

        if (result[0] >= 0) {
            this.minLevel = result[0]
        }
        if (result[1] >= 0) {
            this.maxLevel = result[1]
        }

        return true
    }

    companion object{
        fun getRangeFromString(range: String?): IntArray {
            val result = intArrayOf(-1, -1)

            if (range.isNullOrEmpty()) {
                return result
            }

            if (!range.contains("-")) {
                if (!isInteger(range)) {
                    return result
                }

                result[0] = range.toInt()
                result[1] = result[0]
                return result
            }

            val nums = range.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (nums.size != 2) {
                return result
            }

            if (!isInteger(nums[0].trim { it <= ' ' }) || !isInteger(
                    nums[1].trim { it <= ' ' })
            ) {
                return result
            }
            result[0] = nums[0].trim { it <= ' ' }.toInt()
            result[1] = nums[1].trim { it <= ' ' }.toInt()

            return result
        }
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
            return String.format(
                "%s-%s %s", minLevel, maxLevel,
                if (names == null) valueRanges.contentToString() else names
            )
        }
        return if (minLevel != null) {
            String.format(
                "%s- %s", minLevel,
                if (names == null) valueRanges.contentToString() else names
            )
        } else {
            String.format(
                "-%s %s", maxLevel,
                if (names == null) valueRanges.contentToString() else names
            )
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