package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl

import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.runFunctionsWithTriggers
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent

class EntityDamageListener : ListenerWrapper(true) {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun handle(event: EntityDamageEvent){
        val entity = event.entity

        runFunctionsWithTriggers(
            Context(entity),
            mutableListOf("on-entity-damage")
        )
    }
}