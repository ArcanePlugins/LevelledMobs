package io.github.lokka30.levelledmobs.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeath implements Listener {

    //removes their custom name tag upon dying
    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity().getCustomName() != null)
            e.getEntity().setCustomName(null);
    }

}
