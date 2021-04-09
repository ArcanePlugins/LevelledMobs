package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.persistence.PersistentDataType;

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
            main.levelInterface.removeLevel(livingEntity);

            Utils.debugLog(main, "EntityTameListener", "Removed level of tamed mob");
            return;
        }

        Utils.debugLog(main, "EntityTameListener", "Applying level to tamed mob");
        int level = -1;
        if (livingEntity.getPersistentDataContainer().has(main.levelManager.levelKey, PersistentDataType.INTEGER)) {
            Object temp = livingEntity.getPersistentDataContainer().get(main.levelManager.levelKey, PersistentDataType.INTEGER);
            if (temp != null) level = (int) temp;
        }

        main.levelInterface.applyLevelToMob(livingEntity, level, false, false);
    }
}
