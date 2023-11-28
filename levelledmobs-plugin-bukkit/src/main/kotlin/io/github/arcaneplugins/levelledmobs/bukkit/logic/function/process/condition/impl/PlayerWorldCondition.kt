package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalCollection
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalList
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException

class PlayerWorldCondition(
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
                    "entity world condition error: no in-list/not-in-list declaration",
                    true
                )
            }
        } catch (ex: ConfigurateException) {
            //TODO make better error message
            sev("entity world condition error: unable to parse yml", true)
        }
    }

    override fun applies(context: Context): Boolean {
        requireNotNull(modalList){ "modalList must not be null" }
        requireNotNull(context.player){ "Player must not be null" }

        return modalList!!.contains(context.player!!.world.name)
    }
}