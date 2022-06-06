package me.lokka30.levelledmobs.bukkit.event.action;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.logic.Action;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ActionPostParseEvent extends Event implements ActionEvent {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();
    private final Action action;

    /* constructors */

    public ActionPostParseEvent(final @NotNull Action action) {
        this.action = Objects.requireNonNull(action, "action");
    }

    /* getters and setters */

    @Override
    @NotNull
    public Action getAction() { return action; }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
