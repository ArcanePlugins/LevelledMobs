package me.lokka30.levelledmobs.bukkit.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent

/*
FIXME comment
 */
class PlayerJoinListener : ListenerWrapper(
    "org.bukkit.event.player.PlayerJoinEvent"
) {

    @EventHandler(priority = EventPriority.MONITOR)
    fun handle(event: PlayerJoinEvent) {
        // TODO
    }

}