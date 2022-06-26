package me.lokka30.levelledmobs.bukkit.logic.function;

import me.lokka30.levelledmobs.bukkit.logic.function.LmFunction;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

//TODO this should not be cancellable!
public final class FunctionPostParseEvent extends Event implements Cancellable {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();
    private final LmFunction function;
    private boolean cancelled = false;

    /* constructors */

    public FunctionPostParseEvent(final @NotNull LmFunction function) {
        this.function = function;
    }

    /* getters and setters */

    @NotNull
    public LmFunction getFunction() { return function; }

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
