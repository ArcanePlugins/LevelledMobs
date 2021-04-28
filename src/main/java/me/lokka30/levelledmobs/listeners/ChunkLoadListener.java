package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
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
            LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) entity, main);

            // Make sure they aren't levelled
            if (lmEntity.isLevelled()) continue;

            // Make sure the config says they are levellable
            if (main.levelInterface.getLevellableState(lmEntity) != LevelInterface.LevellableState.ALLOWED)
                continue;

            // Make the mob a levelled mob.
            main.levelInterface.applyLevelToMob(
                    lmEntity,
                    main.levelInterface.generateLevel(lmEntity),
                    false,
                    false,
                    new HashSet<>(Collections.singletonList(LevelInterface.AdditionalLevelInformation.FROM_CHUNK_LISTENER))
            );
        }
    }
}
