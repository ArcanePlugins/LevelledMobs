package me.lokka30.levelledmobs.bukkit.event.group;

import me.lokka30.levelledmobs.bukkit.logic.Group;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class GroupPreParseEvent extends CancellableGroupEvent {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();

    /* constructors */

    public GroupPreParseEvent(final @NotNull Group group) {
        super(group);
    }

    /* getters and setters */

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
