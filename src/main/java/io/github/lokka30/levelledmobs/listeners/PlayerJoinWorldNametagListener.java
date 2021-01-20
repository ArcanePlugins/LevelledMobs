package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;

public class PlayerJoinWorldNametagListener implements Listener {

    private final LevelledMobs instance;

    public PlayerJoinWorldNametagListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    private void updateNametagsInWorld(final Player player, final World world) {
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
                instance.levelManager.updateNametagWithDelays(livingEntity, nametag, Collections.singletonList(player));
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        updateNametagsInWorld(event.getPlayer(), event.getPlayer().getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChangeWorld(final PlayerChangedWorldEvent event) {
        updateNametagsInWorld(event.getPlayer(), event.getPlayer().getWorld());
    }
}
