package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.CommandHandler
import io.github.arcaneplugins.levelledmobs.managers.NotifyManager
import io.github.arcaneplugins.levelledmobs.misc.FileLoader
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.PlayerQueueItem
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.MessageUtils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.persistence.PersistentDataType

/**
 * Listens for when a player joins, leaves or changes worlds so that send messages as needed, update
 * nametags or track the player
 *
 * @author lokka30, stumper66
 * @since 2.4.0
 */
class PlayerJoinListener : Listener {
    val main = LevelledMobs.instance

    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinEvent) {
        if (event.player.isOp && main.debugManager.playerThatEnabledDebug == null) {
            main.debugManager.playerThatEnabledDebug = event.player
        }

        main.mainCompanion.checkSettingsWithMaxPlayerOptions()
        main.mainCompanion.addRecentlyJoinedPlayer(event.player)
        checkForNetherPortalCoords(event.player)
        main.nametagTimerChecker.addPlayerToQueue(PlayerQueueItem(event.player, true))
        parseUpdateChecker(event.player)

        if (!LevelledMobs.instance.ver.isRunningFolia)
            updateNametagsInWorldAsync(event.player, event.player.world.entities)

        if (event.player.isOp) {
            if (main.mainCompanion.hadRulesLoadError) {
                event.player.sendMessage(FileLoader.getFileLoadErrorMessage())
            }

            if (NotifyManager.opHasMessage){
                event.player.sendMessage(NotifyManager.pendingMessage!!)
                NotifyManager.clearLastError()
            }

            if (main.customDropsHandler.customDropsParser.hadParsingError){
                event.player.sendMessage(
                    MessageUtils.colorizeAll(
                    "&b&lLevelledMobs:&r &6There was an error parsing customdrops.yml&r\n" +
                            "Check the console log for more details"
                ))
            }

            if (CommandHandler.hadErrorLoading){
                event.player.sendMessage(
                    MessageUtils.colorizeAll(
                        "&b&lLevelledMobs:&r &6There was an error loading the command framework.&r\n" +
                                "Only the reload option will be available."
                    ))
            }
        }
    }

    private fun checkForNetherPortalCoords(player: Player) {
        val keys = mutableListOf(
            NamespacedKeys.playerNetherCoords,
            NamespacedKeys.playerNetherCoordsIntoWorld
        )
        try {
            for (i in keys.indices) {
                val useKey = keys[i]
                if (!player.persistentDataContainer.has(useKey, PersistentDataType.STRING)) {
                    continue
                }

                val netherCoords = player.persistentDataContainer
                    .get(useKey, PersistentDataType.STRING)
                if (netherCoords == null) {
                    continue
                }
                val coords = netherCoords.split(",")
                if (coords.size != 4) {
                    continue
                }
                val world = Bukkit.getWorld(coords[0]) ?: continue
                val location = Location(
                    world, coords[1].toInt().toDouble(),
                    coords[2].toInt().toDouble(), coords[3].toInt().toDouble()
                )

                if (i == 0) {
                    main.mainCompanion.setPlayerNetherPortalLocation(player, location)
                } else {
                    main.mainCompanion.setPlayerWorldPortalLocation(player, location)
                }
            }
        } catch (e: Exception) {
            Log.war(
                "Unable to get player nether portal coords from ${player.name}, "
                    + e.message
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        main.mainCompanion.checkSettingsWithMaxPlayerOptions(true)

        if (main.placeholderApiIntegration != null) {
            main.placeholderApiIntegration!!.playedLoggedOut(event.player)
        }

        main.mainCompanion.spawnerCopyIds.remove(event.player.uniqueId)
        main.mainCompanion.spawnerInfoIds.remove(event.player.uniqueId)
        main.nametagTimerChecker.addPlayerToQueue(PlayerQueueItem(event.player, false))

        if (main.placeholderApiIntegration != null) {
            main.placeholderApiIntegration!!.removePlayer(event.player)
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onChangeWorld(event: PlayerChangedWorldEvent) {
        updateNametagsInWorldAsync(event.player, event.player.world.entities)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onTeleport(event: PlayerTeleportEvent) {
        // on spigot API .getTo is nullable but not Paper
        // only update tags if teleported to a different world
        @Suppress("SENSELESS_COMPARISON")
        if (event.to != null && event.to.world != null && event.from.world != null && event.from.world != event.to.world) {
            updateNametagsInWorldAsync(event.player, event.to.world.entities)
        }
    }

    private fun updateNametagsInWorldAsync(player: Player, entities: List<Entity>) {
        val scheduler = SchedulerWrapper { updateNametagsInWorld(player, entities) }
        scheduler.runDirectlyInFolia = true
        scheduler.run()
    }

    private fun updateNametagsInWorld(player: Player, entities: List<Entity>) {
        val currentPlayers = Bukkit.getOnlinePlayers().size
        if (currentPlayers > main.maxPlayersRecorded) {
            main.maxPlayersRecorded = currentPlayers
        }

        for (entity in entities) {
            if (entity !is LivingEntity) {
                continue
            }

            // mob must be alive
            if (!entity.isValid()) {
                continue
            }

            // mob must be levelled
            if (!main.levelManager.isLevelled(entity)) {
                continue
            }

            val lmEntity = LivingEntityWrapper.getInstance(entity)

            val nametag = main.levelManager.getNametag(lmEntity, isDeathNametag = false, preserveMobName = false)
            main.levelManager.updateNametag(lmEntity, nametag, mutableListOf(player))
            lmEntity.free()
        }
    }

    private fun parseUpdateChecker(player: Player) {
        if (main.messagesCfg.getBoolean("other.update-notice.send-on-join", true)
            && player.hasPermission("levelledmobs.receive-update-notifications")
        ) {
            main.mainCompanion.updateResult.forEach{ msg ->
                player.sendMessage(MessageUtils.colorizeAll(msg))
            }
        }
    }
}