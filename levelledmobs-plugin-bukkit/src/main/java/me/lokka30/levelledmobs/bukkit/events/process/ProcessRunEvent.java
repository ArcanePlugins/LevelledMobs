package me.lokka30.levelledmobs.bukkit.events.process;

import me.lokka30.levelledmobs.bukkit.logic.Process;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class ProcessRunEvent extends Event implements Cancellable {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();

    private final Process process;
    private boolean isCancelled = false;

    /* constructors */

    public ProcessRunEvent(
        final @NotNull Process process
    ) {
        this.process = process;
    }

    /* getters and setters */

    @NotNull
    public Process getProcess() { return process; }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean state) {
        this.isCancelled = state;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
