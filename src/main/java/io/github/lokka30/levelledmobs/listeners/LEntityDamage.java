package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class LEntityDamage implements Listener {

    private LevelledMobs instance = LevelledMobs.getInstance();

    // When the mob is damaged, try to update their nametag.
    @EventHandler
    public void onDamage(final EntityDamageEvent e) {
        instance.levelManager.updateTag(e.getEntity());
    }
}
