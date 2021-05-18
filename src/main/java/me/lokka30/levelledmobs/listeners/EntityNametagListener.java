package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
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
            final Player player = event.getPlayer();

            // Must have name tag in main hand / off-hand
            if (!(player.getInventory().getItemInMainHand().getType() == Material.NAME_TAG || player.getInventory().getItemInOffHand().getType() == Material.NAME_TAG))
                return;

            final LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) event.getRightClicked(), main);

            // Must be a levelled mob
            if (!lmEntity.isLevelled()) return;

            main.levelManager.updateNametag(lmEntity);
        }
    }
}
