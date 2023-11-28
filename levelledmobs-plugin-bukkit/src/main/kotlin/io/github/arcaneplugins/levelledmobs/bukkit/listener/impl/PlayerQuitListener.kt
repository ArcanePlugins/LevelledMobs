package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl

import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.ConfirmSubcommand
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.runFunctionsWithTriggers
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerQuitEvent

class PlayerQuitListener : ListenerWrapper(true) {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handle(event: PlayerQuitEvent){
        val player = event.player

        ConfirmSubcommand.CONFIRMATION_MAP.remove(player)

        /* Trigger */

        runFunctionsWithTriggers(
            Context().withPlayer(player),
            mutableListOf("on-player-quit", "on-player-leave")
        )
    }
}