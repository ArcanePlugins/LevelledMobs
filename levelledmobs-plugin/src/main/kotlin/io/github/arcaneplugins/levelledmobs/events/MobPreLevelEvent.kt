package io.github.arcaneplugins.levelledmobs.events

import io.github.arcaneplugins.levelledmobs.result.AdditionalLevelInformation
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is fired *before* a mob has been levelled. Note that it does not fire when the mob was
 * spawned using `/lm summon`, instead see Summoned- -MobPreLevelEvent.
 *
 * @author lokka30
 * @since 2.5.0
 */
class MobPreLevelEvent(
    val entity: LivingEntity,
    var level: Int,
    val levelCause: LevelCause,
    val additionalInformation: MutableSet<AdditionalLevelInformation>?
) : Event(!Bukkit.isPrimaryThread()), Cancellable {
    var showLMNametag = true
    private var cancelled = false

    /**
     * When a mob is levelled, the following enum is used to allow plugins to find the cause of the
     * mob being levelled.
     * <p>
     * NORMAL: Spawned naturally, by a spawn egg, etc. CHANGED_LEVEL: When an existing levelled mob
     * has its level changed.
     */
    enum class LevelCause {
        NORMAL,
        CHANGED_LEVEL
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    companion object{
        private val HANDLERS = HandlerList()

        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }


}