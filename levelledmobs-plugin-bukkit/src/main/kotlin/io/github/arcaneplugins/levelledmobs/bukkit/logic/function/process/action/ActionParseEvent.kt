package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action

import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.spongepowered.configurate.CommentedConfigurationNode


class ActionParseEvent(
    val identifier: String,
    val process: Process,
    val node: CommentedConfigurationNode
): Event(), Cancellable {
    companion object{
        val HANDLERS = HandlerList()
    }
    var claimed = false
    var cancelled = false

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(state: Boolean) {
        this.cancelled = state
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
}