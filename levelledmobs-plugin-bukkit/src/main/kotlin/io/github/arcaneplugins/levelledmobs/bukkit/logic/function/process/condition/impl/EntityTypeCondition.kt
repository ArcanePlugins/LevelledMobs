package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl

import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.groups
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.Group
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalCollection
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalList
import org.bukkit.entity.EntityType
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import java.util.function.Predicate
import java.util.stream.Collectors

class EntityTypeCondition(
    process: Process,
    node: CommentedConfigurationNode
): Condition("Entity Type", process, node) {
    var entityTypeModalList: ModalList<EntityType>? = null
    var groupModalList: ModalList<Group>? = null

    init {
        try {
            if (conditionNode.hasChild("in-list")) {
                entityTypeModalList = ModalList(
                    conditionNode.node("in-list")
                        .getList(EntityType::class.java, emptyList()),
                    ModalCollection.Mode.INCLUSIVE
                )
            } else if (conditionNode.hasChild("not-in-list")) {
                entityTypeModalList = ModalList(
                    conditionNode.node("not-in-list")
                        .getList(EntityType::class.java, emptyList()),
                    ModalCollection.Mode.EXCLUSIVE
                )
            } else if (conditionNode.hasChild("in-group")) {
                groupModalList = ModalList(
                    conditionNode.node("in-group")
                        .getList(String::class.java, emptyList())
                        .stream()
                        .map { groupId: String ->
                            val group =
                                groups.stream()
                                    .filter { otherGroup: Group ->
                                        otherGroup.identifier.equals(groupId,ignoreCase = true)
                                    }
                                    .findFirst()
                            require(!group.isEmpty) { "Unknown group: $groupId" }
                            group.get()
                        }
                        .collect(Collectors.toList()),
                    ModalCollection.Mode.INCLUSIVE
                )
            } else if (conditionNode.hasChild("not-in-group")) {
                groupModalList = ModalList(
                    conditionNode.node("not-in-group")
                        .getList(String::class.java, emptyList())
                        .stream()
                        .map { groupId: String ->
                            val group =
                                groups.stream()
                                    .filter { otherGroup: Group ->
                                        otherGroup.identifier.equals(groupId,ignoreCase = true)
                                    }
                                    .findFirst()
                            require(!group.isEmpty) { "Unknown group: $groupId" }
                            group.get()
                        }
                        .collect(Collectors.toList()),
                    ModalCollection.Mode.EXCLUSIVE
                )
            } else {
                throw IllegalArgumentException(
                    "Missing 'in-list' or 'not-in-list' or 'in-group' or 'not-in-group' "
                            + "declaration in entity type condition"
                )
            }
        } catch (ex: ConfigurateException) {
            throw RuntimeException(ex)
        }
    }

    override fun applies(context: Context): Boolean {
        requireNotNull(context.entityType){ "EntityType must not be null" }
        val et = context.entityType!!

        return if (entityTypeModalList != null) {
            entityTypeModalList!!.contains(et)
        } else if (groupModalList != null) {
            /*
                if any of the groups in the group modal list contain the entity type, and the modal list
                mode is Inclusive, then return true
                 */
            val contains: Boolean = groupModalList!!.items.stream()
                .anyMatch(Predicate<Group> { group: Group ->
                    group.items.contains(
                        et.name
                    )
                })
            val mode: ModalCollection.Mode = groupModalList!!.mode
            when (mode) {
                ModalCollection.Mode.INCLUSIVE -> contains
                ModalCollection.Mode.EXCLUSIVE -> !contains
            }
        } else {
            throw IllegalStateException("EntityType and group modal lists are undefined")
        }
    }
}