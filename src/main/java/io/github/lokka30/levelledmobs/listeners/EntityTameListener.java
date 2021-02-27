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
        Utils.debugLog(instance, "EntityTameListener", "Listening for tame events");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityTameEvent(EntityTameEvent event) {
        final LivingEntity le = event.getEntity();

        if (instance.settingsCfg.getBoolean("no-level-conditions.tamed")) {

            // if mob was levelled then remove it

            if (le.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING))
                le.getPersistentDataContainer().remove(instance.levelManager.isLevelledKey);
            if (le.getPersistentDataContainer().has(instance.levelManager.levelKey, PersistentDataType.INTEGER))
                le.getPersistentDataContainer().remove(instance.levelManager.levelKey);

            instance.levelManager.updateNametagWithDelay(le, le.getCustomName(), le.getWorld().getPlayers(), 1);

            return;
        }

        int level = -1;
        if (le.getPersistentDataContainer().has(instance.levelManager.levelKey, PersistentDataType.INTEGER)) {
            Object temp = le.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER);
            if (temp != null) level = (int) temp;
        }

        int finalLevel = level;
        new BukkitRunnable() {
            public void run() {
                instance.levelManager.creatureSpawnListener.processMobSpawn(le, CreatureSpawnEvent.SpawnReason.DEFAULT, finalLevel, MobProcessReason.TAME_EVENT, false);
            }
        }.runTaskLater(instance, 1L);
    }
}
