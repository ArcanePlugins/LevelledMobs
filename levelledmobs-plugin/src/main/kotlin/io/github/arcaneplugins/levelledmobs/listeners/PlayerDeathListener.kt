package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.listeners.paper.PlayerDeathListener
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

/**
 * Listens for when a player dies
 *
 * @author stumper66
 * @since 2.6.0
 */
class PlayerDeathListener : Listener {
    private var paperListener: PlayerDeathListener? = null
    init {
        if (LevelledMobs.instance.ver.isRunningPaper) {
            paperListener = PlayerDeathListener()
        }
    }

    /**
     * This listener handles death nametags so we can determine which mob killed it and update the
     * death message accordingly
     *
     * @param event PlayerDeathEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        // returns false if not a translatable component, in which case just use the old method
        // this can happen if another plugin has butchered the event by using the deprecated method (*cough* mythic mobs)
        if (!LevelledMobs.instance.ver.isRunningPaper || !paperListener!!.onPlayerDeathEvent(event)) {
            nonPaper_PlayerDeath(event)
        }
    }

    private fun nonPaper_PlayerDeath(event: PlayerDeathEvent) {
        val lmEntity = SpigotUtils.getPlayersKiller(event)

        if (LevelledMobs.instance.placeholderApiIntegration != null) {
            LevelledMobs.instance.placeholderApiIntegration!!.putPlayerOrMobDeath(event.entity, lmEntity, true)
            return
        }

        lmEntity?.free()
    }
}