package me.lokka30.levelledmobs.listeners;

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.QueueItem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for when MythicMobs are spawned so they can be marked accordingly
 * since the regular spawn event doesn't distinguish MythicMobs
 *
 * @author stumper66
 */
public class MythicMobsListener implements Listener {

    private final LevelledMobs main;

    public MythicMobsListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onMythicMobSpawnEvent(@NotNull final MythicMobSpawnEvent event) {
        if (!(event.getEntity() instanceof LivingEntity )) return;
        final LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) event.getEntity(), main);

        lmEntity.mythicMobInternalName = event.getMob().getType().getInternalName();
        lmEntity.setMobExternalType(ExternalCompatibilityManager.ExternalCompatibility.MYTHIC_MOBS);

        if (ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibilityManager.ExternalCompatibility.MYTHIC_MOBS, lmEntity)) {
            lmEntity.reEvaluateLevel = true;
            main.queueManager_mobs.addToQueue(new QueueItem(lmEntity, event));
            return;
        }

        if (lmEntity.isLevelled()) main.levelInterface.removeLevel(lmEntity);
        lmEntity.getPDC().set(main.levelManager.noLevelKey, PersistentDataType.STRING, "true");
    }
}
