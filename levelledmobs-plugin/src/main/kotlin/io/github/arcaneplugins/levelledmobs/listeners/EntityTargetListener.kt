package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.misc.NametagTimerChecker
import io.github.arcaneplugins.levelledmobs.misc.QueueItem
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTargetEvent

/**
 * Used as a workaround to ensure mob nametags are properly updated
 *
 * @author stumper66
 * @since 2.4.0
 */
class EntityTargetListener : Listener {
    /**
     * This event is listened to update the nametag of a mob when they start targeting a player.
     * Should provide another band-aid for packets not appearing sometimes for mob nametags.
     *
     * @param event EntityTargetEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onTarget(event: EntityTargetEvent) {
        if (event.entity !is LivingEntity)
            return

        val main = LevelledMobs.instance
        if (event.target == null) {
            synchronized(NametagTimerChecker.entityTarget_Lock) {
                main.nametagTimerChecker.entityTargetMap.remove(event.entity as LivingEntity)
            }
            return
        }

        // Must target a player and must be a living entity
        if (event.target !is Player)
            return

        val lmEntity = LivingEntityWrapper.getInstance(event.entity as LivingEntity)

        // Must be a levelled entity
        if (!lmEntity.isLevelled) {
            if (EntitySpawnListener.instance.processMobSpawns) {
                lmEntity.free()
                return
            }

            if (lmEntity.getMobLevel < 0)
                lmEntity.reEvaluateLevel = true

            main.mobsQueueManager.addToQueue(QueueItem(lmEntity, event))
            return
        }

        if (lmEntity.nametagVisibilityEnum.contains(NametagVisibilityEnum.TRACKING)) {
            synchronized(NametagTimerChecker.entityTarget_Lock) {
                main.nametagTimerChecker.entityTargetMap.put(
                    lmEntity.livingEntity,
                    event.target as Player?
                )
            }
        }

        // Update the nametag.
        main.levelManager.updateNametag(lmEntity)
        lmEntity.free()
    }
}