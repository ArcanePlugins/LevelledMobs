package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.Collections;
import java.util.HashSet;

/**
 * TODO Describe...
 *
 * @author stumper66
 * @contributors lokka30
 */
public class ChunkLoadListener implements Listener {

    private final LevelledMobs main;
    public ChunkLoadListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(final ChunkLoadEvent event) {

        if (!main.settingsCfg.getBoolean("ensure-mobs-are-levelled-on-chunk-load", true)) return;

        // Check each entity in the chunk
        for (final Entity entity : event.getChunk().getEntities()) {

            // Must be a *living* entity
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity livingEntity = (LivingEntity) entity;

            // Make sure they aren't levelled
            if (main.levelInterface.isLevelled(livingEntity)) continue;

            // Make sure the config says they are levellable
            if (main.levelInterface.getLevellableState(livingEntity) != LevelInterface.LevellableState.ALLOWED)
                continue;

            // Make the mob a levelled mob.
            main.levelInterface.applyLevelToMob(livingEntity, main.levelInterface.generateLevel(livingEntity), false, false, new HashSet<>(Collections.singletonList(LevelInterface.AdditionalLevelInformation.FROM_CHUNK_LISTENER)));
        }
    }
}
