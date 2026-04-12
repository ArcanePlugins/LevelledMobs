package io.github.arcaneplugins.levelledmobs.util

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import org.bukkit.Bukkit

/**
 * Writes messages to the console
 *
 * @author lokka30, stumper66
 * @since 4.0
 */
object Log {
    private const val PREFIX = "&b[LevelledMobs]&7 "
    // use this function for testing messages so you will remember to remove them later
    @Deprecated("Remove before releasing", ReplaceWith("inf(msg)", "io.github.arcaneplugins.levelledmobs.util.Log.inf"))
    fun infTemp(msg: String?) {
        inf(msg)
    }

    fun inf(msg: String?) {
        if (LevelledMobs.instance.ver.isRunningPaper) {
            LevelledMobs.instance.logger.info(
                MessageUtils.colorizeAll(msg)
            )
        }
        else
            Bukkit.getServer().consoleSender.sendMessage(MessageUtils.colorizeAll(PREFIX + msg))
    }

    fun war(msg: String) {
        LevelledMobs.instance.logger.warning(
            MessageUtils.removeColorCodes(msg)
        )
    }

    fun sev(msg: String) {
        LevelledMobs.instance.logger.severe(
            MessageUtils.removeColorCodes(msg)
        )
    }
}