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


class AddDropTablesAction(
    parentProcess: Process,
    actionNode: CommentedConfigurationNode
): Action(parentProcess, actionNode) {
    val dropTablesToAdd = mutableSetOf<String>()
    init {
        try {
            dropTablesToAdd.addAll(
                actionNode.node("ids")
                    .getList(String::class.java, emptyList<String>())
            )
        } catch (e: SerializationException) {
            throw RuntimeException(e)
        }
    }

    override fun run(context: Context) {
        val entity = context.entity ?: throw IllegalArgumentException("Action requires entity context")

        require(entity is LivingEntity) { "Action requires LivingEntity context" }

        require(EntityDataUtil.isLevelled(entity, true)) { "Action requires levelled mob context" }

        val dropTables = HashSet(dropTablesToAdd)
        dropTables.addAll(getDropTableIds(entity))
        setDropTableIds(entity, dropTables)
    }
}