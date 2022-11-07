package io.github.arcaneplugins.levelledmobs.bukkit.listener;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreedEvent;

public class EntityBreedListener extends ListenerWrapper {

    public EntityBreedListener() {
        super(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void handle(final EntityBreedEvent event) {
        final LivingEntity child = event.getEntity();
        final LivingEntity father = event.getFather();
        final LivingEntity mother = event.getMother();

        InternalEntityDataUtil.setFather(child, father, true);
        InternalEntityDataUtil.setMother(child, mother, true);
        InternalEntityDataUtil.setWasBred(child, true, true);

        LevelledMobs.getInstance().getLogicHandler().runFunctionsWithTriggers(
            new Context()
                .withEntity(child)
                .withFather(father)
                .withMother(mother)
            , "on-entity-breed"
        );
    }
}
