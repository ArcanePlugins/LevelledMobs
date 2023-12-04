package io.github.arcaneplugins.levelledmobs.bukkit.logic.function

import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import org.spongepowered.configurate.CommentedConfigurationNode

class LmFunction(
    val identifier: String,
    val description: String,
    val node: CommentedConfigurationNode
) {
    //val triggers: Set<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
    val triggers = mutableSetOf<String>()
    val processes: MutableSet<Process> = mutableSetOf()
    var exiting = false
        set(value) {
            field = value
            processes.forEach { process -> process.exit = exiting }
        }

    fun run(context: Context, overrideConditions: Boolean){
        debug(DebugCategory.FUNCTIONS_GENERIC) {"Running function ${identifier}."}

        for (process in processes){
            if (exiting){
                this.exiting = false
                return
            }

            debug(DebugCategory.FUNCTIONS_GENERIC) {"Calling process ${process.identifier} in function ${identifier}."}
            process.call(context, overrideConditions)
        }
    }

    fun hasAnyTriggers(
        triggersToCheck: MutableList<String>
    ): Boolean{
        for (triggerToCheck in triggersToCheck) {
            if (triggers.stream().anyMatch { trig: String ->
                trig.equals(triggerToCheck, ignoreCase = true)
            }) {
                return true
            }
        }

        return false
    }

    fun exitAll(context: Context){
        exiting = true
        context.linkedFunctions.forEach { lmFunction: LmFunction ->  lmFunction.exiting = true }
    }
}