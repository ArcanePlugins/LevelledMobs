package io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder

import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ContextPlaceholdersLoadEvent: Event() {
    companion object{
        val HANDLERS = HandlerList()
    }

    fun getContextPlaceholders(): MutableSet<ContextPlaceholder>{
        return LogicHandler.CONTEXT_PLACEHOLDER_HANDLER.contextPlaceholders
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
}