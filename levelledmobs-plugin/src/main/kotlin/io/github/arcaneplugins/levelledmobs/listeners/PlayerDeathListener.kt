package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.listeners.paper.PlayerDeathListener
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import org.bukkit.Bukkit
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
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
    private var lastPriority: EventPriority? = null
    private val settingName = "player-death-event"

    fun load(){
        if (LevelledMobs.instance.ver.isRunningPaper && paperListener == null) {
            paperListener = PlayerDeathListener()
        }

        val priority = LevelledMobs.instance.mainCompanion.getEventPriority(settingName, EventPriority.NORMAL)
        if (lastPriority != null){
            if (priority == lastPriority) return

            HandlerList.unregisterAll(this)
            Log.inf("Changing event priority for $settingName from $lastPriority to $priority")
        }

        Bukkit.getPluginManager().registerEvent(
            PlayerDeathEvent::class.java,
            this,
            priority,
            { _, event -> if (event is PlayerDeathEvent) onPlayerDeath(event) },
            LevelledMobs.instance,
            false
        )
        lastPriority = priority
    }

    /**
     * This listener handles death nametags so we can determine which mob killed it and update the
     * death message accordingly
     *
     * @param event PlayerDeathEvent
     */
    private fun onPlayerDeath(event: PlayerDeathEvent) {
        // returns false if not a translatable component, in which case just use the old method
        // this can happen if another plugin has butchered the event by using the deprecated method (*cough* mythic mobs)
        if (!LevelledMobs.instance.ver.isRunningPaper || !paperListener!!.onPlayerDeathEvent(event)) {
            nonPaperPlayerDeath(event)
        }
    }

    private fun nonPaperPlayerDeath(event: PlayerDeathEvent) {
        val lmEntity = SpigotUtils.getPlayersKiller(event)

        if (LevelledMobs.instance.placeholderApiIntegration != null) {
            LevelledMobs.instance.placeholderApiIntegration!!.putPlayerOrMobDeath(event.entity, lmEntity, true)
            return
        }

        lmEntity?.free()
    }
}