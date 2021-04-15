package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

/**
 * TODO Describe...
 *
 * @author konsolas
 * @contributors lokka30
 */
public class EntityRegainHealthListener implements Listener {

    private final LevelledMobs main;

    public EntityRegainHealthListener(final LevelledMobs main) {
        this.main = main;
    }

    // When the mob regains health, try to update their nametag.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityRegainHealth(final EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            final LivingEntity livingEntity = (LivingEntity) event.getEntity();

            // Make sure the mob is levelled
            if (!main.levelInterface.isLevelled(livingEntity)) return;

            main.levelManager.updateNametagWithDelay(livingEntity, livingEntity.getWorld().getPlayers(), 1);
        }
    }

}
