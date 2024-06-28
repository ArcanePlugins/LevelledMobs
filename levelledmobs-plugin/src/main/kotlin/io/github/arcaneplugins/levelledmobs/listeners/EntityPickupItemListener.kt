package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.misc.PickedUpEquipment
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent

/**
 * Listens for when an entity picks up items and tracks the items only if
 * the entity equipped the items so that later we can be sure not to
 * destroy these items
 *
 * @author stumper66
 * @since 3.14.0
 */
class EntityPickupItemListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityPickupItemEvent(event: EntityPickupItemEvent) {
        // sorry guys this is a Paper only feature
        if (!LevelledMobs.instance.ver.isRunningPaper) return
        if (event.entity is Player) return

        val lmEntity = LivingEntityWrapper.getInstance(event.entity)

        if (!lmEntity.isLevelled || lmEntity.livingEntity.equipment == null) {
            lmEntity.free()
            return
        }

        // if you don't clone the item then it will change to air in the next function
        val itemStack = event.item.itemStack.clone()
        val pickedUpEquipment = PickedUpEquipment(lmEntity)
        val wrapper = SchedulerWrapper(lmEntity.livingEntity) {
            pickedUpEquipment.checkEquipment(itemStack)
            lmEntity.free()
        }

        lmEntity.inUseCount.getAndIncrement()
        wrapper.runDelayed(1L)
    }
}