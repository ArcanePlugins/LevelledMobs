package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.QueueItem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

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
            final LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) entity, main);

            if (lmEntity.isLevelled()) continue;

            main.queueManager_mobs.addToQueue(new QueueItem(lmEntity, event));
        }
    }
}
