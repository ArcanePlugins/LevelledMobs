package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;

public class EntityDeathListener implements Listener {

    private final LevelledMobs instance;

    public EntityDeathListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    // These entities will be forced not to have levelled drops
    final EnumSet<EntityType> bypassDrops = EnumSet.of(EntityType.ARMOR_STAND);

    @EventHandler(ignoreCancelled = true)
    public void onDeath(final EntityDeathEvent event) {
        if (bypassDrops.contains(event.getEntityType())) {
            return;
        }

        final LivingEntity livingEntity = event.getEntity();

        if (livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING)) {
            instance.levelManager.setLevelledDrops(event.getEntity(), event.getDrops());
            event.setDroppedExp(instance.levelManager.setLevelledXP(event.getEntity(), event.getDroppedExp()));
        }
    }
}
