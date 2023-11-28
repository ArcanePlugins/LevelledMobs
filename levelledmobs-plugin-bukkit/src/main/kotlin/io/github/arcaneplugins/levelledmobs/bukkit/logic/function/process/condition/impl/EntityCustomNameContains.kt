package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalCollection
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalList
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException

class EntityCustomNameContains(
    process: Process,
    node: CommentedConfigurationNode
): Condition(process, node) {
    var modalList: ModalList<String>? = null

    init {
        try {
            if (conditionNode.hasChild("in-list")) {
                modalList = ModalList(
                    conditionNode.node("in-list")
                        .getList(String::class.java, emptyList()),
                    ModalCollection.Mode.INCLUSIVE
                )
            } else if (conditionNode.hasChild("not-in-list")) {
                modalList = ModalList(
                    conditionNode.node("not-in-list")
                        .getList(String::class.java, emptyList()),
                    ModalCollection.Mode.EXCLUSIVE
                )
            } else {
                //TODO make better error message
                sev(
                    "entity custom name contains condition error: no in-list/not-in-list declaration",
                    true
                )
            }
        } catch (ex: ConfigurateException) {
            //TODO make better error message
            sev("entity custom name contains condition error: unable to parse yml", true)
        }
    }

    override fun applies(context: Context): Boolean {
        requireNotNull(context.entity) { "Entity was not be null" }
        val customName = context.entity!!.customName ?:
            return modalList!!.mode == ModalCollection.Mode.EXCLUSIVE

        val hasAsterisk: Boolean = modalList!!.items.stream().anyMatch { s: String? ->
            "*".contains(s!!)
        }

        if (hasAsterisk) {
            return when (modalList!!.mode) {
                ModalCollection.Mode.INCLUSIVE -> true
                ModalCollection.Mode.EXCLUSIVE -> false
            }
        }

        val contains: Boolean = modalList!!.items.stream()
            .anyMatch { s: String? -> customName.contains(s!!) }

        return when (modalList!!.mode) {
            ModalCollection.Mode.INCLUSIVE -> contains
            ModalCollection.Mode.EXCLUSIVE -> !contains
        }
    }
}