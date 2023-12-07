package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.LmFunction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition
import io.github.arcaneplugins.levelledmobs.bukkit.logic.preset.Preset
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import org.bukkit.scheduler.BukkitRunnable
import org.spongepowered.configurate.CommentedConfigurationNode
import java.util.function.Supplier

class Process(
    val identifier: String,
    val description: String,
    val node: CommentedConfigurationNode,
    val parentFunction: LmFunction,
    val delay: Long
) {
    val conditions: MutableList<Condition> = mutableListOf()
    val actions: MutableList<Action> = mutableListOf()
    val presets: MutableSet<Preset> = mutableSetOf()
    var exit = false

    fun call(
        context: Context,
        overrideConditions: Boolean
    ){
        debug(DebugCategory.FUNCTIONS_GENERIC) {"Calling process ${identifier}."}

        //val canRun:Supplier<Boolean> = { overrideConditions || conditionsApply(context) }
        val canRun = Supplier {
            overrideConditions || conditionsApply(context)
        }

        if (delay == 0L){
            if (!canRun.get()) {
                debug(DebugCategory.FUNCTIONS_GENERIC) {"Process $identifier did not meet canRun requirements."}
                return
            }
            runActions(context)
        }
        else{
            debug(DebugCategory.FUNCTIONS_GENERIC) {"Process $identifier has a delay of $delay ticks, running later."}
            object : BukkitRunnable() {
                override fun run() {
                    debug(DebugCategory.FUNCTIONS_GENERIC) {"Process $identifier is now running after its delay of $delay ticks."}
                    if (!canRun.get()) {
                        debug(DebugCategory.FUNCTIONS_GENERIC) {"Process $identifier did not meet canRun requirements."}
                        return
                    }
                    runActions(context)
                }
            }.runTaskLater(LevelledMobs.lmInstance, delay)
        }
    }

    private fun conditionsApply(context: Context): Boolean{
        val totalConditions: Int = conditions.size

        if (totalConditions == 0) return true

        val conditionsPercentageRequired = 1.0f //TODO configurable
        var conditionsMet = 0

        for (condition in conditions){
            val result = condition.applies(context)
            debug(DebugCategory.CONDITION){ "Condition: ${condition.name}, entity: ${context.entity}, result: $result" }
            if (result){
                conditionsMet++
            }
        }

        return (conditionsMet * 1.0f / totalConditions) >= conditionsPercentageRequired
    }

    private fun runActions(context: Context){
        debug(DebugCategory.FUNCTIONS_GENERIC) {"Process $identifier is running its actions."}
        exit = false
        for (action in actions){
            if (exit) {
                debug(DebugCategory.FUNCTIONS_GENERIC) {"Process $identifier is exiting early (exit=${exit})."}
                return
            }
            action.run(context)
        }
        exit = false
    }


}