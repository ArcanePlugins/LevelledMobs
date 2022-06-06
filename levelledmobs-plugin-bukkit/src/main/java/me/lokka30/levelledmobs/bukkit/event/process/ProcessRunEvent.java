package me.lokka30.levelledmobs.bukkit.event.process;

import me.lokka30.levelledmobs.bukkit.logic.Process;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class ProcessRunEvent extends CancellableProcessEvent {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();

    /* constructors */

    public ProcessRunEvent(final @NotNull Process process) {
        super(process);
    }

    /* getters and setters */

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}