package me.lokka30.levelledmobs.bukkit.events.groups;

import me.lokka30.levelledmobs.bukkit.logic.Group;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

//TODO this should not be cancellable!
public final class GroupPostParseEvent extends CancellableGroupEvent {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();

    /* constructors */

    public GroupPostParseEvent(final @NotNull Group group) {
        super(group);
    }

    /* getters and setters */

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
