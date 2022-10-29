package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

/**
 * This event is called whenever LevelledMobs is parsing an Action in a process, and it is looking
 * for a plugin (including LevelledMobs itself) to take ownership for a given action identifier.
 *
 * For example, if an addon plugin to LevelledMobs added a few actions, they should listen for
 * this event and make claims to to this event when the {@code identifier} belongs to them.
 */
public class ActionParseEvent extends Event implements Cancellable {

    /* vars */

    /**
     * identifier used for the action being parsed
     */
    private final String identifier;

    /**
     * the process which the action is being parsed for
     */
    private final Process process;

    /**
     * the config node being parsed
     */
    private final CommentedConfigurationNode node;

    /**
     * whether the action in this parse event has been claimed
     */
    private boolean claimed = false;

    /**
     * whether this event has been cancelled if so, then plugins should not add any actions to the
     * process (ignoreCancelled in event handler)
     */
    private boolean cancelled = false;

    private static final HandlerList HANDLERS = new HandlerList();

    /* constructors */

    public ActionParseEvent(
        final @NotNull String identifier,
        final @NotNull Process process,
        final @NotNull CommentedConfigurationNode node
    ) {
        this.identifier = identifier;
        this.process = process;
        this.node = node;
    }

    /* getters and setters */

    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    @NotNull
    public Process getProcess() { return process; }

    @NotNull
    public CommentedConfigurationNode getNode() { return node; }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(final boolean state) {
        this.claimed = state;
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
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() { return HANDLERS; }
}
