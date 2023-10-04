package io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity

class SchedulerWrapper {
    val main = LevelledMobs.lmInstance

    var runnable: Runnable? = null
    var entity: Entity? = null

    constructor(runnable: Runnable){
        this.runnable = runnable
        this.entity = null
    }

    constructor(entity: Entity?){
        // if an entity is providing and the server is folia, then the entity scheduler will be used
        this.entity = entity
    }

    constructor(entity: Entity?, runnable: Runnable){
        // if an entity is providing and the server is folia, then the entity scheduler will be used
        this.entity = entity
        this.runnable = runnable
    }

    // on folia servers if you want to use the regional scheduler
    // then provide a location before executing run()
    var locationForRegionScheduler: Location? = null
    var runDirectlyInFolia: Boolean = false
    var runDirectlyInBukkit: Boolean = false

    fun run(){
        if (main.verInfo.isRunningFolia) {
            if (runDirectlyInFolia) {
                runnable!!.run()
                return
            }
            val task =
                Consumer { scheduledTask: ScheduledTask? -> runnable!!.run() }
            if (entity != null) {
                entity!!.scheduler.run(main, task, null)
            } else {
                if (locationForRegionScheduler != null) {
                    Bukkit.getRegionScheduler().run(main, locationForRegionScheduler!!, task)
                } else {
                    Bukkit.getAsyncScheduler().runNow(main, task)
                }
            }
        } else {
            if (runDirectlyInBukkit) {
                runnable!!.run()
                return
            }

            // if you provided an entity in the constructor, it is assumed the main thread needs to be used
            // since accessing entities asynchronously will usually result in an error
            if (entity != null) {
                Bukkit.getScheduler().runTask(main, runnable!!)
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(main, runnable!!)
            }
        }
    }

    fun runTaskTimerAsynchronously(delayMS: Long, periodMS: Long) : SchedulerResult{
        if (main.verInfo.isRunningFolia){
            val task =
                Consumer { _: ScheduledTask? -> runnable!!.run() }
            val scheduledTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                main, task, delayMS, periodMS, TimeUnit.MILLISECONDS
            )

            return SchedulerResult(scheduledTask)
        }
        else{
            // convert milliseconds into approximent ticks
            // 1 tick = ~ 50ms
            val convertedDelay = delayMS / 50L
            val convertedPeriod = periodMS / 50L

            val bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                main, runnable!!, convertedDelay, convertedPeriod
            )

            return SchedulerResult(bukkitTask)
        }
    }

    fun runDelayed(delayInTicks: Long) : SchedulerResult{
        if (main.verInfo.isRunningFolia){
            val task =
                Consumer { _: ScheduledTask? -> runnable!!.run() }

            val scheduledTask: ScheduledTask? = if (entity != null) {
                entity!!.scheduler.runDelayed(main, task, null, delayInTicks)
            } else {
                val milliseconds = delayInTicks * 50L
                Bukkit.getAsyncScheduler().runDelayed(
                    main, task, milliseconds, TimeUnit.MILLISECONDS
                )
            }

            return SchedulerResult(scheduledTask)
        }
        else{
            val bukkitTask = Bukkit.getScheduler().runTaskLater(
                main,
                runnable!!, delayInTicks
            )

            return SchedulerResult(bukkitTask)
        }
    }

    fun willRunDirectly() : Boolean{
        return if (main.verInfo.isRunningFolia) {
            runDirectlyInFolia
        } else {
            runDirectlyInBukkit
        }
    }
}