package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.persistence.PersistentDataType;

public class EntityRegainHealthListener implements Listener {

    private final LevelledMobs instance;

    public EntityRegainHealthListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    // When the mob regains health, try to update their nametag.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityRegainHealth(final EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) event.getEntity();

            // Make sure the mob is levelled
            if (!entity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING))
                return;

            instance.levelManager.updateNametagWithDelay(entity);
        }
    }

}
