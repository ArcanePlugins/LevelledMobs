package io.github.arcaneplugins.levelledmobs.wrappers

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity

/**
 * This class is used for when code needs to be executed in a specific thread context.
 * It allows for Folia servers to have code executed in the correct scheduler while
 * providing compatibility for Paper / Spigot servers without needing to use
 * different methods per server type
 *
 * @author stumper66
 * @since 3.11.0
 */
class SchedulerWrapper {
    var runnable: Runnable? = null
    var entity: Entity? = null

    constructor(runnable: Runnable){
        this.runnable = runnable
    }

    constructor(entity: Entity?){
        this.entity = entity
    }

    constructor(entity: Entity?, runnable: Runnable){
        this.entity = entity
        this.runnable = runnable
    }

    var locationForRegionScheduler: Location? = null
    var runDirectlyInFolia: Boolean = false
    var runDirectlyInBukkit: Boolean = false
    val main = LevelledMobs.instance

    fun run() {
        if (main.ver.isRunningFolia) {
            if (runDirectlyInFolia) {
                runnable!!.run()
                return
            }

            val task = Consumer { _: ScheduledTask -> runnable!!.run() }

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

    fun runTaskTimerAsynchronously(
        initialDelayMS: Long,
        repeatPeriodMS: Long
    ): SchedulerResult {
        if (main.ver.isRunningFolia) {
            val task =
                Consumer { _: ScheduledTask -> runnable!!.run() }
            val scheduledTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                main, task, initialDelayMS, repeatPeriodMS, TimeUnit.MILLISECONDS
            )

            return SchedulerResult(scheduledTask)
        } else {
            // convert milliseconds into approximent ticks
            // 1 tick = ~ 50ms
            val convertedDelay = initialDelayMS / 50L
            val convertedPeriod = repeatPeriodMS / 50L

            val bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                main, runnable!!, convertedDelay, convertedPeriod
            )

            return SchedulerResult(bukkitTask)
        }
    }

    fun runDelayed(
        delayInTicks: Long
    ): SchedulerResult {
        if (main.ver.isRunningFolia) {
            val task =
                Consumer { _: ScheduledTask? -> runnable!!.run() }

            val scheduledTask: ScheduledTask?
            if (this.entity != null) {
                scheduledTask = entity!!.scheduler.runDelayed(main, task, null, delayInTicks)
            } else {
                val milliseconds = delayInTicks * 50L
                scheduledTask = Bukkit.getAsyncScheduler().runDelayed(
                    main, task, milliseconds, TimeUnit.MILLISECONDS
                )
            }

            return SchedulerResult(scheduledTask)
        } else {
            val bukkitTask = Bukkit.getScheduler().runTaskLater(
                main,
                runnable!!, delayInTicks
            )

            return SchedulerResult(bukkitTask)
        }
    }

    val willRunDirectly: Boolean
        get() {
            return if (main.ver.isRunningFolia) {
                runDirectlyInFolia
            } else {
                runDirectlyInBukkit
            }
        }
}