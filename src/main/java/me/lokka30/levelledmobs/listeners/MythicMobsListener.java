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
    private final LevelledMobs instance;

    public MythicMobsListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onMythicMobSpawnEvent(MythicMobSpawnEvent event){
        if (!instance.externalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.MYTHIC_MOBS))
            return;

        if (!(event.getEntity() instanceof LivingEntity)) return;

        instance.levelManager.creatureSpawnListener.processMobSpawn(
                (LivingEntity) event.getEntity(), CreatureSpawnEvent.SpawnReason.CUSTOM, -1, MobProcessReason.NONE, false
        );
    }
}
