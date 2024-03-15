package io.github.arcaneplugins.levelledmobs.misc

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import java.time.Duration
import java.time.Instant
import java.util.LinkedList
import java.util.Queue
import java.util.WeakHashMap
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * Used to mobs have a nametag cooldown timer where the nametag stays always visible for a
 * configurable amount of time
 *
 * @author stumper66
 * @since 3.2.0
 */
class NametagTimerChecker {
    val nametagCooldownQueue = mutableMapOf<Player, WeakHashMap<LivingEntity, Instant>>()
    val cooldownTimes = WeakHashMap<LivingEntity, Long>()
    val entityTargetMap = WeakHashMap<LivingEntity, Player>()
    private val playersQueue: Queue<PlayerQueueItem> = LinkedList()
    companion object{
        val nametagTimer_Lock = Any()
        val entityTarget_Lock = Any()
        private val playerQueue_Lock = Any()
    }

    fun addPlayerToQueue(item: PlayerQueueItem) {
        synchronized(playerQueue_Lock) {
            playersQueue.offer(item)
        }
    }

    fun checkNametags() {
        // in folia this is using the bukkit async scheduler context
        synchronized(nametagTimer_Lock) {
            synchronized(playerQueue_Lock) {
                while (!playersQueue.isEmpty()) {
                    val item = playersQueue.poll() ?: continue

                    if (item.isPlayerJoin) {
                        nametagCooldownQueue[item.player] = WeakHashMap()
                    } else {
                        nametagCooldownQueue.remove(item.player)
                    }
                }
            }
            if (nametagCooldownQueue.isEmpty()) return
            for (coolDown in nametagCooldownQueue.entries) {
                val player = coolDown.key
                processCooldownQueue(player, coolDown)
            }
        }
    }

    private fun processCooldownQueue(
        player: Player,
        coolDown: Map.Entry<Player, WeakHashMap<LivingEntity, Instant>>
    ) {
        val entitiesToRemove = mutableListOf<LivingEntity>()
        val isRunningFolia = LevelledMobs.instance.ver.isRunningFolia

        for (livingEntity in coolDown.value.keys) {
            if (isRunningFolia) {
                livingEntity.scheduler.run(
                    LevelledMobs.instance, {
                        processCoolDownEntity(
                            livingEntity,
                            entitiesToRemove,
                            player,
                            coolDown
                        )},
                    null
                )
            }
            else
                processCoolDownEntity(livingEntity, entitiesToRemove, player, coolDown)
        }

        for (livingEntity in entitiesToRemove) {
            coolDown.value.remove(livingEntity)

            val wrapper = SchedulerWrapper(livingEntity) { updateNametag(livingEntity, player) }
            wrapper.runDirectlyInBukkit = true
            wrapper.run()
        }

        entitiesToRemove.clear()
    }

    private fun processCoolDownEntity(
        livingEntity: LivingEntity,
        entitiesToRemove: MutableList<LivingEntity>,
        player: Player,
        coolDown: Map.Entry<Player, WeakHashMap<LivingEntity, Instant>>
    ) {
        if (!livingEntity.isValid) {
            return
        }

        val startInclusive = coolDown.value[livingEntity]
        if (startInclusive == null) {
            entitiesToRemove.add(livingEntity)
            return
        }

        if (player.world.name != livingEntity.world.name) {
            entitiesToRemove.add(livingEntity)
            return
        }

        val timeDuration = Duration.between(
            startInclusive, Instant.now()
        )
        val cooldownTime = cooldownTimes[livingEntity]!!
        if (timeDuration.toMillis() >= cooldownTime) {
            // if using LoS targeting check if it's still within LoS and don't remove if so.
            val lmEntity = LivingEntityWrapper.getInstance(livingEntity)
            val usesLoS: Boolean = LevelledMobs.instance.rulesManager.getRuleCreatureNametagVisbility(
                lmEntity
            ).contains(NametagVisibilityEnum.TARGETED)

            if (usesLoS && livingEntity.hasLineOfSight(player)) {
                coolDown.value[livingEntity] = Instant.now()
            } else {
                entitiesToRemove.add(livingEntity)
            }

            lmEntity.free()
        }
    }

    private fun updateNametag(livingEntity: LivingEntity, player: Player) {
        val lmEntity = LivingEntityWrapper.getInstance(livingEntity)

        val nametag = lmEntity.main.levelManager.getNametag(lmEntity, isDeathNametag = false, preserveMobName = true)
        lmEntity.main.levelManager.updateNametag(lmEntity, nametag, mutableListOf(player))

        lmEntity.free()
    }
}