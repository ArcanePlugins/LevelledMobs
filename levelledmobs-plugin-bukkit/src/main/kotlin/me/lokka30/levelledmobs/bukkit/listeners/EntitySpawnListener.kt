package me.lokka30.levelledmobs.bukkit.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntitySpawnEvent

/*
FIXME comment
 */
class EntitySpawnListener : ListenerWrapper(
    "org.bukkit.event.entity.EntitySpawnEvent"
) {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun handle(event: EntitySpawnEvent) {
        //TODO
    }

}