@file:Suppress("DEPRECATION")

package io.github.arcaneplugins.levelledmobs.util

import java.util.logging.Logger
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import org.bukkit.Bukkit
import org.bukkit.ChatColor

/**
 * Logging utility class.
 *
 * @author lokka30, stumper66
 * @since 1.0.3
 */
@SuppressWarnings("deprecation")
class MicroLogger(
    private val prefix: String
) {
    private val serverIsSpigot: Boolean = LevelledMobs.instance.ver.isRunningSpigot
    private val logger: Logger = Bukkit.getLogger()

    /**
     * @param message message to send with INFO log-level
     * @author lokka30, stumper66
     * @since unknown
     */
    fun info(message: String) {
        if (serverIsSpigot) Bukkit.getServer().consoleSender.sendMessage(MessageUtils.colorizeAll(prefix + message))
        else logger.info(MessageUtils.colorizeAll(prefix + message))
    }

    /**
     * @param message message to send with WARNING log-level
     * @author lokka30, stumper66
     * @since unknown
     */
    fun warning(message: String) {
        if (serverIsSpigot) Bukkit.getServer().consoleSender.sendMessage(MessageUtils.colorizeAll(ChatColor.YELLOW.toString() + "[WARN] " + ChatColor.RESET + prefix + message))
        else logger.warning(MessageUtils.colorizeAll(prefix + message))
    }

    /**
     * @param message message to send with ERROR log-level
     * @author lokka30, stumper66
     * @since unknown
     */
    fun error(message: String) {
        if (serverIsSpigot) Bukkit.getServer().consoleSender.sendMessage(MessageUtils.colorizeAll(ChatColor.RED.toString() + "[ERROR] " + ChatColor.RESET + prefix + message))
        else logger.severe(MessageUtils.colorizeAll(prefix + message))
    }
}