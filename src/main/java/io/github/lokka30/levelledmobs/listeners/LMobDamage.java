package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class LMobDamage implements Listener {

    private LevelledMobs instance = LevelledMobs.getInstance();

    // When the mob is damaged, try to update their nametag.
    @EventHandler
    public void onDamage(final EntityDamageEvent e) {
        updateTag(e.getEntity());
    }

    // Update their tag 10 ticks, or half a second, after the mob was damaged.
    // This makes the nametag show their current health, otherwise you hit a zombie and it shows its old health.
    private void updateTag(Entity ent){
        new BukkitRunnable() {
            public void run() {
                instance.updateTag(ent);
            }
        }.runTaskLater(instance, 10L);
    }
}
