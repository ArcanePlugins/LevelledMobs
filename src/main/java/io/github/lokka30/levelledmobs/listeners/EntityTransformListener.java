package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class EntityTransformListener implements Listener {

    private final LevelledMobs instance;

    public EntityTransformListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTransform(final EntityTransformEvent event) {

        // is the original entity a living entity
        if (!(event.getEntity() instanceof LivingEntity)) return;

        final LivingEntity livingEntity = (LivingEntity) event.getEntity();

        // is the original entity levelled
        if (!livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING))
            return;

        final int level = Objects.requireNonNull(livingEntity.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER));

        for (Entity transformedEntity : event.getTransformedEntities()) {
            if (transformedEntity instanceof LivingEntity) {
                final LivingEntity transformedLivingEntity = (LivingEntity) transformedEntity;

                if (!instance.levelManager.isLevellable(transformedLivingEntity)) {
                    instance.levelManager.updateNametagWithDelay(transformedLivingEntity, "");
                    continue;
                }

                instance.levelManager.creatureSpawnListener.processMobSpawn(transformedLivingEntity, CreatureSpawnEvent.SpawnReason.CUSTOM, level);
            }
        }
    }
}
