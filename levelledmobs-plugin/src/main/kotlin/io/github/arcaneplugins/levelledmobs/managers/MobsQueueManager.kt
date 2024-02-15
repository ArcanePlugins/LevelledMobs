package io.github.arcaneplugins.levelledmobs.managers

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.misc.QueueItem
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import org.bukkit.Bukkit

/**
 * Queues up mob info so they can be processed in a background thread
 *
 * @author stumper66
 * @since 3.0.0
 */
class MobsQueueManager {
    private var isRunning = false
    private var doThread = false
    private val queue = LinkedBlockingQueue<QueueItem>()

    fun start() {
        if (isRunning) {
            return
        }
        doThread = true
        isRunning = true

        if (!LevelledMobs.instance.ver.isRunningFolia) {
            val bgThread = Runnable {
                try {
                    mainThread()
                } catch (ignored: InterruptedException) {
                    isRunning = false
                }
                Log.inf("Mob processing queue Manager has exited")
            }

            Bukkit.getScheduler().runTaskAsynchronously(LevelledMobs.instance, bgThread)
        }
    }

    fun stop() {
        doThread = false
    }

    fun addToQueue(item: QueueItem) {
        item.lmEntity.inUseCount.getAndIncrement()

        if (LevelledMobs.instance.ver.isRunningFolia) {
            val wrapper = SchedulerWrapper(
                item.lmEntity.livingEntity
            ) {
                processItem(item)
                item.lmEntity.free()
            }

            wrapper.run()
        } else {
            queue.offer(item)
        }
    }

    private fun mainThread() {
        while (doThread) {
            val item = queue.poll(200, TimeUnit.MILLISECONDS) ?: continue

            try {
                processItem(item)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                item.lmEntity.free()
            }
        }

        isRunning = false
    }

    private fun processItem(item: QueueItem) {
        if (!item.lmEntity.isPopulated) return

        if (!item.lmEntity.shouldShowLMNametag) {
            return
        }
        LevelledMobs.instance.levelManager.entitySpawnListener.preprocessMob(item.lmEntity, item.event)
    }
}