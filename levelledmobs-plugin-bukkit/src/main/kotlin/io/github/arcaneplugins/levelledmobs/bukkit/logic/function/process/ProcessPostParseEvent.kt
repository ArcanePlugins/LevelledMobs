package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ProcessPostParseEvent(
    val process: Process
): Event(), Cancellable {
    companion object{
        val HANDLERS = HandlerList()
    }

    var cancelled = false

    override fun isCancelled(): Boolean {
        return this.cancelled
    }

    override fun setCancelled(state: Boolean) {
        this.cancelled = state
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
}