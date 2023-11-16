package io.github.arcaneplugins.levelledmobs.bukkit.util.math

import java.util.function.Function

object TimeUtils {
    fun parseTimeToTicks(
        o: Any
    ): Long {
        return if (o is String) {
            try {
                // default unit: ticks
                return o.toLong()
            } catch (ignored: NumberFormatException) {}
            val parser =
                Function { suffixLen: Int -> o.substring(0, o.length - suffixLen).toLong() }
            if (o.endsWith("t")) {
                parser.apply(1)
            } else if (o.endsWith("s")) {
                parser.apply(1) * 20
            } else if (o.endsWith("ms")) {
                (parser.apply(2) * 0.002).toLong()
            } else if (o.endsWith("m")) {
                parser.apply(1) * 1200
            } else if (o.endsWith("h")) {
                parser.apply(1) * 72000
            } else if (o.endsWith("d")) {
                parser.apply(1) * 1728000
            } else {
                throw IllegalArgumentException(
                    "Invalid time unit '${o.substring(o.length - 1)}'."
                )
            }
        } else (o as? Number)?.toLong()
            ?: throw IllegalArgumentException(
                "Unable to parse type " + o.javaClass.getSimpleName()
            )
    }
}