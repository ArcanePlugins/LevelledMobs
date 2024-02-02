package io.github.arcaneplugins.levelledmobs.rules

import java.lang.reflect.Modifier
import io.github.arcaneplugins.levelledmobs.util.Utils.isInteger

/**
 * Specifies a region based on coordinates that can be
 * used in a rule condition
 *
 * @author stumper66
 * @since 3.8.0
 */
class WithinCoordinates {
    var startX: Int? = null
    var startY: Int? = null
    var startZ: Int? = null
    var endX: Int? = null
    var endY: Int? = null
    var endZ: Int? = null
    private var infinityDirectionX = InfinityDirection.NONE
    private var infinityDirectionY = InfinityDirection.NONE
    private var infinityDirectionZ = InfinityDirection.NONE

    fun parseAxis(
        number: String?,
        axis: Axis,
        isStart: Boolean
    ): Boolean {
        if (number == null) return true

        var infinityDirection = InfinityDirection.NONE
        if ("-" == number) {
            infinityDirection = InfinityDirection.DESCENDING
        } else if ("+" == number) {
            infinityDirection = InfinityDirection.ASCENDING
        } else if (isInteger(number)) {
            val num = number.toInt()
            when (axis) {
                Axis.X -> {
                    if (isStart) startX = num else endX = num
                }

                Axis.Y -> {
                    if (isStart) startY = num else endY = num
                }

                Axis.Z -> {
                    if (isStart) startZ = num else endZ = num
                }
            }
            return true
        }

        if (infinityDirection != InfinityDirection.NONE) {
            when (axis) {
                Axis.X -> infinityDirectionX = infinityDirection
                Axis.Y -> infinityDirectionY = infinityDirection
                Axis.Z -> infinityDirectionZ = infinityDirection
            }
            return true
        }

        return false
    }

    val isEmpty: Boolean
        get() {
            for (f in this.javaClass.declaredFields) {
                if (!Modifier.isPublic(f.modifiers)) continue

                try {
                    if (f[this] != null) {
                        return false
                    }
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }

            return true
        }

    val getHasX: Boolean
        get() = startX != null || endX != null


    val getHasY: Boolean
        get() = startY != null || endY != null


    val getHasZ: Boolean
        get() = startZ != null || endZ != null


    fun isLocationWithinRange(
        coord: Int,
        axis: Axis
    ): Boolean {
        var range1: Int? = null
        var range2: Int? = null
        var infinityDirection = InfinityDirection.NONE
        when (axis) {
            Axis.X -> {
                range1 = this.startX
                range2 = this.endX
                infinityDirection = this.infinityDirectionX
            }

            Axis.Y -> {
                range1 = this.startY
                range2 = this.endY
                infinityDirection = this.infinityDirectionY
            }

            Axis.Z -> {
                range1 = this.startZ
                range2 = this.endZ
                infinityDirection = this.infinityDirectionZ
            }
        }
        if (range1 == null && range2 == null) return false

        if (range1 != null && range2 != null) {
            return if (range1 < range2) {
                coord >= range1 && coord <= range2
            } else {
                coord >= range2 && coord <= range1
            }
        }

        val useRange = range1 ?: range2!!
        return when (infinityDirection) {
            InfinityDirection.NONE -> { coord == useRange }
            InfinityDirection.ASCENDING -> { coord >= useRange}
            else -> { coord <= useRange }
        }
    }

    enum class Axis {
        X,
        Y,
        Z
    }

    enum class InfinityDirection {
        NONE,
        ASCENDING,
        DESCENDING
    }

    override fun toString(): String {
        val sb = StringBuilder()

        checkNumber(startX, "startX", sb)
        checkNumber(endX, "endX", sb)
        checkInfinityDirection(infinityDirectionX, "X", sb)
        checkNumber(startY, "startY", sb)
        checkNumber(endY, "endY", sb)
        checkInfinityDirection(infinityDirectionY, "Y", sb)
        checkNumber(startZ, "startZ", sb)
        checkNumber(endZ, "endZ", sb)
        checkInfinityDirection(infinityDirectionZ, "Z", sb)

        return if (sb.isEmpty()) super.toString()
        else sb.toString()
    }

    private fun checkNumber(num: Int?, name: String, sb: java.lang.StringBuilder) {
        if (num == null) return

        if (sb.isNotEmpty()) sb.append(", ")
        sb.append(name).append(": ").append(num)
    }

    private fun checkInfinityDirection(
        infinityDirection: InfinityDirection,
        name: String,
        sb: java.lang.StringBuilder
    ) {
        if (infinityDirection == InfinityDirection.NONE) return

        if (sb.isNotEmpty()) sb.append(", ")
        sb.append(name).append(": ")
        if (infinityDirection == InfinityDirection.ASCENDING) sb.append("+")
        else sb.append("-")
    }
}