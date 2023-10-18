package io.github.arcaneplugins.levelledmobs.bukkit.util

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugHandler
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import java.util.function.Supplier

object Log {
    /*
    When testing, sometimes it is useful to include this suffix on particular debug logs to
    make it more clear to see amongst the rest of the mess.
     */
    val DEBUG_I_AM_BLIND_SUFFIX = " " + ChatColor.AQUA + "<------ notice me"

    fun inf(msg: String){
        LevelledMobs.lmInstance.logger.info(msg)
    }

    fun war(msg: String, suggestSupport: Boolean = false){
        LevelledMobs.lmInstance.logger.warning(
            msg + if (!suggestSupport) "" else " If (despite multiple attempts) you are unable to fix this issue, feel free to " +
                    "contact our support team for assistance."
            )
    }

    fun sev(msg: String, suggestSupport: Boolean = false){
        LevelledMobs.lmInstance.logger
            .severe(msg + if (!suggestSupport) "" else " Feel free to contact our support team if assistance is required."
            )
    }

    fun debug(
        cat: DebugCategory,
        msgSupplier: Supplier<String>
    ){
        if (!DebugHandler.isCategoryEnabled(cat)) return

        val msg = msgSupplier.get()

        if (DebugHandler.isCategoryEnabled(DebugCategory.BROADCAST_TO_OPS)) {
            for (player in Bukkit.getOnlinePlayers()) {
                if (!player.isOp) continue
                player.sendMessage(
                    ChatColor.DARK_GRAY.toString() + "[LM DEBUG : " + cat.name + "] " + msg
                )
            }
        }

        inf("[DEBUG : " + cat.name + "] " + msg)
    }
}