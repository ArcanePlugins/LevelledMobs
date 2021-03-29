package me.lokka30.levelledmobs.listeners;

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.MobProcessReason;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

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

        main.levelManager.creatureSpawnListener.processMobSpawn(
                (LivingEntity) event.getEntity(), CreatureSpawnEvent.SpawnReason.CUSTOM, -1, MobProcessReason.NONE, false
        );
    }
}
