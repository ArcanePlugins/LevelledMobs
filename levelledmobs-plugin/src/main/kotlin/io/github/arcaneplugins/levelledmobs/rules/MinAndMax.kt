package io.github.arcaneplugins.levelledmobs.rules

/**
 * Holds two int values that are usually used to
 * define a min and max value
 *
 * @author stumper66
 * @since 3.2.0
 */
class MinAndMax : Comparable<MinAndMax>{
    var min: Int = 0
    var max: Int = 0

    val isEmpty: Boolean
        get() = (min == 0 && max == 0)

    override fun toString(): String {
        return String.format("%s-%s", this.min, this.max)
    }

    override fun compareTo(other: MinAndMax): Int {
        return if (other.min == this.min && other.max == this.max) {
            0
        } else {
            1
        }
    }
}