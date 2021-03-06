package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.misc.MobProcessReason;
import io.github.lokka30.levelledmobs.misc.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class EntityTameListener implements Listener {

    private final LevelledMobs instance;

    public EntityTameListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityTameEvent(EntityTameEvent event) {
        final LivingEntity livingEntity = event.getEntity();

        if (instance.settingsCfg.getBoolean("no-level-conditions.tamed")) {
            Utils.debugLog(instance, "EntityTameListener", "no-level-conditions.tamed = true");

            // if mob was levelled then remove it

            if (livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING))
                livingEntity.getPersistentDataContainer().remove(instance.levelManager.isLevelledKey);
            if (livingEntity.getPersistentDataContainer().has(instance.levelManager.levelKey, PersistentDataType.INTEGER))
                livingEntity.getPersistentDataContainer().remove(instance.levelManager.levelKey);

            instance.levelManager.updateNametagWithDelay(livingEntity,
                    livingEntity.getCustomName() == null ? "" : livingEntity.getCustomName(),
                    livingEntity.getWorld().getPlayers(),
                    1);

            Utils.debugLog(instance, "EntityTameListener", "Removed level of tamed mob");
            return;
        }

        Utils.debugLog(instance, "EntityTameListener", "Applying level to tamed mob");
        int level = -1;
        if (livingEntity.getPersistentDataContainer().has(instance.levelManager.levelKey, PersistentDataType.INTEGER)) {
            Object temp = livingEntity.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER);
            if (temp != null) level = (int) temp;
        }

        int finalLevel = level;
        new BukkitRunnable() {
            public void run() {
                instance.levelManager.creatureSpawnListener.processMobSpawn(livingEntity, CreatureSpawnEvent.SpawnReason.DEFAULT, finalLevel, MobProcessReason.TAME, false);
            }
        }.runTaskLater(instance, 1L);
    }
}
