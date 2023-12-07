package io.github.arcaneplugins.levelledmobs.bukkit.logic

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import io.github.arcaneplugins.levelledmobs.bukkit.util.ServerInfoInfo as ver

class SchedulerWrapper(
    var entity: Entity? = null,
    var runnable: Runnable? = null
) {

    // on folia servers if you want to use the regional scheduler
    // then provide a location before executing run()
    var locationForRegionScheduler: Location? = null
    var runDirectlyInFolia = false
    var runDirectlyInBukkit = false

    fun run(){
        if (ver.isRunningFolia){
            if (runDirectlyInFolia) {
                runnable!!.run()
                return
            }

            val task = Consumer { _: ScheduledTask -> runnable!!.run() }

            if (entity != null) {
                entity!!.scheduler.run(LevelledMobs.lmInstance, task, null)
            } else {
                if (locationForRegionScheduler != null) {
                    Bukkit.getRegionScheduler().run(LevelledMobs.lmInstance, locationForRegionScheduler!!, task)
                } else {
                    Bukkit.getAsyncScheduler().runNow(LevelledMobs.lmInstance, task)
                }
            }
        }
        else{
            if (runDirectlyInBukkit) {
                runnable!!.run()
                return
            }

            // if you provided an entity in the constructor, it is assumed the main thread needs to be used
            // since accessing entities asynchronously will usually result in an error
            if (entity != null) {
                Bukkit.getScheduler().runTask(LevelledMobs.lmInstance, runnable!!)
            } else {
                Bukkit.getScheduler().runTaskAsynchronously(LevelledMobs.lmInstance, runnable!!)
            }
        }
    }

    fun runTaskTimerAsynchronously(
        delayMS: Long,
        periodMS: Long
    ): SchedulerResult{
        return if (ver.isRunningFolia) {
            val task =
                Consumer { _: ScheduledTask? -> runnable!!.run() }
            val scheduledTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                LevelledMobs.lmInstance, task, delayMS, periodMS, TimeUnit.MILLISECONDS
            )
            SchedulerResult(foliaTask = scheduledTask)
        } else {
            // convert milliseconds into approximent ticks
            // 1 tick = ~ 50ms
            val convertedDelay = delayMS / 50L
            val convertedPeriod = periodMS / 50L
            val bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                LevelledMobs.lmInstance, runnable!!, convertedDelay, convertedPeriod
            )
            SchedulerResult(regularTask = bukkitTask)
        }
    }

    fun runDelayed(
        delayInTicks: Long
    ): SchedulerResult{
        return if (ver.isRunningFolia) {
            val task =
                Consumer { _: ScheduledTask? -> runnable!!.run() }
            val scheduledTask: ScheduledTask? = if (entity != null) {
                entity!!.scheduler.runDelayed(LevelledMobs.lmInstance, task, null, delayInTicks)
            } else {
                val milliseconds = delayInTicks * 50L
                Bukkit.getAsyncScheduler().runDelayed(
                    LevelledMobs.lmInstance, task, milliseconds, TimeUnit.MILLISECONDS
                )
            }
            SchedulerResult(foliaTask = scheduledTask)
        } else {
            val bukkitTask = Bukkit.getScheduler().runTaskLater(
                LevelledMobs.lmInstance,
                runnable!!, delayInTicks
            )
            SchedulerResult(regularTask = bukkitTask)
        }
    }

    val getWillRunDirectly: Boolean
        get() {
            return if (ver.isRunningFolia) {
                runDirectlyInFolia
            } else {
                runDirectlyInBukkit
            }
        }
}