package io.github.arcaneplugins.levelledmobs.misc

import java.time.Duration
import java.time.Instant
import java.util.LinkedList
import java.util.Queue
import java.util.WeakHashMap
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.rules.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
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
        val nametagTimer_Lock: Any = Any()
        val entityTarget_Lock: Any = Any()
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
                        nametagCooldownQueue[item.player] = WeakHashMap<LivingEntity, Instant>()
                    } else {
                        nametagCooldownQueue.remove(item.player)
                    }
                }
            }
            if (nametagCooldownQueue.isEmpty()) return
            for (coolDown in nametagCooldownQueue.entries) {
                val player = coolDown.key

                if (LevelledMobs.instance.ver.isRunningFolia) {
                    player.scheduler.run(
                        LevelledMobs.instance,
                        {
                            processCooldownQueue(
                                player,
                                coolDown
                            )
                        }, null
                    )
                } else {
                    processCooldownQueue(player, coolDown)
                }
            }
        }
    }

    private fun processCooldownQueue(
        player: Player,
        coolDown: Map.Entry<Player, WeakHashMap<LivingEntity, Instant>>
    ) {
        val entitiesToRemove: MutableList<LivingEntity> = LinkedList()

        for (livingEntity in coolDown.value.keys) {
            if (!livingEntity.isValid) {
                continue
            }

            val startInclusive = coolDown.value[livingEntity]
            if (startInclusive == null) {
                entitiesToRemove.add(livingEntity)
                continue
            }

            if (player.world.name != livingEntity.world.name) {
                entitiesToRemove.add(livingEntity)
                continue
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

                val scheduler = SchedulerWrapper(livingEntity) {
                    if (usesLoS && livingEntity.hasLineOfSight(player)) {
                        coolDown.value[livingEntity] = Instant.now()
                    } else {
                        entitiesToRemove.add(livingEntity)
                    }
                    lmEntity.free()
                }
                scheduler.runDirectlyInBukkit = true
                scheduler.run()
            }
        }

        for (livingEntity in entitiesToRemove) {
            coolDown.value.remove(livingEntity)

            val wrapper = SchedulerWrapper(
                livingEntity
            ) { updateNametag(livingEntity, player) }
            wrapper.runDirectlyInBukkit = true
            wrapper.run()
        }

        entitiesToRemove.clear()
    }

    private fun updateNametag(livingEntity: LivingEntity, player: Player) {
        val lmEntity = LivingEntityWrapper.getInstance(livingEntity)

        val nametag = lmEntity.main.levelManager.getNametag(lmEntity, isDeathNametag = false, preserveMobName = true)
        lmEntity.main.levelManager.updateNametag(lmEntity, nametag, mutableListOf(player))

        lmEntity.free()
    }
}