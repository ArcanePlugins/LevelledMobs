package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * TODO Describe...
 *
 * @author lokka30
 */
public class EntityNametagListener implements Listener {

    private final LevelledMobs main;

    public EntityNametagListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onNametag(final PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof LivingEntity) {
            final LivingEntity livingEntity = (LivingEntity) event.getRightClicked();
            final Player player = event.getPlayer();

            // Must have name tag in main hand / off-hand
            if (!(player.getInventory().getItemInMainHand().getType() == Material.NAME_TAG || player.getInventory().getItemInOffHand().getType() == Material.NAME_TAG))
                return;

            // Must be a levelled mob
            if (!main.levelInterface.isLevelled(livingEntity)) return;

            main.levelManager.updateNametagWithDelay(livingEntity, livingEntity.getWorld().getPlayers(), 2L);
        }
    }
}
