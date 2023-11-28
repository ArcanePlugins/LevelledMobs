package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setDropTableIds
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

class SetDropTablesAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    val dropTablesToSet = mutableSetOf<String>()

    init {
        try {
            dropTablesToSet.addAll(
                actionNode.node("ids")
                    .getList(String::class.java, emptyList<String>())
            )
        } catch (e: SerializationException) {
            throw RuntimeException(e)
        }
    }

    override fun run(context: Context) {
        requireNotNull(context.livingEntity) { "Action requires LivingEntity context" }

        require(EntityDataUtil.isLevelled(context.livingEntity!!, true)) {
            "Action requires levelled mob context"
        }

        setDropTableIds(context.livingEntity!!, dropTablesToSet)
    }
}