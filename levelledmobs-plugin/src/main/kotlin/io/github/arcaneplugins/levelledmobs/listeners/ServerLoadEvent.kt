package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent

class ServerLoadEvent : Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    fun onServerLoad(event: ServerLoadEvent) {
        if (event.type != ServerLoadEvent.LoadType.STARTUP) return

        for (mobPlugin in ExternalCompatibilityManager.instance.externalPluginDefinitions.values){
            mobPlugin.clearDetectionCache()
        }

        MainCompanion.instance.hasFinishedLoading = true
        val lmItemsParser = LevelledMobs.instance.customDropsHandler.lmItemsParser
        if (lmItemsParser != null){
            val scheduler = SchedulerWrapper {
                lmItemsParser.processPendingItems()
                if (MainCompanion.instance.showCustomDrops)
                    LevelledMobs.instance.customDropsHandler.customDropsParser.showCustomDropsDebugInfo(null)
            }
            scheduler.runDelayed(10L)
        }
        else if (MainCompanion.instance.showCustomDrops)
            LevelledMobs.instance.customDropsHandler.customDropsParser.showCustomDropsDebugInfo(null)
    }
}