package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.spongepowered.configurate.CommentedConfigurationNode

/**
 * This event is fired whenever something (such as the SetLevelAction) is requesting levelling strategy
 * objects (one or more) for a particular strategyId.
 * <p>
 * If a levelling strategy identifies that a given strategyId belongs to itself, then it should 'claim'
 * the event with given strategyId so that other listeners are informed that the event has been
 * successfully handled. A claim should result in one or more levelling strategies being added to the
 * strategies set.
 * <p>
 * Prior to running other code, listeners of this event should immediately 'return' if the event is
 * claimed and/or cancelled.
 */
class LevellingStrategyRequestEvent(
    val strategyId: String,
    val strategyNode: CommentedConfigurationNode
): Event() {
   companion object val HANDLER_LIST = HandlerList()
    var claimed = false
    var cancelled = false
    val strategies = mutableSetOf<LevellingStrategy>()

    override fun getHandlers(): HandlerList {
        return HANDLER_LIST
    }
}