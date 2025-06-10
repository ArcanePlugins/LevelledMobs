package io.github.arcaneplugins.levelledmobs.listeners

import java.time.Instant
import java.util.UUID
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.customdrops.CustomDropResult
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.result.ChunkKillInfo
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.managers.MobDataManager
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.NametagTimerChecker
import io.github.arcaneplugins.levelledmobs.result.AdjacentChunksResult
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Bukkit
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * Listens for when an entity dies so it's drops can be multiplied, manipulated, etc
 *
 * @author lokka30
 * @since 2.4.0
 */
class EntityDeathListener : Listener {
    val damageMappings = mutableMapOf<UUID, Player>()
    private var lastPriority: EventPriority? = null
    private val settingName = "entity-death-event"

    // These entity types will be forced not to be processed
    private val bypassEntity = mutableListOf(
        EntityType.ARMOR_STAND,EntityType.ITEM_FRAME, EntityType.ITEM_DISPLAY, EntityType.PAINTING)

    fun load(){
        val priority = LevelledMobs.instance.mainCompanion.getEventPriority(settingName, EventPriority.NORMAL)
        if (lastPriority != null){
            if (priority == lastPriority) return

            HandlerList.unregisterAll(this)
            Log.inf("Changing event priority for $settingName from $lastPriority to $priority")
        }

        Bukkit.getPluginManager().registerEvent(
            EntityDeathEvent::class.java,
            this,
            priority,
            { _, event -> if (event is EntityDeathEvent) onDeath(event) },
            LevelledMobs.instance,
            false
        )
        lastPriority = priority
    }

    private fun onDeath(event: EntityDeathEvent) {
        var damagingPlayer: Player? = null
        if (damageMappings.containsKey(event.entity.uniqueId)) {
            damagingPlayer = damageMappings[event.entity.uniqueId]
            damageMappings.remove(event.entity.uniqueId)
        }

        if (event.entity is Player) return
        if (bypassEntity.contains(event.entityType)) return

        val main = LevelledMobs.instance
        val killer = damagingPlayer ?: event.entity.killer

        synchronized(NametagTimerChecker.entityTarget_Lock) {
            main.nametagTimerChecker.entityTargetMap.remove(event.entity)
        }

        val lmEntity = LivingEntityWrapper.getInstance(event.entity)

        lmEntity.associatedPlayer = killer
        val damage = lmEntity.livingEntity.lastDamageCause

        if (damage != null) lmEntity.deathCause = damage.cause

        if (killer != null && main.placeholderApiIntegration != null) {
            main.placeholderApiIntegration!!.putPlayerOrMobDeath(
                killer, lmEntity, false
            )
        }

        var doNotMultiplyDrops = false
        var doNotBoostXp = false
        var disableXpDrops = false

        if (lmEntity.isLevelled && killer != null && main.rulesManager.getMaximumDeathInChunkThreshold(lmEntity) > 0) {
            // Only counts if mob is killed by player

            val opts = main.rulesManager.getRuleUseCustomDropsForMob(lmEntity).chunkKillOptions!!
            val isAnyOptionEnabled = opts.getDisableXpDrops() || opts.getDisableVanillaDrops() || opts.getDisableItemBoost()

            if (isAnyOptionEnabled && hasReachedEntityDeathChunkMax(lmEntity, killer)) {
                if (opts.getDisableVanillaDrops()) {
                    event.drops.clear()
                    disableXpDrops = true
                }
                if (opts.getDisableItemBoost()) doNotMultiplyDrops = true
                if (opts.getDisableXpDrops()) doNotBoostXp = true
            }
        }

        if (lmEntity.pdc.has(NamespacedKeys.lockSettings, PersistentDataType.INTEGER)
            && lmEntity.pdc.has(NamespacedKeys.lockedDropRules, PersistentDataType.STRING)
        ) {
            val lockedDropRules =
                lmEntity.pdc.get(NamespacedKeys.lockedDropRules, PersistentDataType.STRING)
            if (lockedDropRules != null) {
                lmEntity.lockedCustomDrops =
                    lockedDropRules.split(";").toMutableList()
            }
            if (lmEntity.pdc.has(NamespacedKeys.lockedDropRulesOverride, PersistentDataType.INTEGER)) {
                val lockedOverride =
                    lmEntity.pdc.get(NamespacedKeys.lockedDropRulesOverride, PersistentDataType.INTEGER)
                lmEntity.hasLockedDropsOverride = (lockedOverride != null && lockedOverride == 1)
            }
        }

        event.entity.scheduler.run(LevelledMobs.instance, { task ->
            if (lmEntity.isLevelled) {
                // Set levelled item drops
                MobDataManager.populateAttributeCache(lmEntity)
                main.levelManager.setLevelledItemDrops(lmEntity, event.drops, doNotMultiplyDrops)

                // Set levelled exp drops
                if (disableXpDrops)
                    event.droppedExp = 0
                else if (!doNotBoostXp) {
                    if (event.droppedExp > 0)
                        event.droppedExp = main.levelManager.getLevelledExpDrops(lmEntity, event.droppedExp.toDouble())
                }
            } else if (lmEntity.lockedCustomDrops != null || main.rulesManager.getRuleUseCustomDropsForMob(lmEntity).useDrops) {
                val drops = mutableListOf<ItemStack>()
                val result: CustomDropResult = main.customDropsHandler.getCustomItemDrops(
                    lmEntity,
                    drops, false
                )
                if (result.hasOverride)
                    main.levelManager.removeVanillaDrops(lmEntity, event.drops)

                event.drops.addAll(drops)
            }

            lmEntity.free()
        }, null)
    }

    private fun hasReachedEntityDeathChunkMax(
        lmEntity: LivingEntityWrapper,
        player: Player
    ): Boolean {
        val main = LevelledMobs.instance
        val chunkKey: Long = Utils.getChunkKey(lmEntity.location.chunk)
        val pairList: MutableMap<EntityType, ChunkKillInfo> = main.mainCompanion.getorAddPairForSpecifiedChunk(
            chunkKey
        )
        var numberOfEntityDeathInChunk =
            if (pairList.containsKey(lmEntity.entityType)) pairList[lmEntity.entityType]!!.count else 0

        val adjacentChunksResult = getNumberOfEntityDeathsInAdjacentChunks(lmEntity )
        if (adjacentChunksResult != null) {
            numberOfEntityDeathInChunk += adjacentChunksResult.entities
            adjacentChunksResult.chunkKeys.add(chunkKey)
        }

        lmEntity.chunkKillcount = numberOfEntityDeathInChunk
        val maximumDeathInChunkThreshold = main.rulesManager.getMaximumDeathInChunkThreshold(lmEntity)
        val maxCooldownTime = main.rulesManager.getMaxChunkCooldownTime(lmEntity)

        if (numberOfEntityDeathInChunk < maximumDeathInChunkThreshold) {
            val chunkKillInfo = pairList.computeIfAbsent(lmEntity.entityType) { ChunkKillInfo() }
            chunkKillInfo.entityCounts[Instant.now()] = maxCooldownTime
            return false
        }

        if (main.helperSettings.getBoolean("exceed-kill-in-chunk-message",true)
        ) {
            val chunkKeys = adjacentChunksResult?.chunkKeys ?: mutableListOf(chunkKey)
            if (main.mainCompanion.doesUserHaveCooldown(chunkKeys, player.uniqueId))
                return true

            DebugManager.log(DebugType.CHUNK_KILL_COUNT, lmEntity) {
                "${Utils.displayChunkLocation(lmEntity.location)}: player: ${player.name}, reached chunk kill limit, max: $maximumDeathInChunkThreshold"
            }

            val prefix = main.configUtils.prefix
            val msg = main.messagesCfg.getString("other.no-drop-in-chunk")

            if (msg != null)
                player.sendMessage(colorizeAll(msg.replace("%prefix%", prefix)))

            main.mainCompanion.addUserCooldown(chunkKeys, player.uniqueId)
        }

        return true
    }

    private fun getNumberOfEntityDeathsInAdjacentChunks(
        lmEntity: LivingEntityWrapper
    ): AdjacentChunksResult? {
        val adjacentChunksToCheck = LevelledMobs.instance.rulesManager.getAdjacentChunksToCheck(lmEntity)
        if (adjacentChunksToCheck <= 0) {
            return null
        }

        val startingChunk = lmEntity.location.chunk
        val result = AdjacentChunksResult()

        for (x in -adjacentChunksToCheck until adjacentChunksToCheck) {
            for (z in -adjacentChunksToCheck until adjacentChunksToCheck) {
                if (x == 0 && z == 0) {
                    continue
                }

                val chunk = lmEntity.world
                    .getChunkAt(startingChunk.x + x, startingChunk.z + z)
                if (!chunk.isLoaded) {
                    continue
                }

                result.chunkKeys.add(Utils.getChunkKey(chunk))
            }
        }

        val pairLists: List<Map<EntityType, ChunkKillInfo>> = MainCompanion.instance.getorAddPairForSpecifiedChunks(
            result.chunkKeys
        )
        for (pairList in pairLists) {
            result.entities += if (pairList.containsKey(lmEntity.entityType)) pairList[lmEntity.entityType]!!.count else 0
        }

        return result
    }
}