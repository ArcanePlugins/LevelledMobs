package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl

import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.groups
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.Group
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalCollection
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalList
import org.bukkit.block.Biome
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import java.util.stream.Collectors

class EntityBiomeCondition(
    process: Process,
    node: CommentedConfigurationNode
): Condition("Entity Biome", process, node) {
    var biomeModalList: ModalList<Biome>? = null
    var groupModalList: ModalList<Group>? = null

    init {
        try {
            if (conditionNode.hasChild("in-list")) {
                biomeModalList = ModalList(
                    conditionNode.node("in-list")
                        .getList(Biome::class.java, mutableListOf()),
                    ModalCollection.Mode.INCLUSIVE
                )
            } else if (conditionNode.hasChild("not-in-list")) {
                biomeModalList = ModalList(
                    conditionNode.node("not-in-list")
                        .getList(Biome::class.java, mutableListOf()),
                    ModalCollection.Mode.EXCLUSIVE
                )
            } else if (conditionNode.hasChild("in-group")) {
                val group = conditionNode.node("in-group")
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
                    .collect(Collectors.toList())

                groupModalList = ModalList(group, ModalCollection.Mode.INCLUSIVE)
            } else if (conditionNode.hasChild("not-in-group")) {
                val group = conditionNode.node("not-in-group")
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
                    .collect(Collectors.toList())

                groupModalList = ModalList(group, ModalCollection.Mode.EXCLUSIVE)
            } else {
                throw IllegalArgumentException(
                    "Missing 'in-list' or 'not-in-list' or 'in-group' or 'not-in-group' "
                            + "declaration in entity biome condition"
                )
            }
        } catch (ex: ConfigurateException) {
            throw RuntimeException(ex)
        }
    }

    override fun applies(context: Context): Boolean {
        val biome = context.entity!!.location.block.biome

        return if (biomeModalList != null) {
            biomeModalList!!.contains(biome)
        } else if (groupModalList != null) {
            /*
                if any of the groups in the group modal list contain the biome, and the modal list
                mode is Inclusive, then return true
                 */
            val contains: Boolean = groupModalList!!.items.stream()
                .anyMatch { group: Group ->
                    group.items.contains(
                        biome.name
                    )
                }
            val mode: ModalCollection.Mode = groupModalList!!.mode
            when (mode) {
                ModalCollection.Mode.INCLUSIVE -> contains
                ModalCollection.Mode.EXCLUSIVE -> !contains
            }
        } else {
            throw IllegalStateException("Biome and group modal lists are undefined")
        }
    }
}