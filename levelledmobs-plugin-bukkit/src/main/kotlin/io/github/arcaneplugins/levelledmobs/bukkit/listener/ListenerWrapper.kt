package io.github.arcaneplugins.levelledmobs.bukkit.listener

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import org.bukkit.Bukkit
import org.bukkit.event.Listener

/**
 * Create a new listener.
 * <p>
 * 'Imperative' listeners are required by LevelledMobs in order for the plugin to operate
 * correctly, OR, they are considered very stable events which should not see breaking
 * changes (that affect LevelledMobs).
 * Imperative listeners must also be operational on all
 * versions of Minecraft which LevelledMobs is considered 'compatible'.
 * Almost every possible listener usable by LevelledMobs would be considered imperative due to
 * the brilliant (sometimes, frustrating) stability of the Bukkit API.
 *
 * @param imperative whether the listener is imperative
 */
abstract class ListenerWrapper(
    val imperative: Boolean
) : Listener {
    /**
     * Attempt to register this listener
     *
     * @return {@code false} if registration failed, AND the listener is considered 'imperative'.
     */
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