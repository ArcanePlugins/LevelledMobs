package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ProcessPreParseEvent(
    val process: Process
): Event() {
    var cancelled = false

    companion object{
        val HANDLERS = HandlerList()
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
}