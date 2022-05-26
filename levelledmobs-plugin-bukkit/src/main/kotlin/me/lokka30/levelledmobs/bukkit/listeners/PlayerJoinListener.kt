package me.lokka30.levelledmobs.bukkit.listeners

import org.bukkit.ChatColor.AQUA
import org.bukkit.ChatColor.DARK_GRAY
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
        event.player.sendMessage("${AQUA}This server is running LevelledMobs 4 [Alpha]")
        event.player.sendMessage("${DARK_GRAY}https://github.com/lokka30/LevelledMobs/")
        //FIXME add more stuff
    }

}