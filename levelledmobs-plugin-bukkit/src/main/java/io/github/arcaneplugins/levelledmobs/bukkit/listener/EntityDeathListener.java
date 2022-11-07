package io.github.arcaneplugins.levelledmobs.bukkit.listener;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import javax.annotation.Nonnull;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import redempt.crunch.Crunch;

public class EntityDeathListener extends ListenerWrapper {

    public EntityDeathListener() {
        super(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void handle(final @Nonnull EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();

        if(EntityDataUtil.isLevelled(entity, true)) {
            handleCustomDrops(event);
            handleItemDrops(event);
            handleExpDrops(event);
        }

        LevelledMobs.getInstance().getLogicHandler().runFunctionsWithTriggers(
            new Context().withEntity(entity), "on-entity-death"
        );
    }

    private void handleCustomDrops(final @Nonnull EntityDeathEvent event) {
        //TODO
    }

    private void handleItemDrops(final @Nonnull EntityDeathEvent event) {
        //TODO
    }

    private void handleExpDrops(final @Nonnull EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();

        final String multFormula = EntityDataUtil
            .getExpDropMultiplierFormula(entity, true);

        if(multFormula == null || multFormula.isBlank()) return;

        final double eval = Crunch.evaluateExpression(
            LogicHandler.replacePapiAndContextPlaceholders(
                multFormula,
                new Context().withEntity(entity)
            )
        );

        event.setDroppedExp(event.getDroppedExp() * (int) eval);
    }

}
