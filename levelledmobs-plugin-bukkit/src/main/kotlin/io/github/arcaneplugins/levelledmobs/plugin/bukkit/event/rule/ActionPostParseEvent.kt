package io.github.arcaneplugins.levelledmobs.plugin.bukkit.event.rule

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

//todo doc
class ActionPostParseEvent(
    val action: Action,
) : Event() {

    companion object {
        val HANDLERS: HandlerList = HandlerList()
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

}