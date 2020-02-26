package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelManager;
import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class LMobDeath implements Listener {

    private LevelledMobs instance = LevelledMobs.getInstance();

    @EventHandler
    public void onDeath(final EntityDeathEvent e) {
        final LevelManager levelManager = instance.levelManager;
        final LivingEntity entity = e.getEntity();

        levelManager.checkClearNametag(entity);
        levelManager.calculateDrops(entity, e.getDrops());
        e.setDroppedExp(levelManager.calculateXp(entity, e.getDroppedExp()));
    }
}
