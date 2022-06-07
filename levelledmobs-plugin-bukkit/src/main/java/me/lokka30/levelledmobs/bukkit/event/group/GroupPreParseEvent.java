package me.lokka30.levelledmobs.bukkit.event.group;

import me.lokka30.levelledmobs.bukkit.logic.Group;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class GroupPreParseEvent extends Event implements Cancellable {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();
    private final Group group;
    private boolean cancelled = false;

    /* constructors */

    public GroupPreParseEvent(final @NotNull Group group) {
        this.group = group;
    }

    /* getters and setters */

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public Group getGroup() { return group; }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean state) {
        this.cancelled = state;
    }
}
