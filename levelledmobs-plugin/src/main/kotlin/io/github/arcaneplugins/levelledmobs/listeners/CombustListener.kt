package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustByBlockEvent
import org.bukkit.event.entity.EntityCombustByEntityEvent
import org.bukkit.event.entity.EntityCombustEvent

/**
 * Listens for when an entity combusts for the purpose of increasing sunlight damage if desired
 *
 * @author stumper66
 * @since 2.4.0
 */
class CombustListener: Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onCombust(event: EntityCombustEvent) {
        if (event is EntityCombustByBlockEvent
            || event is EntityCombustByEntityEvent
        ) {
            return
        }

        if (event.entity.world.environment == World.Environment.NETHER ||
            event.entity.world.environment == World.Environment.THE_END
        ) {
            return
        }

        val entityTypesCanBurnInSunlight2 = mutableListOf(
            EntityType.SKELETON, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.STRAY,
            EntityType.DROWNED, EntityType.PHANTOM
        )
        if (!entityTypesCanBurnInSunlight2.contains(event.entity.type)) {
            return
        }

        if (event.entity is LivingEntity) {
            val equipment = (event.entity as LivingEntity).equipment

            if (equipment != null && equipment.helmet != null && equipment.helmet.type != Material.AIR) {
                return
            }
        }

        val lmEntity = LivingEntityWrapper.getInstance(event.entity as LivingEntity)
        val multiplier = LevelledMobs.instance.rulesManager.getRuleSunlightBurnIntensity(lmEntity)
        if (multiplier == 0.0) {
            lmEntity.free()
            return
        }

        var newHealth = lmEntity.livingEntity.health - multiplier
        if (newHealth < 0.0) {
            newHealth = 0.0
        }

        if (lmEntity.livingEntity.health <= 0.0) {
            lmEntity.free()
            return
        }
        lmEntity.livingEntity.health = newHealth

        if (lmEntity.isLevelled) {
            LevelledMobs.instance.levelManager.updateNametag(lmEntity)
        }

        lmEntity.free()
    }
}