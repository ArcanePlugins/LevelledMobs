package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class LMobDeath implements Listener {

    private LevelledMobs instance = LevelledMobs.getInstance();

    @EventHandler
    public void onDeath(final EntityDeathEvent e) {
        instance.levelManager.checkClearNametag(e.getEntity());
        instance.levelManager.calculateDrops(e.getEntity(), e.getDrops());
    }
}
