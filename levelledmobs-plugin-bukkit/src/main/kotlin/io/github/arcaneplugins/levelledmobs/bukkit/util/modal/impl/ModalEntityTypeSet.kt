package io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl

import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalCollection
import org.bukkit.entity.EntityType
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException

class ModalEntityTypeSet(
    strItems: MutableList<String>,
    mode: Mode
) : ModalCollection<EntityType>(mutableSetOf(), mode) {
    init {
        for (strItem in strItems) {
            if (strItem == "*") {
                items.clear()
                this.mode = mode.inverse()
                break
            }
            items.add(EntityType.valueOf(strItem.uppercase()))
        }
    }

    companion object{
        fun parseNode(
            node: CommentedConfigurationNode
        ): ModalEntityTypeSet{
            return try {
                if (node.hasChild("in-list")) {
                    ModalEntityTypeSet(
                        node.node("in-list").getList(String::class.java, ArrayList()),
                        Mode.INCLUSIVE
                    )
                } else if (node.hasChild("not-in-list")) {
                    ModalEntityTypeSet(
                        node.node("not-in-list").getList(String::class.java, ArrayList()),
                        Mode.EXCLUSIVE
                    )
                } else {
                    throw IllegalStateException(
                        "Modal list at path '${node.path()}' " +
                                "does not have 'in-list' or 'not-in-list' declared."
                    )
                }
            } catch (ex: ConfigurateException) {
                sev("Unable to parse modal list at path: ${node.path()}", true)
                throw RuntimeException(ex)
            }
        }
    }
}