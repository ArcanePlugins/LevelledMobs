package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import org.spongepowered.configurate.CommentedConfigurationNode

abstract class Condition(
    val name: String,
    val parentProcess: Process?,
    val conditionNode: CommentedConfigurationNode) {

    abstract fun applies(context: Context) : Boolean
}