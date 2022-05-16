package me.lokka30.levelledmobs.bukkit.listeners

import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent

/*
FIXME comment
 */
class PlayerJoinListener : ListenerWrapper(
    "org.bukkit.event.player.PlayerJoinEvent"
) {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!event.player.isOp) return
        event.player.sendMessage("${ChatColor.AQUA}LevelledMobs 4! Wooo!")
    }

}