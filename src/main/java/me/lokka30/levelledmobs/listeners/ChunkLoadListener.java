package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.MobProcessReason;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkLoadListener implements Listener {
    private final LevelledMobs instance;

    public ChunkLoadListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(final ChunkLoadEvent event) {

        if (!instance.settingsCfg.getBoolean("ensure-mobs-are-levelled-on-chunk-load", true)) return;

        // Check each entity in the chunk
        for (final Entity entity : event.getChunk().getEntities()) {

            // Must be a *living* entity
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity livingEntity = (LivingEntity) entity;

            // Make sure they aren't levelled
            if (instance.levelInterface.isLevelled(livingEntity)) continue;

            // Make sure the config says they are levellable
            if (instance.levelInterface.getLevellableState(livingEntity) != LevelInterface.LevellableState.ALLOWED)
                continue;

            //TODO move the following code
            // For some reason they aren't levelled - let's fix that!
            final int mobLevel = instance.levelManager.creatureSpawnListener.processMobSpawn(livingEntity, CreatureSpawnEvent.SpawnReason.DEFAULT, -1, MobProcessReason.NONE, false);
            if (mobLevel >= 0 && instance.settingsCfg.getBoolean("use-custom-item-drops-for-mobs"))
                instance.levelManager.creatureSpawnListener.processMobEquipment(livingEntity, mobLevel);
        }
    }
}
