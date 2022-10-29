package io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import java.util.Set;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ContextPlaceholdersLoadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    @NotNull
    public Set<ContextPlaceholder> getContextPlaceholders() {
        return LevelledMobs.getInstance()
            .getLogicHandler()
            .getContextPlaceholderHandler()
            .getContextPlaceholders();
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
