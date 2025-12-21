package io.github.arcaneplugins.levelledmobs.wrappers

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.scheduler.BukkitTask

/**
 * Holds info on a scheduled task that was executed
 *
 * @see SchedulerWrapper
 * @author stumper66
 * @since 3.11.0
 */
class SchedulerResult {
    var foliaTask: ScheduledTask? = null
        private set
    var regularTask: BukkitTask? = null
        private set
    var isRunningFolia: Boolean = false
        private set

    constructor(foliaTask: ScheduledTask?){
        this.foliaTask = foliaTask
        this.isRunningFolia = true
    }

    constructor(regularTask: BukkitTask?){
        this.regularTask = regularTask
    }

    fun cancelTask() {
        if (isRunningFolia)
            if (foliaTask != null) foliaTask!!.cancel()
        else
            if (regularTask != null) regularTask!!.cancel()
    }

    fun isCancelled(): Boolean {
        return if (isRunningFolia) {
            if (foliaTask != null) foliaTask!!.isCancelled
            else false
        } else {
            if (regularTask != null) regularTask!!.isCancelled
            else false
        }
    }
}