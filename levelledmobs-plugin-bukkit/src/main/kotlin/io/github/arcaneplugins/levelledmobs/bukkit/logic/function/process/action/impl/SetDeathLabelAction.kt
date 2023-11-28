package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl

import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setDeathLabelFormula
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import org.spongepowered.configurate.CommentedConfigurationNode

class SetDeathLabelAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    val formula = actionNode.node("formula").string!!

    override fun run(context: Context) {
        requireNotNull(
            context.livingEntity
        ) { "Action requires LivingEntity context" }

        setDeathLabelFormula(context.livingEntity!!, formula, true)
    }
}