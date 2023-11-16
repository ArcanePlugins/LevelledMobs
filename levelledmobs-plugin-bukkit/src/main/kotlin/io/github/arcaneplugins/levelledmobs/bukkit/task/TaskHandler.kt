package io.github.arcaneplugins.levelledmobs.bukkit.task

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.runFunctionsWithTriggers
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.inf
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.TimeUtils.parseTimeToTicks
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import java.util.*

object TaskHandler {
    private var mobScanTask: BukkitTask? = null

    fun startTasks() {
        inf("Starting tasks")
        startMobScanTask()
    }

    fun stopTasks() {
        inf("Stopping tasks")
        mobScanTask?.cancel()
    }

    private fun startMobScanTask(){
        mobScanTask?.cancel()

        val scanTaskNode: CommentedConfigurationNode = LevelledMobs.lmInstance.configHandler.settingsCfg
            .root!!.node("advanced", "scan-task")

        if (!scanTaskNode.node("enabled").getBoolean(false)) return

        val runnable = Runnable {
            for (world in Bukkit.getWorlds()) {
                for (entity in world.entities) {
                    runFunctionsWithTriggers(
                        Context().withEntity(entity),
                        mutableListOf("on-entity-scan")
                    )
                }
            }
        }

        val periodNode = scanTaskNode.node("period")
        var period = (20 * 7).toLong() // 7 seconds

        if (!periodNode.virtual()) {
            period = try {
                parseTimeToTicks(periodNode.get(Any::class.java)!!)
            } catch (ex: SerializationException) {
                throw RuntimeException(ex)
            }
        }

        mobScanTask = Bukkit.getScheduler().runTaskTimer(
            LevelledMobs.lmInstance,
            runnable,
            0L,  // Delay
            period
        )
    }
}