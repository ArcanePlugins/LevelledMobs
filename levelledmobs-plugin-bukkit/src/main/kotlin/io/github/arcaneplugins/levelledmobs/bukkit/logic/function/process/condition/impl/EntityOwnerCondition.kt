package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition
import org.spongepowered.configurate.CommentedConfigurationNode

class EntityOwnerCondition(
    process: Process,
    node: CommentedConfigurationNode
): Condition(process, node) {
    override fun applies(context: Context): Boolean {

        requireNotNull(context.livingEntity) { "LivingEntity context must not be null" }

        //TODO check integrations


        //TODO check integrations
        return true
    }
}