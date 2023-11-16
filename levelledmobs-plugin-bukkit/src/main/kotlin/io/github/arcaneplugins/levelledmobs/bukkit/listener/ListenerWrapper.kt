package io.github.arcaneplugins.levelledmobs.bukkit.listener

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import org.bukkit.Bukkit
import org.bukkit.event.Listener

abstract class ListenerWrapper(val imperative: Boolean) : Listener {
    fun register(): Boolean{
        try {
            Bukkit.getPluginManager().registerEvents(this, LevelledMobs.lmInstance)
        } catch (ex: Exception) {
            if (imperative) {
                sev(
                    "Unable to register listener '" + javaClass.getSimpleName() + "'. " +
                            "A stack trace will be printed below for debugging purposes.",
                    true
                )
                ex.printStackTrace()
                return false
            }
        }
        return true
    }
}