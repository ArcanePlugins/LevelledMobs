package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent

/**
 * Listens when a nametag is placed on an entity so LevelledMobs can apply various rules around
 * nametagged entities
 *
 * @author lokka30
 * @since 2.4.0
 */
class EntityNametagListener: Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onNametag(event: PlayerInteractEntityEvent) {
        if (event.rightClicked !is LivingEntity) {
            return
        }
        val player = event.player

        // Must have name tag in main hand / off-hand
        if (!(player.inventory.itemInMainHand.type == Material.NAME_TAG
                    || player.inventory.itemInOffHand.type == Material.NAME_TAG)
        ) {
            return
        }

        val main = LevelledMobs.instance
        // Must be a levelled mob
        if (!main.levelManager.isLevelled(event.rightClicked as LivingEntity)) {
            return
        }

        val lmEntity = LivingEntityWrapper.getInstance(event.rightClicked as LivingEntity)
        val level = main.rulesManager.getRuleMobMaxLevel(lmEntity)
        val mobLevel = lmEntity.mobLevel ?: 0

        if (level <= 0 && mobLevel > 0) {
            main.levelInterface.removeLevel(lmEntity)
            lmEntity.free()
            return
        }

        main.levelManager.updateNametag(lmEntity)
        lmEntity.free()

    }
}