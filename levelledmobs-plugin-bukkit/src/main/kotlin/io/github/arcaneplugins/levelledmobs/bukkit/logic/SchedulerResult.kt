package io.github.arcaneplugins.levelledmobs.bukkit.logic

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.scheduler.BukkitTask

class SchedulerResult(
    val foliaTask: ScheduledTask? = null,
    val regularTask: BukkitTask? = null
) {
    val isRunningFolia = foliaTask != null

    fun cancelTask(){
        if (isRunningFolia) {
            foliaTask?.cancel()
        } else {
            regularTask?.cancel()
        }
    }

    val isCancelled: Boolean
        get() {
            return if (isRunningFolia) {
                foliaTask?.isCancelled ?: false
            } else {
                regularTask?.isCancelled ?: false
            }
        }
}