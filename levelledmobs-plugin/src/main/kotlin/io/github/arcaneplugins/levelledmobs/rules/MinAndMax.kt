package io.github.arcaneplugins.levelledmobs.rules

import io.github.arcaneplugins.levelledmobs.util.Utils

/**
 * Holds two int or float values that are usually used to
 * define a min and max value
 *
 * @author stumper66
 * @since 3.2.0
 */
class MinAndMax : Comparable<MinAndMax>{
    var min = 0f
        set(value) {
            isDefault = false
            field = value
        }
    var max = 0f
        set(value) {
            isDefault = false
            field = value
        }

    constructor()

    constructor(min: Float, max: Float){
        this.min = min
        this.max = max
    }

    var showAsInt = false
    var isDefault = false
        private set

    val minAsInt: Int
        get() = min.toInt()

    val maxAsInt: Int
        get() = max.toInt()

    companion object{
        fun setAmountRangeFromString(
            numberOrNumberRange: String?
        ): MinAndMax? {
            if (numberOrNumberRange.isNullOrEmpty())
                return null

            if (!numberOrNumberRange.contains("-")) {
                if (!Utils.isDouble(numberOrNumberRange))
                    return null

                val result = MinAndMax()
                result.min = numberOrNumberRange.toDouble().toFloat()
                result.max = result.min
                return result
            }

            val nums = numberOrNumberRange.split("-")
            if (nums.size != 2) return null


            if (!Utils.isDouble(nums[0].trim()) || !Utils.isDouble(nums[1].trim()))
                return null

            val result = MinAndMax()
            result.min = nums[0].trim().toDouble().toFloat()
            result.max = nums[1].trim().toDouble().toFloat()

            return result
        }
    }

    val isEmpty: Boolean
        get() = (min == 0.0f && max == 0.0f)

    override fun toString(): String {
        return if (this.min == this.max) {
            if (showAsInt) "$minAsInt"
            else "$min"
        } else {
            if (showAsInt) "$minAsInt-$maxAsInt"
            else "$min-$max"
        }
    }

    override fun compareTo(other: MinAndMax): Int {
        return if (other.min == this.min && other.max == this.max)
            0 else 1
    }
}