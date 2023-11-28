package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl

import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.replacePapiAndContextPlaceholders
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import org.bukkit.Location
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException


class BroadcastMessageToNearbyPlayersAction(
    process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    val requiredPermission: String = node
        .node("required-permission")
        .getString("")
    val message: MutableList<String>
    val range = node.node("range").getDouble(32.0)

    init {
        try {
            message =  node.node("message").getList(String::class.java)!!
        } catch (ex: ConfigurateException) {
            throw RuntimeException(ex)
        } catch (ex: NullPointerException) {
            throw RuntimeException(ex)
        }
    }

    override fun run(context: Context) {
        val location: Location?

        location = if (context.location != null) {
            context.location
        } else if (context.entity != null) {
            context.entity!!.location
        } else if (context.player != null) {
            context.player!!.location
        } else {
            throw RuntimeException("Action requires a location context")
        }

        val lines = mutableListOf<String>()
        for (line in message) {
            lines.add(replacePapiAndContextPlaceholders(line, context))
        }
        val msg = Message.formatMd(lines)

        for (player in location!!.getNearbyPlayers(range, range, range)) {
            if (hasRequiredPermission() && !player.hasPermission(requiredPermission)) continue
            player.sendMessage(msg)
        }
    }

    fun hasRequiredPermission(): Boolean{
        return requiredPermission.isNotEmpty()
    }
}