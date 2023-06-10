package io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc

import org.spongepowered.configurate.CommentedConfigurationNode

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