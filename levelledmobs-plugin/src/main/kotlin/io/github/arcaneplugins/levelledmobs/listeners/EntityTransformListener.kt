package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.result.AdditionalLevelInformation
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.enums.InternalSpawnReason
import io.github.arcaneplugins.levelledmobs.enums.LevellableState
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.EntityTransformEvent

/**
 * Listens for when a mob transforms so the applicable rules can be applied
 *
 * @author stumper66
 * @version 2.4.0
 */
class EntityTransformListener : Listener {
    val damageMappings = mutableMapOf<UUID, Player>()
    private var lastPriority: EventPriority? = null
    private val settingName = "entity-transform-event"

    companion object {
        @JvmStatic
        lateinit var instance: EntityTransformListener
            private set
    }

    init {
        instance = this
    }

    fun load(){
        val priority = LevelledMobs.instance.mainCompanion.getEventPriority(settingName, EventPriority.MONITOR)
        if (lastPriority != null){
            if (priority == lastPriority) return

            HandlerList.unregisterAll(this)
            Log.inf("Changing event priority for $settingName from $lastPriority to $priority")
        }

        Bukkit.getPluginManager().registerEvent(
            EntityTransformEvent::class.java,
            this,
            priority,
            { _, event -> if (event is EntityTransformEvent) onTransform(event) },
            LevelledMobs.instance,
            false
        )
        lastPriority = priority
    }

    private fun onTransform(event: EntityTransformEvent) {
        // is the original entity a living entity
        if (event.entity !is LivingEntity) {
            DebugManager.log(DebugType.ENTITY_MISC, event.entity, false) {
                "entity was &bnot&7 an instance of LivingEntity"
            }
            return
        }

        val main = LevelledMobs.instance
        // is the original entity levelled
        if (!main.levelManager.isLevelled(event.entity as LivingEntity)) {
            DebugManager.log(DebugType.ENTITY_MISC, event.entity, false) {
                "original entity was &bnot&7 levelled"
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
                    "entity was&b not&7 an instance of LivingEntity (loop)"
                }
                continue
            }

            val transformedLmEntity = LivingEntityWrapper.getInstance(transformedEntity)

            val levelledState: LevellableState = main.levelInterface.getLevellableState(
                transformedLmEntity
            )
            if (levelledState != LevellableState.ALLOWED) {
                DebugManager.log(DebugType.ENTITY_MISC, event.entity, false) {
                    "transformed entity was &bnot&7 levellable, reason: &b$levelledState"
                }
                main.levelManager.updateNametagWithDelay(transformedLmEntity)
                transformedLmEntity.free()
                continue
            }

            DebugManager.log(
                DebugType.ENTITY_MISC,
                transformedEntity,
                true
            ) { "entity was transformed" }

            if (useInheritance) {
                val internalSpawnReason = lmEntity.spawnReason.getInternalSpawnReason(lmEntity)
                if (internalSpawnReason == InternalSpawnReason.LM_SPAWNER)
                    transformedLmEntity.spawnReason.setMinecraftSpawnReason(lmEntity, CreatureSpawnEvent.SpawnReason.SPAWNER)

                main.levelInterface.applyLevelToMob(
                    transformedLmEntity,
                    level,
                    isSummoned = false,
                    bypassLimits = false,
                    additionalLevelInformation = mutableSetOf(AdditionalLevelInformation.FROM_TRANSFORM_LISTENER)
                )
            } else {
                main.levelManager.entitySpawnListener.processMob(
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
        val minecraftSpawnReason = parent.spawnReason.getMinecraftSpawnReason(parent)
        if (minecraftSpawnReason == CreatureSpawnEvent.SpawnReason.DEFAULT ||
            minecraftSpawnReason == CreatureSpawnEvent.SpawnReason.SLIME_SPLIT
        ) {
            parent.free()
            return
        }

        for (transformedEntity in transformedEntities) {
            if (transformedEntity !is LivingEntity) continue

            val lew = LivingEntityWrapper.getInstance(transformedEntity)
            lew.spawnReason.setLMSpawnReason(parent)
            lew.free()
        }

        parent.free()
    }
}