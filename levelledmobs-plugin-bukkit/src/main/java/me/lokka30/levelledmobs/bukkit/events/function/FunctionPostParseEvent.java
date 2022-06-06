package me.lokka30.levelledmobs.bukkit.events.function;

import me.lokka30.levelledmobs.bukkit.logic.LmFunction;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

//TODO this should not be cancellable!
public final class FunctionPostParseEvent extends CancellableFunctionEvent {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();

    /* constructors */

    public FunctionPostParseEvent(final @NotNull LmFunction function) { super(function); }

    /* getters and setters */

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
