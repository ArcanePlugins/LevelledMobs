package me.lokka30.levelledmobs.bukkit.event.function;

import me.lokka30.levelledmobs.bukkit.logic.function.LmFunction;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class FunctionRunEvent extends Event implements Cancellable {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();
    private final LmFunction function;
    private boolean cancelled = false;

    /* constructors */

    public FunctionRunEvent(final @NotNull LmFunction function) {
        this.function = function;
    }

    /* getters and setters */

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

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
}
