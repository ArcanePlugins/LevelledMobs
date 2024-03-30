package io.github.arcaneplugins.levelledmobs.managers

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.misc.EvaluationException
import io.github.arcaneplugins.levelledmobs.misc.QueueItem
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.wrappers.SchedulerWrapper
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
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
    private val processingList = mutableListOf<UUID>()
    private val maxThreads = 3
    private val threadsCount = AtomicInteger()
    private val queueLock = Any()

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
                doneWithThread()
            }

            for (i in 0..<maxThreads){
                threadsCount.getAndIncrement()
                Bukkit.getScheduler().runTaskAsynchronously(LevelledMobs.instance, bgThread)
            }
        }
    }

    fun clearQueue(){
        synchronized(queueLock){
            queue.clear()
        }
    }

    private fun doneWithThread(){
        threadsCount.getAndDecrement()
        if (threadsCount.get() == 0) {
            isRunning = false
            Log.inf("Mob processing queue Manager has exited")
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
            var offeredItem = false
            synchronized(queueLock){
                if (!processingList.contains(item.entityId)) {
                    queue.offer(item)
                    offeredItem = true
                }
            }

            if (!offeredItem)
                item.lmEntity.free()
        }
    }

    private fun mainThread() {
        while (doThread) {
            val item: QueueItem?
            synchronized(queueLock){
                item = queue.poll()
                if (item != null)
                    processingList.add(item.entityId)
            }
            if (item == null) {
                Thread.sleep(2L)
                continue
            }

            try {
                processItem(item)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                synchronized(queueLock){
                    processingList.remove(item.entityId)
                }
                item.lmEntity.free()
            }
        }

        doneWithThread()
    }

    private fun processItem(item: QueueItem) {
        if (!item.lmEntity.isPopulated) return

        if (!item.lmEntity.shouldShowLMNametag) {
            return
        }
        try{
            LevelledMobs.instance.levelManager.entitySpawnListener.preprocessMob(item.lmEntity, item.event)
        }

        catch (ignored: EvaluationException){
            // this exception is manually thrown after logging and notifying op users
        }
        catch (e: java.util.concurrent.TimeoutException){
            DebugManager.log(DebugType.APPLY_LEVEL_RESULT, item.lmEntity, false){
                "Timed out applying level to mob"
            }
        }
    }
}