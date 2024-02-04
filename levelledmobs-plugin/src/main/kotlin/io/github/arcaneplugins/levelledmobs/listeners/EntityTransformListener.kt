package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.result.AdditionalLevelInformation
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.enums.LevellableState
import io.github.arcaneplugins.levelledmobs.enums.LevelledMobSpawnReason
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.EntityTransformEvent

/**
 * Listens for when a mob transforms so the applicable rules can be applied
 *
 * @author stumper66
 * @version 2.4.0
 */
class EntityTransformListener : Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onTransform(event: EntityTransformEvent) {
        // is the original entity a living entity
        if (event.entity !is LivingEntity) {
            DebugManager.log(DebugType.ENTITY_MISC, event.entity, false) {
                event.entity.type.name +
                        ": entity was &bnot&7 an instance of LivingEntity"
            }
            return
        }

        val main = LevelledMobs.instance
        // is the original entity levelled
        if (!main.levelManager.isLevelled(event.entity as LivingEntity)) {
            DebugManager.log(DebugType.ENTITY_MISC, event.entity, false) {
                event.entity.name + ": original entity was &bnot&7 levelled"
            }
            if (event.transformReason == EntityTransformEvent.TransformReason.SPLIT)
                checkForSlimeSplit(event.entity as LivingEntity,event.transformedEntities)
            return
        }

        val lmEntity = LivingEntityWrapper.getInstance(event.entity as LivingEntity)

        var useInheritance = false
        var level = 1

        if (main.rulesManager.getRuleMobLevelInheritance(lmEntity)) {
            useInheritance = true
            level = lmEntity.getMobLevel
        }

        for (transformedEntity in event.transformedEntities) {
            if (transformedEntity !is LivingEntity) {
                DebugManager.log(DebugType.ENTITY_MISC, event.entity, false) {
                    (event.entity.type.name
                            + ": entity was&b not&7 an instance of LivingEntity (loop)")
                }
                continue
            }

            val transformedLmEntity = LivingEntityWrapper.getInstance(transformedEntity)

            val levelledState: LevellableState = main.levelInterface.getLevellableState(
                transformedLmEntity
            )
            if (levelledState != LevellableState.ALLOWED) {
                DebugManager.log(DebugType.ENTITY_MISC, event.entity, false) {
                    (transformedEntity.getType().name
                            + ": transformed entity was &bnot&7 levellable, reason: &b"
                            + levelledState)
                }
                main.levelManager.updateNametagWithDelay(transformedLmEntity)
                transformedLmEntity.free()
                continue
            }

            DebugManager.log(
                DebugType.ENTITY_MISC,
                event.entity,
                true
            ) { transformedEntity.getType().name + ": entity was transformed" }

            if (useInheritance) {
                if (lmEntity.spawnReason == LevelledMobSpawnReason.LM_SPAWNER) {
                    transformedLmEntity.spawnReason = LevelledMobSpawnReason.SPAWNER
                }

                main.levelInterface.applyLevelToMob(
                    transformedLmEntity,
                    level,
                    isSummoned = false,
                    bypassLimits = false,
                    additionalLevelInformation = mutableSetOf(AdditionalLevelInformation.FROM_TRANSFORM_LISTENER)
                )
            } else {
                main.levelManager.entitySpawnListener.preprocessMob(
                    transformedLmEntity,
                    EntitySpawnEvent(transformedEntity)
                )
            }

            main.levelManager.updateNametagWithDelay(lmEntity)
            transformedLmEntity.free()
        }

        lmEntity.free()
    }

    private fun checkForSlimeSplit(livingEntity: LivingEntity, transformedEntities: List<Entity>) {
        val parent = LivingEntityWrapper.getInstance(livingEntity)
        if (parent.spawnReason == LevelledMobSpawnReason.DEFAULT ||
            parent.spawnReason == LevelledMobSpawnReason.SLIME_SPLIT
        ) {
            parent.free()
            return
        }

        for (transformedEntity in transformedEntities) {
            if (transformedEntity !is LivingEntity) continue

            val lew = LivingEntityWrapper.getInstance(transformedEntity)
            lew.spawnReason = parent.spawnReason
            lew.free()
        }

        parent.free()
    }
}