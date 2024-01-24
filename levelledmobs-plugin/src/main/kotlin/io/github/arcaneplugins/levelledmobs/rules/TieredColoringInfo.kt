package io.github.arcaneplugins.levelledmobs.rules

import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.util.Utils.isInteger

/**
 * Holds the values parsed from rules.yml used with the tiered placeholder for nametags
 *
 * @author stumper66
 * @since 3.0.0
 */
class TieredColoringInfo : Cloneable {
    var minLevel = 0
    var maxLevel = 0
    var text: String? = null
    var isDefault = false

    companion object{
        fun createDefault(value: String): TieredColoringInfo {
            val coloringInfo = TieredColoringInfo()
            coloringInfo.isDefault = true
            coloringInfo.text = value

            return coloringInfo
        }

        fun createFromString(
            key: String,
            value: String
        ): TieredColoringInfo? {
            val numbers = key.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (numbers.size != 2 && numbers.size != 1) {
                Utils.logger.warning("Invalid tiered coloring key: $key")
                return null
            }

            val coloringInfo = TieredColoringInfo()
            coloringInfo.text = value

            if (numbers.size == 1) {
                coloringInfo.minLevel = numbers[0].toInt()
                coloringInfo.maxLevel = coloringInfo.minLevel
                return coloringInfo
            }

            for (i in 0..1) {
                val num = numbers[i].trim { it <= ' ' }
                if (!isInteger(num)) {
                    Utils.logger.warning("Invalid number in tiered coloring key: $key")
                    return null
                }

                if (i == 0) {
                    coloringInfo.minLevel = num.toInt()
                } else {
                    coloringInfo.maxLevel = num.toInt()
                }
            }

            return coloringInfo
        }
    }

    override fun toString(): String {
        return if (isDefault) {
            text + "default&r"
        } else {
            "$text$minLevel-$maxLevel&r"
        }
    }

    fun cloneItem(): TieredColoringInfo? {
        var copy: TieredColoringInfo? = null
        try {
            copy = super.clone() as TieredColoringInfo?
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy
    }
}