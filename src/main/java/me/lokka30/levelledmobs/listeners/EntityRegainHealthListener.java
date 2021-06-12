package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for when an entity regains health so the nametag can
 * be updated accordingly
 *
 * @author konsolas, lokka30
 */
public class EntityRegainHealthListener implements Listener {

    private final LevelledMobs main;

    public EntityRegainHealthListener(final LevelledMobs main) {
        this.main = main;
    }

    // When the mob regains health, try to update their nametag.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityRegainHealth(@NotNull final EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        final LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) event.getEntity(), main);

        // Make sure the mob is levelled
        if (!lmEntity.isLevelled()) return;

        main.levelManager.updateNametag_WithDelay(lmEntity);
    }

}
