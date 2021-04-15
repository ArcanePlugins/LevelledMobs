package me.lokka30.levelledmobs.listeners;

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;

/**
 * TODO Describe...
 *
 * @author stumper66
 */
public class MythicMobsListener implements Listener {

    private final LevelledMobs main;

    public MythicMobsListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onMythicMobSpawnEvent(MythicMobSpawnEvent event) {
        if (!ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.MYTHIC_MOBS))
            return;

        if (!(event.getEntity() instanceof LivingEntity)) return;

        final LivingEntity livingEntity = (LivingEntity) event.getEntity();

        livingEntity.getPersistentDataContainer().set(main.levelManager.noLevelKey, PersistentDataType.STRING, "true");
    }
}
