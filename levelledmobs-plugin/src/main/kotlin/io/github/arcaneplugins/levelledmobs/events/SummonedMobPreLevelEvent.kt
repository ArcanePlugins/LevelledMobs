package io.github.arcaneplugins.levelledmobs.events

import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is fired *before* a mob has been levelled, but only if it was spawned using the `/lm
 * summon` command.
 *
 * @author lokka30
 * @since 2.5.0
 */
class SummonedMobPreLevelEvent(
    val entity: LivingEntity,
    var level: Int
): Event(!Bukkit.isPrimaryThread()), Cancellable {
    private var cancelled = false

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }

    companion object{
        private val HANDLERS = HandlerList()

        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }
}