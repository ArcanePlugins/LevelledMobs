package io.github.arcaneplugins.levelledmobs.bukkit.util.modal

import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException

class ModalList<T>(
    items: MutableList<T>,
    mode: Mode
) : ModalCollection<T>(items, mode) {
    companion object{
        fun parseModalStringListFromNode(
            node: CommentedConfigurationNode
        ): ModalList<String>{
            var result: ModalList<String>? = null
            try {
                if (node.hasChild("in-list")) {
                    result = ModalList(
                        node.node("in-list").getList(String::class.java, mutableListOf()),
                        Mode.INCLUSIVE
                    )
                } else if (node.hasChild("not-in-list")) {
                    result = ModalList(
                        node.node("not-in-list").getList(String::class.java, mutableListOf()),
                        Mode.EXCLUSIVE
                    )
                } else {
                    throw IllegalStateException(
                        "Modal list at path '" + node.path().toString() +
                                "' does not have 'in-list' or 'not-in-list' declared."
                    )
                }
            } catch (ex: ConfigurateException) {
                sev("Unable to parse modal list at path: " + node.path().toString(), true)
                throw RuntimeException(ex)
            }
            return result
        }
    }
}