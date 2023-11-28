package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.LmFunction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import org.spongepowered.configurate.CommentedConfigurationNode
import java.util.*

class RunFunctionAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    val otherFuncId: String? = actionNode.node("id").string
    var sentError = false

    init {
        require(
            otherFuncId != null
        ) { "RunFunctionAction did not specify valid ID of function to run" };
    }

    override fun run(context: Context) {

        val potentiallyCircularFunction = context.linkedFunctions.stream()
            .anyMatch { otherFunction: LmFunction -> otherFunction.identifier == otherFuncId }

        val useCircularFunctionDependencyDetection: Boolean = LevelledMobs.lmInstance
            .configHandler.settingsCfg
            .root!!.node("advanced", "circular-function-dependency-detection")
            .getBoolean(false)

        if (useCircularFunctionDependencyDetection && potentiallyCircularFunction) {
            sev(
                "Blocked potentially recursive call to run function '$otherFuncId' in process '${parentProcess.identifier}' (parent " +
                        "function '${parentProcess.parentFunction.identifier}'). This protection can be disabled - be advised that recursive " +
                        "calls can result in memory leaks. LM will call 'exit-all' on the cause. " +
                        "This message will only appear once.",
    true
            )
            parentProcess.parentFunction.exitAll(context)
            return
        }

        val functionToRunOpt: Optional<LmFunction> = LogicHandler.lmFunctions.stream()
            .filter { otherFunction -> otherFunction.identifier == otherFuncId }
            .findFirst()

        if (functionToRunOpt.isEmpty) {
            if (sentError) return
            sev(
                "Unable to run function '$otherFuncId' from process '${parentProcess.identifier}' in function '${parentProcess.parentFunction.identifier}' as function '$otherFuncId' " +
                        "does not exist.",
                true
            )
            sentError = true
        } else {
            if (!potentiallyCircularFunction) context.withLinkedFunction(parentProcess.parentFunction)
            functionToRunOpt.get().run(context, false)
        }
    }
}