package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import org.spongepowered.configurate.CommentedConfigurationNode

class ExitProcessAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    override fun run(context: Context) {
        parentProcess.exit = true
    }
}