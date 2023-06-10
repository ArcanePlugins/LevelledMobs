package io.github.arcaneplugins.levelledmobs.plugin.bukkit.event.rule

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

//todo doc
class RulePostParseEvent(
    val rule: Rule,
) : Event() {

    companion object {
        val HANDLERS: HandlerList = HandlerList()
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

}