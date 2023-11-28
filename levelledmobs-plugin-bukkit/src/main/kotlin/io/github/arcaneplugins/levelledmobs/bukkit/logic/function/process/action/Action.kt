package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import org.spongepowered.configurate.CommentedConfigurationNode

abstract class Action(
    val parentProcess: Process,
    val actionNode: CommentedConfigurationNode
) {
    abstract fun run(context: Context)
}