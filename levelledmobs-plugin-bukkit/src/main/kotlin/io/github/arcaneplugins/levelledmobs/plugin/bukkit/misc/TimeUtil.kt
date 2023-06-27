/*
This program is/was a part of the LevelledMobs project's source code.
Copyright (C) 2023  Lachlan Adamson (aka lokka30)
Copyright (C) 2023  LevelledMobs Contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc

import org.spongepowered.configurate.CommentedConfigurationNode

//todo doc
object TimeUtil {

    // todo doc. function parses time string and returns a long which is the time in Minecraft ticks
    fun parseDelayAtConfigNode(
        node: CommentedConfigurationNode
    ): Long {

        val strVal: String = (node.string ?: "0")
            .lowercase()
            .replace(" ", "")

        fun numericComponent(suffix: String): Double {
            return strVal.substringBefore(suffix).toDouble()
        }

        val suffix1: String = strVal.takeLast(1)
        val suffix2: String = strVal.takeLast(2)

        try {
            return when {
                suffix1 === "t" -> numericComponent("t")
                suffix2 === "ms" -> numericComponent("ms") / 1000 * 20
                suffix1 === "s" -> numericComponent("s") * 20
                suffix1 === "m" -> numericComponent("m") * 60 * 20
                suffix1 === "h" -> numericComponent("h") * 60 * 60 * 20
                suffix1 === "d" -> numericComponent("d") * 24 * 60 * 60 * 20
                else -> strVal.toDouble()
            }.toLong()
        } catch(ex: NumberFormatException) {
            throw IllegalArgumentException(
                "Attempted to parse a delay with an invalid format '${strVal}'",
                ex
            )
        }
    }

}