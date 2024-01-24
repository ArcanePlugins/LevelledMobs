package io.github.arcaneplugins.levelledmobs.misc

import io.github.arcaneplugins.levelledmobs.util.Utils.isInteger

/**
 * Used as a placeholder for when a number or a number-range is passed from a user argument
 *
 * @author stumper66
 * @since 3.2.0
 */
class RequestedLevel {
    var level = 0
    var levelRangeMin = 0
    var levelRangeMax = 0
    var hasLevelRange = false
    var hadInvalidArguments = false

    fun setMinAllowedLevel(level: Int) {
        if (this.hasLevelRange && this.levelRangeMin < level) {
            this.levelRangeMin = level
        } else if (!this.hasLevelRange && this.level < levelRangeMax) {
            this.level = level
        }
    }

    fun setMaxAllowedLevel(level: Int) {
        if (this.hasLevelRange && this.levelRangeMax > level) {
            this.levelRangeMax = level
        } else if (!this.hasLevelRange && this.level > level) {
            this.level = level
        }
    }

    val levelMin: Int
        get() {
            return if (this.hasLevelRange) {
                levelRangeMin
            } else {
                level
            }
        }

    val levelMax: Int
        get() {
            return if (this.hasLevelRange) {
                levelRangeMax
            } else {
                level
            }
        }

    fun setLevelFromString(numberOrNumberRange: String?): Boolean {
        if (numberOrNumberRange.isNullOrEmpty()) {
            return false
        }

        if (!numberOrNumberRange.contains("-")) {
            if (!isInteger(numberOrNumberRange)) {
                return false
            }

            this.level = numberOrNumberRange.toInt()
            this.hasLevelRange = false
            return true
        }

        val nums = numberOrNumberRange.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (nums.size != 2) {
            return false
        }

        if (!isInteger(nums[0].trim { it <= ' ' }) || !isInteger(
            nums[1].trim { it <= ' ' })
        ) {
            return false
        }
        this.levelRangeMin = nums[0].trim { it <= ' ' }.toInt()
        this.levelRangeMax = nums[1].trim { it <= ' ' }.toInt()
        this.hasLevelRange = true

        return true
    }

    override fun toString(): String {
        return if (this.hasLevelRange) {
            "$levelRangeMin-$levelRangeMax"
        } else {
            level.toString()
        }
    }
}