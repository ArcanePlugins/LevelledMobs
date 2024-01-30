package io.github.arcaneplugins.levelledmobs.result

import kotlin.math.max
import kotlin.math.min

/**
 * Holds values used when a min and max value is needed
 *
 * @author stumper66
 * @since 3.12.2
 */
class MinAndMaxHolder(
    var min: Int,
    var max: Int
) {
    var useMin: Boolean = true

    fun ensureMinAndMax(min: Int, max: Int) {
        this.min = max(this.min.toDouble(), min.toDouble()).toInt()
        this.max = min(this.max.toDouble(), max.toDouble()).toInt()
    }

    override fun toString(): String {
        return "$min, $max"
    }
}