package io.github.arcaneplugins.levelledmobs.bukkit.logic.function

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

//TODO this should not be cancellable!
class FunctionPostParseEvent(
    val function: LmFunction
): Event(), Cancellable {
    companion object{
        val HANDLERS = HandlerList()
    }
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