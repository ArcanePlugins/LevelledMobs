package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * @author lokka30
 */
public class EntityNametagListener implements Listener {

    private final LevelledMobs instance;

    public EntityNametagListener(final LevelledMobs instance) {
        this.instance = instance;
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
            if (!livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING))
                return;

            instance.levelManager.updateNametagWithDelay(livingEntity, livingEntity.getWorld().getPlayers(), 2L);
        }
    }
}
