package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl

import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.runFunctionsWithTriggers
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityExplodeEvent

class EntityExplodeListener : ListenerWrapper(true) {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun handle(event: EntityExplodeEvent) {
        val entity = event.entity

        /*
        Fire the associated trigger.
         */
        runFunctionsWithTriggers(
            Context(entity), mutableListOf("on-entity-explode")
        )
    }
}