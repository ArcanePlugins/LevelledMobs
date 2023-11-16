package io.github.arcaneplugins.levelledmobs.bukkit.logic.group

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList


class GroupPreParseEvent(val group: Group) : Event(), Cancellable {
    companion object {
        val HANDLERS = HandlerList()
    }

    private var cancelled = false

    override fun isCancelled(): Boolean {
        return this.cancelled
    }

    override fun setCancelled(state: Boolean){
        this.cancelled = state
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS;
    }
}