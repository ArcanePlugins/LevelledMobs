package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.enums.InternalSpawnReason
import io.github.arcaneplugins.levelledmobs.enums.LevellableState
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.managers.LevelManager
import io.github.arcaneplugins.levelledmobs.managers.MobDataManager
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.QueueItem
import io.github.arcaneplugins.levelledmobs.result.AdditionalLevelInformation
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.block.CreatureSpawner
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.SpawnerSpawnEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.persistence.PersistentDataType

/**
 * This class handles mob spawning on the server, forming the starting point of the 'levelling'
 * process
 *
 * @author lokka30
 * @version 2.5.0
 */
class EntitySpawnListener : Listener{
    var processMobSpawns = true

    init {
        instance = this
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onEntitySpawn(event: EntitySpawnEvent) {
        if (event.entity !is LivingEntity) return

        val main = LevelledMobs.instance
        val lmEntity = LivingEntityWrapper.getInstance(event.entity as LivingEntity)
        lmEntity.skylightLevel = lmEntity.currentSkyLightLevel
        lmEntity.isNewlySpawned = true
        lmEntity.populateShowShowLMNametag()
        var hasBuiltCache = false

        if (event is CreatureSpawnEvent) {
            val spawnReason = event.spawnReason
            lmEntity.spawnReason.setMinecraftSpawnReason(lmEntity, spawnReason)

            lmEntity.buildCacheIfNeeded()
            MobDataManager.populateAttributeCache(lmEntity, null)
            hasBuiltCache = true

            if ((spawnReason == SpawnReason.CUSTOM || spawnReason == SpawnReason.SPAWNER_EGG) &&
                !lmEntity.isLevelled
            ) {
                if (main.rulesManager.isPlayerLevellingEnabled()
                    && lmEntity.playerForLevelling == null
                ) {
                    updateMobForPlayerLevelling(lmEntity)
                }

                delayedAddToQueue(lmEntity, event, 20)
                lmEntity.free()
                return
            }
        } else if (event is SpawnerSpawnEvent)
            lmEntity.spawnReason.setMinecraftSpawnReason(lmEntity, SpawnReason.SPAWNER)

        if (!processMobSpawns) {
            lmEntity.free()
            return
        }

        if (!hasBuiltCache) {
            lmEntity.buildCacheIfNeeded()
            MobDataManager.populateAttributeCache(lmEntity, null)
        }

        if (main.rulesManager.isPlayerLevellingEnabled() && lmEntity.playerForLevelling == null)
            updateMobForPlayerLevelling(lmEntity)

        val mobProcessDelay = main.helperSettings.getInt("mob-process-delay", 0)

        if (mobProcessDelay > 0)
            delayedAddToQueue(lmEntity, event, mobProcessDelay)
        else
            main.mobsQueueManager.addToQueue(QueueItem(lmEntity, event))

        lmEntity.free()
    }

    private fun delayedAddToQueue(
        lmEntity: LivingEntityWrapper,
        event: Event,
        delay: Int
    ) {
        val scheduler = SchedulerWrapper {
            LevelledMobs.instance.mobsQueueManager.addToQueue(QueueItem(lmEntity, event))
            lmEntity.free()
        }

        lmEntity.inUseCount.getAndIncrement()
        scheduler.runDelayed(delay.toLong())
    }

    private fun lmSpawnerSpawn(
        lmEntity: LivingEntityWrapper,
        event: SpawnerSpawnEvent
    ) {
        val cs = event.spawner
        val main = LevelledMobs.instance

        if (cs == null) return
        // mob was spawned from a custom LM spawner
        val useParticle = main.rulesManager.getSpawnerParticle(lmEntity)
        val particleCount = main.rulesManager.getSpawnerParticleCount(lmEntity)

        if (useParticle != null && particleCount > 0) {
            createParticleEffect(cs.location.add(0.5, 1.0, 0.5), useParticle, particleCount)
        }

        val minLevel = cs.persistentDataContainer
            .get(NamespacedKeys.keySpawnerMinLevel, PersistentDataType.INTEGER)
        val maxLevel = cs.persistentDataContainer
            .get(NamespacedKeys.keySpawnerMaxLevel, PersistentDataType.INTEGER)
        val useMinLevel = minLevel ?: -1
        val useMaxLevel = maxLevel ?: -1
        val generatedLevel = main.levelInterface.generateLevel(
            lmEntity, useMinLevel,
            useMaxLevel
        )
        val spawnerName = if (cs.persistentDataContainer
                .has(NamespacedKeys.keySpawnerCustomName, PersistentDataType.STRING)
        ) cs.persistentDataContainer
            .get(NamespacedKeys.keySpawnerCustomName, PersistentDataType.STRING) else null
        var customDropId: String? = null
        if (cs.persistentDataContainer
                .has(NamespacedKeys.keySpawnerCustomDropId, PersistentDataType.STRING)
        ) {
            customDropId = cs.persistentDataContainer
                .get(NamespacedKeys.keySpawnerCustomDropId, PersistentDataType.STRING)
            if (!customDropId.isNullOrEmpty()) {
                synchronized(lmEntity.livingEntity.persistentDataContainer) {
                    lmEntity.pdc.set(
                        NamespacedKeys.keySpawnerCustomDropId,
                        PersistentDataType.STRING, customDropId
                    )
                }
            }
        }

        lmEntity.sourceSpawnerName = spawnerName
        lmEntity.spawnReason.setInternalSpawnReason(lmEntity, InternalSpawnReason.LM_SPAWNER, true)

        val customDropIdFinal = customDropId
        DebugManager.log(DebugType.LM_MOB_SPAWNER, lmEntity) {
            val msg = (if (customDropIdFinal == null) "" else ", dropid: $customDropIdFinal")
            "Spawned mob from LM spawner: minLevel:&b $useMinLevel&7, maxLevel: &b$useMaxLevel&7, generatedLevel: &b$generatedLevel&b$msg"
        }

        main.levelInterface.applyLevelToMob(
            lmEntity, generatedLevel,
            isSummoned = false, bypassLimits = true,
            additionalLevelInformation = mutableSetOf(AdditionalLevelInformation.NOT_APPLICABLE)
        )
    }

    private fun createParticleEffect(
        location: Location,
        particle: Particle,
        count: Int
    ) {
        val world = location.world ?: return

        val scheduler = SchedulerWrapper {
            try {
                for (i in 0 until count) {
                    world.spawnParticle(particle, location, 20, 0.0, 0.0, 0.0, 0.1)
                    Thread.sleep(50)
                }
            } catch (ignored: InterruptedException) { }
        }
        scheduler.run()
    }

    fun preprocessMob(
        lmEntity: LivingEntityWrapper,
        event: Event?
    ) {
        if (!lmEntity.reEvaluateLevel && lmEntity.isLevelled) {
            return
        }
        if (!lmEntity.isPopulated) {
            return
        }

        val main = LevelledMobs.instance
        var additionalInfo = AdditionalLevelInformation.NOT_APPLICABLE

        lmEntity.spawnedTimeOfDay = lmEntity.world.time.toInt()

        if (event is SpawnerSpawnEvent) {
            // on spigot servers event.spawner can be null
            if (event.spawner != null && (event.spawner as CreatureSpawner)
                    .persistentDataContainer
                    .has(NamespacedKeys.keySpawner, PersistentDataType.INTEGER)
            ) {
                lmEntity.spawnReason.setInternalSpawnReason(lmEntity, InternalSpawnReason.LM_SPAWNER, true)
                lmSpawnerSpawn(lmEntity, event)
                return
            }

            DebugManager.log(
                DebugType.LM_MOB_SPAWNER,
                lmEntity
            ) { "Spawned mob from vanilla spawner" }
        } else if (event is CreatureSpawnEvent) {
            if (event.spawnReason == SpawnReason.SPAWNER ||
                event.spawnReason == SpawnReason.SLIME_SPLIT
            ) {
                return
            }

            if (event.spawnReason == SpawnReason.CUSTOM ||
                event.spawnReason == SpawnReason.SPAWNER_EGG
            ) {
                synchronized(LevelManager.summonedOrSpawnEggs_Lock) {
                    if (main.levelManager.summonedOrSpawnEggs.containsKey(
                            lmEntity.livingEntity
                        )
                    ) {
                        // the mob was spawned by the summon command and will get processed directly
                        return
                    }
                }
            }

            if (!lmEntity.reEvaluateLevel)
                lmEntity.spawnReason.setMinecraftSpawnReason(lmEntity, event.spawnReason)
        } else if (event is ChunkLoadEvent)
            additionalInfo = AdditionalLevelInformation.FROM_CHUNK_LISTENER

        if (lmEntity.reEvaluateLevel && main.rulesManager.isPlayerLevellingEnabled()
            && lmEntity.isRulesForceAll
        ) {
            synchronized(lmEntity.livingEntity.persistentDataContainer) {
                if (lmEntity.pdc
                        .has(NamespacedKeys.playerLevellingId, PersistentDataType.STRING)
                ) {
                    lmEntity.pdc.remove(NamespacedKeys.playerLevellingId)
                }
            }
        }

        val additionalLevelInfo = mutableSetOf(additionalInfo)
        val levellableState = getLevellableState(lmEntity, event)
        if (levellableState == LevellableState.ALLOWED) {
            val levelAssignment = main.levelInterface.generateLevel(lmEntity)
            if (shouldDenyLevel(lmEntity, levelAssignment)) {
                DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                    "denied relevelling to &b$levelAssignment&r due to decrease-level disabled"
                }
            } else {
                if (lmEntity.reEvaluateLevel && main.rulesManager.isPlayerLevellingEnabled()) {
                    val scheduler = SchedulerWrapper(lmEntity.livingEntity){
                        updateMobForPlayerLevelling(lmEntity)
                        lmEntity.free()
                    }
                    scheduler.runDirectlyInBukkit = true
                    lmEntity.inUseCount.getAndIncrement()
                    scheduler.run()
                }

                main.levelInterface.applyLevelToMob(
                    lmEntity, levelAssignment,
                    isSummoned = false, bypassLimits = false, additionalLevelInformation = additionalLevelInfo
                )
            }
        } else {
            DebugManager.log(DebugType.APPLY_LEVEL_RESULT, lmEntity, false) {
                ("world: &b${lmEntity.worldName}&7 was not levelled -> levellable state: &b${levellableState}")
            }

            // Check if the mob is already levelled - if so, remove their level
            if (lmEntity.isLevelled) {
                main.levelInterface.removeLevel(lmEntity)
            } else if (lmEntity.isBabyMob) {
                // add a tag so we can potentially level the mob when/if it ages
                synchronized(lmEntity.livingEntity.persistentDataContainer) {
                    lmEntity.pdc
                        .set(NamespacedKeys.wasBabyMobKey, PersistentDataType.INTEGER, 1)
                }
            }

            if (lmEntity.wasPreviouslyLevelled) {
                main.levelManager.updateNametag(lmEntity)
            }
        }
    }

    private fun shouldDenyLevel(
        lmEntity: LivingEntityWrapper,
        levelAssignment: Int
    ): Boolean {
        var result =
            lmEntity.reEvaluateLevel &&
                    !lmEntity.isRulesForceAll && lmEntity.playerLevellingAllowDecrease != null &&
                    !lmEntity.playerLevellingAllowDecrease!! &&
                    lmEntity.isLevelled && levelAssignment < lmEntity.getMobLevel

        if (result) {
            synchronized(lmEntity.livingEntity.persistentDataContainer) {
                result = lmEntity.pdc
                    .has(
                        NamespacedKeys.playerLevellingId,
                        PersistentDataType.STRING
                    )
            }
        }

        if (!result && lmEntity.pendingPlayerIdToSet != null) {
            synchronized(lmEntity.livingEntity.persistentDataContainer) {
                lmEntity.pdc.set(
                    NamespacedKeys.playerLevellingId,
                    PersistentDataType.STRING, lmEntity.pendingPlayerIdToSet!!
                )
            }
        }

        return result
    }

    private fun getLevellableState(
        lmEntity: LivingEntityWrapper,
        event: Event?
    ): LevellableState {
        val levellableState = LevelledMobs.instance.levelInterface.getLevellableState(lmEntity)

        if (levellableState != LevellableState.ALLOWED)
            return levellableState

        if (event is CreatureSpawnEvent) {
            // the mob gets processed via SpawnerSpawnEvent

            if (event.spawnReason == SpawnReason.SPAWNER)
                return LevellableState.DENIED_OTHER

            DebugManager.log(DebugType.ENTITY_SPAWN, lmEntity) {
                "instanceof CreatureSpawnListener: &b${event.entityType}" +
                        "&7, with spawnReason &b${event.spawnReason}&7."
            }
        } else if (event is EntitySpawnEvent) {
            DebugManager.log(DebugType.ENTITY_SPAWN, lmEntity) {
                "not instanceof CreatureSpawnListener: &b${event.entityType}"
            }
        }

        return LevellableState.ALLOWED
    }

    companion object{
        @JvmStatic
        lateinit var instance: EntitySpawnListener
            private set

        fun updateMobForPlayerLevelling(lmEntity: LivingEntityWrapper) {
            val onlinePlayerCount = lmEntity.world.players.size
            val checkDistance = LevelledMobs.instance.helperSettings.getInt(
                "async-task-max-blocks-from-player", 100
            )

            if (LevelledMobs.instance.ver.isRunningFolia || Bukkit.isPrimaryThread()){
                // run directly if we're in the main thread already
                updateMobForPlayerLevellingNonAsync(
                    lmEntity,
                    checkDistance,
                    onlinePlayerCount
                )
                return
            }

            val wrapper = SchedulerWrapper(lmEntity.livingEntity){
                updateMobForPlayerLevellingNonAsync(
                    lmEntity,
                    checkDistance,
                    onlinePlayerCount
                )
                lmEntity.free()
            }

            lmEntity.inUseCount.getAndIncrement()
            wrapper.run()
        }

        private fun updateMobForPlayerLevellingNonAsync(
            lmEntity: LivingEntityWrapper,
            checkDistance: Int,
            onlinePlayerCount: Int
        ){
            val main = LevelledMobs.instance
            val playerList: MutableList<Player> = if (onlinePlayerCount <= 10) getPlayersOnServerNearMob(
                lmEntity.livingEntity,
                checkDistance
            ) else getPlayersNearMob(lmEntity.livingEntity, checkDistance)

            var closestPlayer: Player? = null
            for (player in playerList) {
                if (ExternalCompatibilityManager.isMobOfCitizens(player)) continue

                closestPlayer = player
                break
            }

            if (closestPlayer == null) return

            // if player has been logged in for less than 5 seconds then ignore
            val logonTime = main.mainCompanion.getRecentlyJoinedPlayerLogonTime(closestPlayer)
            if (logonTime != null) {
                if (Utils.getMillisecondsFromInstant(logonTime) < 5000L) return

                main.mainCompanion.removeRecentlyJoinedPlayer(closestPlayer)
            }

            lmEntity.pdc.set(
                NamespacedKeys.playerLevellingId, PersistentDataType.STRING,
                closestPlayer.uniqueId.toString()
            )

            lmEntity.playerForLevelling = closestPlayer
            val nametagVisibilityEnums = lmEntity.nametagVisibilityEnum
            if (nametagVisibilityEnums.contains(NametagVisibilityEnum.TARGETED) &&
                lmEntity.livingEntity.hasLineOfSight(closestPlayer)
            ) {
                main.levelManager.updateNametag(lmEntity)
            }
        }

        private fun getPlayersOnServerNearMob(
            mob: LivingEntity,
            checkDistance: Int
        ): MutableList<Player> {
            return Utils.filterPlayersList(
                mob.world.players,
                mob,
                (checkDistance * 4).toDouble()
            )
        }

        private fun getPlayersNearMob(
            mob: LivingEntity,
            checkDistance: Int
        ): MutableList<Player> {
            var temp = mob.getNearbyEntities(checkDistance.toDouble(), checkDistance.toDouble(), checkDistance.toDouble()
            ).asSequence()
                .filterIsInstance<Player>()
                .filter { e: Entity -> (e as Player).gameMode != GameMode.SPECTATOR }
                .map { e: Entity -> Pair(mob.location.distanceSquared(e.location), e as Player) }
                .sortedBy { it.first }
                .map { it.second }

            if (MainCompanion.instance.excludePlayersInCreative){
                temp = temp.filter { e: Entity -> (e as Player).gameMode != GameMode.CREATIVE }
            }

            return temp.toMutableList()
        }
    }
}