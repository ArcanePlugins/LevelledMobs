package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl

import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.replacePapiAndContextPlaceholders
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import org.bukkit.Bukkit
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException

class BroadcastMessageToServerAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    private val requiredPermission: String = node.node("required-permission").getString("")
    val message: MutableList<String>

    init {
        try {
            this.message = node.node("message").getList(String::class.java)!!
        } catch (ex: ConfigurateException) {
            throw RuntimeException(ex)
        } catch (ex: NullPointerException) {
            throw RuntimeException(ex)
        }
    }

    override fun run(context: Context) {
        debug(DebugCategory.FUNCTIONS_GENERIC) {
            "BroadcastMessageToServerAction is running with message='$message'," +
                    "requiredPermission='$requiredPermission'."
        }

        val lines = mutableListOf<String>()
        for (line in message) {
            debug(DebugCategory.FUNCTIONS_GENERIC) {
                "BroadcastMessageToServerAction BEFORE replacePapiAndContextPlaceholders LINE: $line"
            }
            lines.add(replacePapiAndContextPlaceholders(line, context))
            debug(DebugCategory.FUNCTIONS_GENERIC) {
                "BroadcastMessageToServerAction line added to lines, new lines size: ${lines.size}"
            }
        }
        debug(DebugCategory.FUNCTIONS_GENERIC) {
            "BroadcastMessageToServerAction replacePapiAndContextPlaceholders lines: $lines"
        }

        val msg = Message.formatMd(lines)

        for (player in Bukkit.getOnlinePlayers()) {
            debug(DebugCategory.FUNCTIONS_GENERIC) {"BroadcastMessageToServerAction is checking perms of ${player.name}"}
            if (requiresPermission() && !player.hasPermission(requiredPermission)) continue
            debug(DebugCategory.FUNCTIONS_GENERIC) {"BroadcastMessageToServerAction is sending msg to ${player.name}"}
            player.sendMessage(msg)
        }
    }

    private fun requiresPermission(): Boolean {
        return requiredPermission.isNotEmpty()
    }
}