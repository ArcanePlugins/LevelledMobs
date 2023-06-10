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

        val str: String = (node.string ?: "0").lowercase()

        fun numericComponent(suffix: String): Double {
            return str.substringBefore(suffix).toDouble()
        }

        try {
            return if(str.endsWith("t")) {
                numericComponent("t").toLong()
            } else if(str.endsWith("ms")) {
                (numericComponent("ms") / 1000 * 20).toLong()
            } else if(str.endsWith("s")) {
                (numericComponent("s") * 20).toLong()
            } else if(str.endsWith("m")) {
                (numericComponent("m") * 60 * 20).toLong()
            } else if(str.endsWith("h")) {
                (numericComponent("h") * 60 * 60 * 20).toLong()
            } else if(str.endsWith("d")) {
                (numericComponent("d") * 24 * 60 * 60 * 20).toLong()
            } else {
                str.toLong()
            }
        } catch(ex: NumberFormatException) {
            throw DescriptiveException(
                "Attempted to parse a delay with an invalid format '${str}'",
                ex
            )
        }
    }

}