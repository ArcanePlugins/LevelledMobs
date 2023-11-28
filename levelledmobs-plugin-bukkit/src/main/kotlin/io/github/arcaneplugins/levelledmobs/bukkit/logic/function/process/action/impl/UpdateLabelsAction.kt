package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl

import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.updateLabels
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import org.spongepowered.configurate.CommentedConfigurationNode

class UpdateLabelsAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    override fun run(context: Context) {
        if (context.livingEntity != null){
            updateLabels(context.livingEntity!!, context, false)
        }

        if (context.player != null){
            updateLabels(context.player!!, context, false)
        }
    }
}