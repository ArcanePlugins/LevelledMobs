package io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.scheduler.BukkitTask

class SchedulerResult {
    var foliaTask: ScheduledTask? = null
        private set
    var regularTask: BukkitTask? = null
        private set

    constructor(foliaTask: ScheduledTask?){
        this.foliaTask = foliaTask
    }

    constructor(regularTask: BukkitTask?){
        this.regularTask = regularTask
    }

    val isRunningFolia: Boolean
        get() = this.foliaTask != null

    fun cancelTask(){
        if (isRunningFolia) {
            if (foliaTask != null) foliaTask!!.cancel()
        } else {
            if (regularTask != null) regularTask!!.cancel()
        }
    }

    fun isCancelled() : Boolean{
        return if (isRunningFolia) {
            if (foliaTask != null) foliaTask!!.isCancelled else false
        } else {
            if (regularTask != null) regularTask!!.isCancelled else false
        }
    }
}