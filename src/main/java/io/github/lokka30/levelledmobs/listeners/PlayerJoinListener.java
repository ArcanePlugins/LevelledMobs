package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerJoinListener implements Listener {

    private final LevelledMobs instance;

    public PlayerJoinListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        // For each entity in each world, check if they are levellable, if so, send the client a nametag packet
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity) {
                    final LivingEntity livingEntity = (LivingEntity) entity;

                    // mob must be alive
                    if (livingEntity.isDead()) continue;

                    // mob must be levelled
                    if (!livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING)) {
                        continue;
                    }

                    final String nametag = instance.levelManager.getNametag(livingEntity);

                    //Send nametag packet
                    //This also must be delayed by 1 tick
                    new BukkitRunnable() {
                        public void run() {
                            // In case the entity died 1 tick later
                            if (livingEntity.isDead()) return;

                            instance.levelManager.updateNametag(livingEntity, nametag, player);
                        }
                    }.runTaskLater(instance, 1L);
                }
            }
        }
    }
}
