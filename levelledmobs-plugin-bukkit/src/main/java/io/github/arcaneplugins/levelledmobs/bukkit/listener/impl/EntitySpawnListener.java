package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.keys.EntityKeyStore;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDropHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.cdevent.CustomDropsEventType;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.CommandCustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.StandardCustomDropType;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntitySpawnEvent;

public final class EntitySpawnListener extends ListenerWrapper {

    /*
    Constructors
     */

    public EntitySpawnListener() {
        super(true);
    }

    /*
    Methods
     */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void handle(final EntitySpawnEvent event) {

        /*
        LevelledMobs only concerns LivingEntities
         */
        if(!(event.getEntity() instanceof final LivingEntity entity))
            return;

        /*
        Check if the entity has any non-persistent metadata to migrate
         */
        // wasSummoned
        final String wasSummonedKeyStr = EntityKeyStore.WAS_SUMMONED.toString();
        InternalEntityDataUtil.setWasSummoned(
            entity,
            entity.hasMetadata(wasSummonedKeyStr) &&
                entity.getMetadata(wasSummonedKeyStr).stream().anyMatch(val -> val.asInt() == 1),
            true
        );
        entity.removeMetadata(wasSummonedKeyStr, LevelledMobs.getInstance());

        /*
        Fire the associated trigger.
         */
        LogicHandler.runFunctionsWithTriggers(
            new Context().withEntity(entity), "on-entity-spawn"
        );

        /*
        Custom Drops
         */
        handleCustomDrops(event);
    }

    private static void handleCustomDrops(
        final @Nonnull EntitySpawnEvent event
    ) {
        // This is a safe cast since LM will only call this after it has verified this is a LivngEnt
        final LivingEntity entity = (LivingEntity) event.getEntity();

        final Context context = new Context()
            .withEntity(entity)
            .withEvent(event);

        final Collection<CustomDrop> cds =
            CustomDropHandler.getDefinedCustomDropsForEntity(entity);

        for(final @Nonnull CustomDrop cd : cds) {
            if(cd.getType().equals(StandardCustomDropType.ITEM.name())) {
                final ItemCustomDrop icd = (ItemCustomDrop) cd;
                icd.attemptToApplyEquipment(entity);
            } else if(cd.getType().equalsIgnoreCase(StandardCustomDropType.COMMAND.name())) {
                final CommandCustomDrop ccd = (CommandCustomDrop) cd;
                if(ccd.getCommandRunEvents().contains(CustomDropsEventType.ON_SPAWN.name())) {
                    ccd.execute(CustomDropsEventType.ON_SPAWN, context);
                }
            }
        }
    }
}
