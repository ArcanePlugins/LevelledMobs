package io.github.arcaneplugins.levelledmobs.managers

import java.time.Instant
import java.util.WeakHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.misc.NametagTimerChecker
import io.github.arcaneplugins.levelledmobs.misc.QueueItem
import io.github.arcaneplugins.levelledmobs.nametag.NametagSender
import io.github.arcaneplugins.levelledmobs.nametag.NametagSenderHandler
import io.github.arcaneplugins.levelledmobs.result.NametagResult
import io.github.arcaneplugins.levelledmobs.rules.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.util.LibsDisguisesUtils
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.persistence.PersistentDataType

/**
 * Queues up mob nametag updates so they can be applied in a background thread
 *
 * @author stumper66
 * @since 3.0.0
 */
class NametagQueueManager {
    private var isRunning = false
    private var doThread = false
    private var nametagSender: NametagSender? = null
    private var hasLibsDisguisesInstalled = false
    private val queue = LinkedBlockingQueue<QueueItem>()
    val nametagSenderHandler = NametagSenderHandler()

    fun load(){
        hasLibsDisguisesInstalled = ExternalCompatibilityManager.hasLibsDisguisesInstalled()
        this.nametagSender = nametagSenderHandler.getCurrentUtil()
    }

    val hasNametagSupport: Boolean
        get() = this.nametagSender != null

    fun start() {
        if (isRunning) {
            return
        }
        doThread = true
        isRunning = true

        val scheduler = SchedulerWrapper {
            try {
                mainThread()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            Utils.logger.info("Nametag update queue Manager has exited")
        }
        scheduler.run()
    }

    fun stop() {
        doThread = false
    }

    fun addToQueue(item: QueueItem) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return
        }

        if (LevelledMobs.instance.rulesManager.getRuleCreatureNametagVisbility(item.lmEntity)
            .contains(NametagVisibilityEnum.DISABLED)
        ) {
            return
        }

        item.lmEntity.inUseCount.getAndIncrement()
        queue.offer(item)
    }

    @Throws(InterruptedException::class)
    private fun mainThread() {
        while (doThread) {
            val item = queue.poll(200, TimeUnit.MILLISECONDS) ?: continue

            val scheduler = SchedulerWrapper(
                item.lmEntity.livingEntity
            ) {
                preProcessItem(item)
                item.lmEntity.free()
            }

            scheduler.runDirectlyInBukkit = true
            scheduler.entity = item.lmEntity.livingEntity
            item.lmEntity.inUseCount.getAndIncrement()
            scheduler.run()
        }

        isRunning = false
    }

    private fun preProcessItem(item: QueueItem) {
        if (!item.lmEntity.isPopulated) {
            item.lmEntity.free()
            return
        }

        var lastEntityType: String? = null
        try {
            lastEntityType = item.lmEntity.nameIfBaby
            processItem(item)
        } catch (ex: Exception) {
            val entityName = lastEntityType ?: "Unknown Entity"

            Utils.logger.error("Unable to process nametag update for '$entityName'. ")
            ex.printStackTrace()
        } finally {
            item.lmEntity.free()
        }
    }

    private fun processItem(item: QueueItem) {
        if (this.nametagSender == null) {
            // this would happen if the Minecraft version isn't supported directly by NMS
            // and ProtocolLib is not installed
            return
        }

        val nametagTimerResetTime = item.lmEntity.getNametagCooldownTime()

        if (nametagTimerResetTime > 0L && !item.nametag!!.isNullOrEmpty) {
            synchronized(NametagTimerChecker.nametagTimer_Lock) {
                val nametagCooldownQueue: Map<Player, WeakHashMap<LivingEntity, Instant>> =
                    LevelledMobs.instance.nametagTimerChecker.nametagCooldownQueue
                if (item.lmEntity.playersNeedingNametagCooldownUpdate != null) {
                    // record which players should get the cooldown for this mob
                    // public Map<Player, WeakHashMap<LivingEntity, Instant>> nametagCooldownQueue;
                    for (player in item.lmEntity.playersNeedingNametagCooldownUpdate!!) {
                        if (!nametagCooldownQueue.containsKey(player)) {
                            continue
                        }

                        nametagCooldownQueue[player]?.set(item.lmEntity.livingEntity, Instant.now())
                        LevelledMobs.instance.nametagTimerChecker.cooldownTimes[item.lmEntity.livingEntity] =
                            item.lmEntity.getNametagCooldownTime()
                    }

                    // if any players already have a cooldown on this mob then don't remove the cooldown
                    for ((player, value) in nametagCooldownQueue) {
                        if (item.lmEntity.playersNeedingNametagCooldownUpdate!!.contains(player)) {
                            continue
                        }

                        if (value.containsKey(item.lmEntity.livingEntity)) {
                            item.lmEntity.playersNeedingNametagCooldownUpdate!!.add(player)
                        }
                    }
                } else {
                    // if there's any existing cooldowns we'll use them
                    for ((key, value) in nametagCooldownQueue) {
                        if (value.containsKey(item.lmEntity.livingEntity)) {
                            if (item.lmEntity.playersNeedingNametagCooldownUpdate == null) {
                                item.lmEntity.playersNeedingNametagCooldownUpdate = HashSet()
                            }

                            item.lmEntity.playersNeedingNametagCooldownUpdate!!.add(
                                key
                            )
                        }
                    }
                }
            }
        } else if (item.lmEntity.playersNeedingNametagCooldownUpdate != null) {
            item.lmEntity.playersNeedingNametagCooldownUpdate = null
        }

        val main = LevelledMobs.instance
        synchronized(NametagTimerChecker.entityTarget_Lock) {
            if (main.nametagTimerChecker.entityTargetMap.containsKey(
                    item.lmEntity.livingEntity
                )
            ) {
                if (item.lmEntity.playersNeedingNametagCooldownUpdate == null) {
                    item.lmEntity.playersNeedingNametagCooldownUpdate = mutableSetOf()
                }

                item.lmEntity.playersNeedingNametagCooldownUpdate!!.add(
                    main.nametagTimerChecker.entityTargetMap[item.lmEntity.livingEntity]!!
                )
            }
        }

        if (!item.lmEntity.isPopulated) {
            return
        }

        if (main.helperSettings.getBoolean(main.settingsCfg, "use-customname-for-mob-nametags")) {
            updateNametagCustomName(item.lmEntity, item.nametag!!.nametagNonNull)
            return
        }

        if (main.helperSettings.getBoolean(
                main.settingsCfg,
                "assert-entity-validity-with-nametag-packets"
            ) && !item.lmEntity.livingEntity
                .isValid
        ) {
            return
        }

        updateNametag(item.lmEntity, item.nametag!!, item.players!!)
    }

    private fun updateNametag(
        lmEntity: LivingEntityWrapper,
        nametag: NametagResult,
        players: MutableList<Player>
    ) {
        val loopCount = if (lmEntity.playersNeedingNametagCooldownUpdate == null) 1 else 2

        for (i in 0 until loopCount) {
            // will loop again to update with nametag cooldown for only the specified players

            val nametagVisibilityEnum = LevelledMobs.instance.rulesManager.getRuleCreatureNametagVisbility(
                    lmEntity
                )
            val doAlwaysVisible = i == 1 || !nametag.isNullOrEmpty && lmEntity.livingEntity.isCustomNameVisible ||
                    nametagVisibilityEnum.contains(NametagVisibilityEnum.ALWAYS_ON)

            if (i == 0) {
                // these players are not getting always on nametags unless always on has been configured for the mob
                for (player in players) {
                    if (lmEntity.playersNeedingNametagCooldownUpdate != null
                        && lmEntity.playersNeedingNametagCooldownUpdate!!.contains(player)
                    ) {
                        continue
                    }

                    nametagSender!!.sendNametag(
                        lmEntity.livingEntity, nametag, player,
                        doAlwaysVisible
                    )
                }
            } else {
                // these players are getting always on nametags
                for (player in lmEntity.playersNeedingNametagCooldownUpdate!!) {
                    nametagSender!!.sendNametag(lmEntity.livingEntity, nametag, player, true)
                }
            }

            if (hasLibsDisguisesInstalled && LibsDisguisesUtils.isMobUsingLibsDisguises(lmEntity)) {
                val useNametag = if (nametag.nametag != null) colorizeAll(nametag.nametag) else null
                LibsDisguisesUtils.updateLibsDisguiseNametag(lmEntity, useNametag)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun updateNametagCustomName(
        lmEntity: LivingEntityWrapper,
        nametag: String
    ) {
        synchronized(lmEntity.livingEntity.persistentDataContainer) {
            if (lmEntity.pdc
                    .has(NamespacedKeys.hasCustomNameTag, PersistentDataType.INTEGER)
            ) {
                return
            }
        }

        val hadCustomName = lmEntity.livingEntity.customName != null

        lmEntity.livingEntity.customName = nametag
        lmEntity.livingEntity.isCustomNameVisible = true

        val isTamable = (lmEntity.livingEntity is Tameable)

        if (!hadCustomName && !isTamable && !lmEntity.typeName.equals("Axolotl", ignoreCase = true)) {
            lmEntity.livingEntity.removeWhenFarAway = true
        }
    }
}