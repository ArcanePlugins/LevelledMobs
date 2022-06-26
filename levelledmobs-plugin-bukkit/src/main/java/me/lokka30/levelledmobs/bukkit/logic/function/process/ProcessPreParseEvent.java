package me.lokka30.levelledmobs.bukkit.logic.function.process;

import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class ProcessPreParseEvent extends Event implements Cancellable {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();
    private final Process process;
    private boolean cancelled = false;

    /* constructors */

    public ProcessPreParseEvent(final @NotNull Process process) {
        this.process = process;
    }

    /* getters and setters */


    @NotNull
    public Process getProcess() { return process; }

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