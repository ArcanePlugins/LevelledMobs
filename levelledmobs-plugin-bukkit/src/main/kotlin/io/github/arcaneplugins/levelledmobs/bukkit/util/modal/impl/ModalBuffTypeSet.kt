package io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl

import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs.BuffType
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalCollection
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import java.util.*

class ModalBuffTypeSet(
    items: EnumSet<BuffType>,
    mode: Mode
): ModalCollection<BuffType>(items, mode) {
    companion object{
        fun fromCfgSection(
            node: CommentedConfigurationNode
        ): ModalBuffTypeSet{
            val mode: Mode
            val items: EnumSet<BuffType>

            if (node.hasChild("in-list")) {
                mode = Mode.INCLUSIVE
                items = EnumSet.noneOf(BuffType::class.java)
                try {
                    for (buffTypeStr in node.node("in-list").getList(String::class.java, emptyList())) {
                        items.add(BuffType.valueOf(buffTypeStr.uppercase()))
                    }
                } catch (ex: ConfigurateException) {
                    throw RuntimeException(ex)
                }
            } else {
                mode = Mode.EXCLUSIVE
                items = EnumSet.allOf(BuffType::class.java)
                try {
                    for (buffTypeStr in node.node("not-in-list").getList(String::class.java, emptyList())) {
                        items.remove(BuffType.valueOf(buffTypeStr.uppercase()))
                    }
                } catch (ex: ConfigurateException) {
                    throw RuntimeException(ex)
                }
            }

            return ModalBuffTypeSet(items, mode)
        }
    }
}