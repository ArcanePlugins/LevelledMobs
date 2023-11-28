package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.getDropTableIds
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil.setDropTableIds
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import org.bukkit.entity.LivingEntity
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException

class RemoveDropTablesAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    val dropTablesToRemove = mutableSetOf<String>()
    init {
        try {
            dropTablesToRemove.addAll(
                actionNode.node("ids")
                    .getList(String::class.java, emptyList<String>())
            )
        } catch (e: SerializationException) {
            throw RuntimeException(e)
        }
    }

    override fun run(context: Context) {
        val entity = context.livingEntity ?: throw IllegalArgumentException("Action requires entity context")

        require(EntityDataUtil.isLevelled(entity, true)) { "Action requires levelled mob context" }

        val dropTables: MutableSet<String> = HashSet(getDropTableIds(entity))
        dropTables.removeAll(dropTablesToRemove)
        setDropTableIds(entity, dropTables)
    }
}