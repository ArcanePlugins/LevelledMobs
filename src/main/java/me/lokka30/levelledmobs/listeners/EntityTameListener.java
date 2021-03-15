package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.MobProcessReason;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author stumper66
 * @contributors lokka30
 */
public class EntityTameListener implements Listener {

    private final LevelledMobs main;

    public EntityTameListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onEntityTameEvent(EntityTameEvent event) {
        final LivingEntity livingEntity = event.getEntity();

        if (main.settingsCfg.getBoolean("no-level-conditions.tamed")) {
            Utils.debugLog(main, "EntityTameListener", "no-level-conditions.tamed = true");

            // if mob was levelled then remove it
            if (livingEntity.getPersistentDataContainer().has(main.levelManager.levelKey, PersistentDataType.INTEGER))
                livingEntity.getPersistentDataContainer().remove(main.levelManager.levelKey);

            main.levelManager.updateNametagWithDelay(livingEntity,
                    livingEntity.getCustomName(),
                    livingEntity.getWorld().getPlayers(),
                    1);

            Utils.debugLog(main, "EntityTameListener", "Removed level of tamed mob");
            return;
        }

        Utils.debugLog(main, "EntityTameListener", "Applying level to tamed mob");
        int level = -1;
        if (livingEntity.getPersistentDataContainer().has(main.levelManager.levelKey, PersistentDataType.INTEGER)) {
            Object temp = livingEntity.getPersistentDataContainer().get(main.levelManager.levelKey, PersistentDataType.INTEGER);
            if (temp != null) level = (int) temp;
        }

        int finalLevel = level;
        new BukkitRunnable() {
            public void run() {
                main.levelManager.creatureSpawnListener.processMobSpawn(livingEntity, CreatureSpawnEvent.SpawnReason.DEFAULT, finalLevel, MobProcessReason.TAME, false);
            }
        }.runTaskLater(main, 1L);
    }
}
