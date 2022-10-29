package io.github.arcaneplugins.levelledmobs.bukkit.logic.levelling.strategy;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

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
@SuppressWarnings("unused")
public class LevellingStrategyRequestEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final String strategyId;
    private final CommentedConfigurationNode strategyNode;
    private boolean claimed = false;
    private boolean cancelled = false;
    private final Set<LevellingStrategy> strategies = new HashSet<>();

    public LevellingStrategyRequestEvent(
        final @NotNull String strategyId,
        final @NotNull CommentedConfigurationNode strategyNode
    ) {
        this.strategyId = Objects.requireNonNull(strategyId, "strategyId");
        this.strategyNode = Objects.requireNonNull(strategyNode, "strategyNode");
    }

    @NotNull
    public CommentedConfigurationNode getStrategyNode() {
        return strategyNode;
    }

    @NotNull
    public Set<LevellingStrategy> getStrategies() {
        return strategies;
    }

    @NotNull
    public String getStrategyId() {
        return strategyId;
    }

    public void claim() {
        claimed = true;
    }

    public boolean isClaimed() {
        return claimed;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean state) {
        this.cancelled = state;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
