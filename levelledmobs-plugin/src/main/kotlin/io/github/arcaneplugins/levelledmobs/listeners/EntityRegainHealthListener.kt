package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRegainHealthEvent

/**
 * Listens for when an entity regains health so the nametag can be updated accordingly
 *
 * @author konsolas, lokka30
 * @since 2.4.0
 */
class EntityRegainHealthListener : Listener {
    // When the mob regains health, try to update their nametag.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onEntityRegainHealth(event: EntityRegainHealthEvent) {
        if (event.entity !is LivingEntity) {
            return
        }

        // Make sure the mob is levelled
        if (!LevelledMobs.instance.levelManager.isLevelled(event.entity as LivingEntity)) {
            return
        }

        val lmEntity = LivingEntityWrapper.getInstance((event.entity as LivingEntity))

        LevelledMobs.instance.levelManager.updateNametagWithDelay(lmEntity)
        lmEntity.free()
    }
}