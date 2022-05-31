package me.lokka30.levelledmobs.bukkit.listeners;

import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.api.data.keys.EntityKeyStore;
import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntitySpawnEvent;

public final class EntitySpawnListener extends ListenerWrapper {

    public EntitySpawnListener() {
        super("org.bukkit.event.entity.EntitySpawnEvent", true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void handle(final EntitySpawnEvent event) {
        /*
        LevelledMobs only concerns LivingEntities
         */
        if(!(event.getEntity() instanceof LivingEntity))
            return;

        final LivingEntity entity = (LivingEntity) event.getEntity();

        /*
        Check if the entity has any non-persistent metadata to migrate
         */
        // wasSummoned
        var wasSummonedKeyStr = EntityKeyStore.wasSummoned.toString();
        InternalEntityDataUtil.setWasSummoned(
            entity,
            entity.hasMetadata(wasSummonedKeyStr) &&
                entity.getMetadata(wasSummonedKeyStr).stream().anyMatch(val -> val.asInt() == 1)
        );
        entity.removeMetadata(wasSummonedKeyStr, LevelledMobs.getInstance());
    }
}
