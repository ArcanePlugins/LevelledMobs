package me.lokka30.levelledmobs.bukkit.event.condition;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.event.CancellableEvent;
import me.lokka30.levelledmobs.bukkit.logic.Condition;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ConditionPreParseEvent extends CancellableEvent implements ConditionEvent {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();
    private final Condition condition;

    /* constructors */

    public ConditionPreParseEvent(final @NotNull Condition condition) {
        this.condition = Objects.requireNonNull(condition, "condition");
    }

    /* getters and setters */

    @Override
    @NotNull
    public Condition getCondition() { return condition; }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
