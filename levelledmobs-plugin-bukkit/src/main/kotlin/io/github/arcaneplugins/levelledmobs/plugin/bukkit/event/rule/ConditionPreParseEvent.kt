package io.github.arcaneplugins.levelledmobs.plugin.bukkit.event.rule

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.condition.Condition
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

//todo doc
class ConditionPreParseEvent(
    val condition: Condition,
) : Event(), Cancellable {

    companion object {
        val HANDLERS: HandlerList = HandlerList()
    }

    private var cancelled: Boolean = false

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }

}