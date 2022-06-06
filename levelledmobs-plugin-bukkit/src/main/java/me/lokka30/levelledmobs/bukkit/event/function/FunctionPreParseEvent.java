package me.lokka30.levelledmobs.bukkit.event.function;

import me.lokka30.levelledmobs.bukkit.logic.LmFunction;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class FunctionPreParseEvent extends CancellableFunctionEvent {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();

    /* constructors */

    public FunctionPreParseEvent(final @NotNull LmFunction function) { super(function); }

    /* getters and setters */

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
