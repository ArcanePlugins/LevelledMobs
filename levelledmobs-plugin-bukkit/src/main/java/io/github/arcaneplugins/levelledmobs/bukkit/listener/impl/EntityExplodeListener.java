package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import javax.annotation.Nonnull;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;

public class EntityExplodeListener extends ListenerWrapper {

    public EntityExplodeListener() {
        super(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void handle(final @Nonnull EntityExplodeEvent event) {
        final Entity entity = event.getEntity();

        /*
        Fire the associated trigger.
         */
        LogicHandler.runFunctionsWithTriggers(
            new Context().withEntity(entity), "on-entity-explode"
        );
    }
}
