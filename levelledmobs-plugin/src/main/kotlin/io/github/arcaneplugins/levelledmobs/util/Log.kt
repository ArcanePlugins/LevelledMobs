package io.github.arcaneplugins.levelledmobs.util

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
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
        if (LevelledMobs.instance.ver.isRunningPaper)
            sendMessagePaper(msg)
        else
            Bukkit.getServer().consoleSender.sendMessage(MessageUtils.colorizeAll(PREFIX + msg))
    }

    fun war(msg: String) {
        if (LevelledMobs.instance.ver.isRunningPaper)
            sendMessagePaper("&e[WARN] $msg")
        else
            Bukkit.getServer().consoleSender.sendMessage(MessageUtils.colorizeAll(PREFIX + msg))
    }

    fun sev(msg: String) {
        if (LevelledMobs.instance.ver.isRunningPaper)
            sendMessagePaper("&e[SEVERE] $msg")
        else
            Bukkit.getServer().consoleSender.sendMessage(MessageUtils.colorizeAll(PREFIX + msg))
    }

    private fun sendMessagePaper(msg: String?){
        if (msg == null) return

        val serializer = LegacyComponentSerializer.legacyAmpersand()
        val msgComp = serializer.deserialize("$PREFIX$msg")
        Bukkit.getServer().consoleSender.sendMessage { msgComp }
    }
}