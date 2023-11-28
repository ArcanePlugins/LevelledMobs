package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl

import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.replacePapiAndContextPlaceholders
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import org.bukkit.Bukkit
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException

class BroadcastMessageToServerAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    val requiredPermission: String = node.node("required-permission").getString("")
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
        val lines = mutableListOf<String>()
        for (line in message) {
            lines.add(replacePapiAndContextPlaceholders(line, context))
        }
        val msg = Message.formatMd(lines)

        for (player in Bukkit.getOnlinePlayers()) {
            if (hasRequiredPermission() && !player.hasPermission(requiredPermission)) continue
            player.sendMessage(msg)
        }
    }

    fun hasRequiredPermission(): Boolean {
        return requiredPermission.isNotEmpty()
    }
}