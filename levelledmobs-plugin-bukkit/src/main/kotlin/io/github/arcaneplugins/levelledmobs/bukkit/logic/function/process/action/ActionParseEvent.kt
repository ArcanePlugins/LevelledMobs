package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action

import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.spongepowered.configurate.CommentedConfigurationNode

class ActionParseEvent(
    val identifier: String,
    val process: Process,
    val node: CommentedConfigurationNode
): Event() {
    companion object{
        val HANDLERS = HandlerList()

        fun getHandlerList(): HandlerList{
            return HANDLERS
        }
    }
    var claimed = false
    var cancelled = false

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
}