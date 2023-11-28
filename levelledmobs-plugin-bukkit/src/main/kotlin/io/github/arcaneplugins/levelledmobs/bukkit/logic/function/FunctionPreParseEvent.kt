package io.github.arcaneplugins.levelledmobs.bukkit.logic.function

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class FunctionPreParseEvent(
    val function: LmFunction
) : Event() {
    companion object{
        val HANDLERS = HandlerList()
    }
    var cancelled = false

    override fun getHandlers(): HandlerList{
        return HANDLERS
    }
}