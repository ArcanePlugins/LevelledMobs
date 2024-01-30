package io.github.arcaneplugins.levelledmobs.events

import io.github.arcaneplugins.levelledmobs.result.AdditionalLevelInformation
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is fired *after* a mob is levelled. Other plugins can cancel this event.
 *
 * @author lokka30
 * @since 2.5.0
 */
class MobPostLevelEvent(
    val lmEntity: LivingEntityWrapper,
    val levelCause: LevelCause,
    val additionalInformation: MutableSet<AdditionalLevelInformation?>
) : Event(!Bukkit.isPrimaryThread()) {
    companion object {
        private val HANDLERS = HandlerList()

        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }

    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    /**
     * When a mob is levelled, the following enum is used to allow plugins to find the cause of the
     * mob being levelled.
     *
     *
     * NORMAL: Spawned naturally, by a spawn egg, etc. CHANGED_LEVEL: When an existing levelled mob
     * has its level changed.
     */
    enum class LevelCause {
        NORMAL,
        CHANGED_LEVEL,
        SUMMONED
    }

    val entity: LivingEntity
        get() = lmEntity.livingEntity

    val level: Int
        get() = lmEntity.getMobLevel()
}