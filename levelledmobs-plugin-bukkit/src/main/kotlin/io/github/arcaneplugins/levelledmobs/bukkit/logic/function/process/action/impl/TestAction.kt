package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.inf
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.spongepowered.configurate.CommentedConfigurationNode

class TestAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    override fun run(context: Context) {
        inf("Test action ran at path: " + actionNode.path())

        //noinspection ConstantConditions
        Bukkit.broadcastMessage(
            "Movement speed = " +
                    context.livingEntity!!
                        .getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!
                        .value
        )

        Bukkit.broadcastMessage(
            "Max health = " +
                    context.livingEntity!!
                        .getAttribute(Attribute.GENERIC_MAX_HEALTH)!!
                        .value
        )
    }
}