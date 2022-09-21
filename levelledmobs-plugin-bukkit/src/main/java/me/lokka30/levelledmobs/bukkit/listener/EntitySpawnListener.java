package me.lokka30.levelledmobs.bukkit.listener;

import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.api.data.keys.EntityKeyStore;
import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
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
        var wasSummonedKeyStr = EntityKeyStore.WAS_SUMMONED.toString();
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
        LevelledMobs.getInstance().getLogicHandler().runFunctionsWithTriggers(
            new Context()
                .withEntity(entity)
                .withEntityType(entity.getType())
                .withWorld(entity.getWorld())
                .withLocation(entity.getLocation()),
            "on-entity-spawn", "on-mob-spawn"
        );
    }
}
