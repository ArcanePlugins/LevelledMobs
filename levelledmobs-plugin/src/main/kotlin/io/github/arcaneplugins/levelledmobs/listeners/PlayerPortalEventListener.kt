package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.util.Log
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable

/**
 * Provides logic for when a player enters a portal.
 * Used for various spawn distance placeholders
 *
 * @author stumper66
 * @since 3.3.0
 */
class PlayerPortalEventListener : Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onPlayerPortalEvent(event: PlayerPortalEvent) {
        if (event.cause != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            return
        }
        if (event.to.world == null) {
            return
        }
        val isToNether = (event.to.world.environment
                == World.Environment.NETHER)

        val player = event.player
        val main = LevelledMobs.instance

        // store the player's portal coords in the nether.  only used for player levelling
        main.mainCompanion.setPlayerNetherPortalLocation(player, event.to)
        val locationStr = "${event.to.world.name},${event.to.blockX},${event.to.blockY},${event.to.blockZ}"

        val runnable: BukkitRunnable = object : BukkitRunnable() {
            override fun run() {
                if (isToNether) {
                    main.mainCompanion.setPlayerNetherPortalLocation(player, player.location)
                } else {
                    main.mainCompanion.setPlayerWorldPortalLocation(player, player.location)
                }

                try {
                    if (isToNether) {
                        event.player.persistentDataContainer
                            .set(
                                NamespacedKeys.playerNetherCoords, PersistentDataType.STRING,
                                locationStr
                            )
                    } else {
                        event.player.persistentDataContainer
                            .set(
                                NamespacedKeys.playerNetherCoordsIntoWorld,
                                PersistentDataType.STRING, locationStr
                            )
                    }
                } catch (e: ConcurrentModificationException) {
                    Log.war(
                        "Error updating PDC on ${player.name}, ${e.message}"
                    )
                }
            }
        }

        // for some reason event#getTo has different coords that the actual nether portal
        // delay for 1 ticket and grab the player location instead
        runnable.runTaskLater(main, 1L)
    }
}