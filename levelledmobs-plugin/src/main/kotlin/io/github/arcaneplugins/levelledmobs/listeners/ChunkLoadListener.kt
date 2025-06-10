package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.misc.QueueItem
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent

/**
 * Listens for when chunks are loaded and processes any mobs accordingly Needed for server startup
 * and for mostly passive mobs when players are moving around
 *
 * @author stumper66
 * @since 2.4.0
 */
class ChunkLoadListener : Listener {
    private var ensureMobsAreLevelledOnChunkLoad = true

    fun load(){
        ensureMobsAreLevelledOnChunkLoad = LevelledMobs.instance.helperSettings.getBoolean(
            "ensure-mobs-are-levelled-on-chunk-load", true
        )
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onChunkLoad(event: ChunkLoadEvent) {
        if (!ensureMobsAreLevelledOnChunkLoad) return

        // Check each entity in the chunk
        for (entity in event.chunk.entities) {
            // Must be a *living* entity

            if (entity !is LivingEntity) {
                continue
            }

            checkEntity(entity, event)
        }
    }

    private fun checkEntity(livingEntity: LivingEntity, event: ChunkLoadEvent) {
        val lmEntity = LivingEntityWrapper.getInstance(livingEntity)
        livingEntity.scheduler.run(LevelledMobs.instance, { task ->
            val wrapper = SchedulerWrapper(livingEntity) {
                if (LevelledMobs.instance.levelManager.doCheckMobHash && Utils.checkIfMobHashChanged(lmEntity)) {
                    lmEntity.reEvaluateLevel = true
                    lmEntity.isRulesForceAll = true
                    lmEntity.wasPreviouslyLevelled = lmEntity.isLevelled
                } else if (lmEntity.isLevelled) {
                    lmEntity.free()
                    return@SchedulerWrapper
                }

                LevelledMobs.instance.mobsQueueManager.addToQueue(QueueItem(lmEntity, event))
                lmEntity.free()
            }

            lmEntity.buildCacheIfNeeded()
            lmEntity.inUseCount.getAndIncrement()
            wrapper.runDirectlyInBukkit = true
            wrapper.run()
        }, null)
    }
}