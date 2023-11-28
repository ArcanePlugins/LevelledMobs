package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl

import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.replacePapiAndContextPlaceholders
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log
import org.bukkit.World
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException

class BroadcastMessageToWorldAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    val requiredPermission: String = node.node("required-permission").getString("")
    val message: MutableList<String>

    init {
        try {
            this.message = node.node("message").getList(String::class.java)!!
        } catch (ex: ConfigurateException) {
            logError()
            throw ex
        } catch (ex: NullPointerException) {
            logError()
            throw ex
        }
    }

    private fun logError() {
        Log.sev(
            "Unable to parse action '${this.javaClass.getSimpleName()}' in " +
                    "process '${process.identifier}': invalid message value. This is " +
                    "usually the result of a user-caused syntax error in settings.yml. A stack trace " +
                    "will be printed below for debugging purposes.",
            true
        );
    }

    override fun run(context: Context) {

        val world: World = if (context.location != null) {
            context.location!!.world
        } else if (context.entity != null) {
            context.entity!!.world
        } else if (context.player != null) {
            context.player!!.world
        } else {
            throw RuntimeException(
                "A 'broadcast-message-to-world' action has encountered an issue in process '${parentProcess.identifier}' " +
                "(in function '${parentProcess.parentFunction.identifier}'), where a context is missing an entity or player."
            )
        }

        val lines = mutableListOf<String>()
        for (line in message) {
            lines.add(replacePapiAndContextPlaceholders(line, context))
        }

        val msg = Message.formatMd(lines)

        for (player in world.players) {
            if (hasRequiredPermission() && !player.hasPermission(requiredPermission)) continue
            player.sendMessage(msg)
        }
    }

    fun hasRequiredPermission(): Boolean {
        return requiredPermission.isNotEmpty()
    }
}