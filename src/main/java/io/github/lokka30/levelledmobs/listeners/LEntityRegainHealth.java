package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class LEntityRegainHealth implements Listener {

    // When the mob regains health, try to update their nametag.
    @EventHandler
    public void onEntityRegainHealth(final EntityRegainHealthEvent e) {
        LevelledMobs.getInstance().levelManager.updateTag(e.getEntity());
    }

}
