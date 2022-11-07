package io.github.arcaneplugins.levelledmobs.bukkit.listener;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import javax.annotation.Nonnull;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
        final EntityType type = event.getEntityType();

        if(type == EntityType.CREEPER && entity instanceof Creeper creeper) {
            final Double mult = EntityDataUtil
                .getCreeperBlastRadiusMultiplier(creeper, true);

            if(mult != null) {
                event.setYield(event.getYield() * mult.floatValue());
            }
        }

        /*
        Fire the associated trigger.
         */
        LevelledMobs.getInstance().getLogicHandler().runFunctionsWithTriggers(
            new Context().withEntity(entity), "on-entity-explode"
        );
    }
}
