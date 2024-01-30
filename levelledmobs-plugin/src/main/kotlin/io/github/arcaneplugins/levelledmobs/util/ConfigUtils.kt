package io.github.arcaneplugins.levelledmobs.util

import java.util.function.Consumer
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.util.Utils.colorizeAllInList
import io.github.arcaneplugins.levelledmobs.util.Utils.replaceAllInList
import org.bukkit.command.CommandSender

/**
 * Used for managing configuration data
 *
 * @author lokka30, stumper66
 * @since 2.5.0
 */
class ConfigUtils{
    companion object{
        private var SETTINGS_CREEPER_MAX_RADIUS = 0
        private var SETTINGS_SPAWN_DISTANCE_FROM_PLAYER = 0
    }

    var debugEntityDamageWasEnabled: Boolean = false
    var chunkLoadListenerWasEnabled: Boolean = false
    var playerLevellingEnabled: Boolean = false

    fun load() {
        // anything less than 3 breaks the formula
        if (SETTINGS_CREEPER_MAX_RADIUS < 3) {
            SETTINGS_CREEPER_MAX_RADIUS = 3
        }
        if (SETTINGS_SPAWN_DISTANCE_FROM_PLAYER < 1) {
            SETTINGS_SPAWN_DISTANCE_FROM_PLAYER = 1
        }
    }

    fun getPrefix(): String {
        return MessageUtils.colorizeAll(
            LevelledMobs.instance.messagesCfg.getString("common.prefix")
        )
    }

    fun sendNoPermissionMsg(sender: CommandSender) {
        var noPermissionMsg = LevelledMobs.instance.messagesCfg.getStringList("common.no-permission")

        noPermissionMsg = replaceAllInList(noPermissionMsg, "%prefix%", getPrefix())
        noPermissionMsg = colorizeAllInList(noPermissionMsg)

        noPermissionMsg.forEach(Consumer { s: String -> sender.sendMessage(s) })
    }
}