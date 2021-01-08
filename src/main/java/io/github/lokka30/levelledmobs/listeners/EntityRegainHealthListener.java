package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class EntityRegainHealthListener implements Listener {

    private final LevelledMobs instance;

    public EntityRegainHealthListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    // When the mob regains health, try to update their nametag.
    @EventHandler
    public void onEntityRegainHealth(final EntityRegainHealthEvent e) {
        if (e.isCancelled() || !instance.settingsCfg.getBoolean("update-nametag-health")) {
            return;
        }

        instance.levelManager.updateTag(e.getEntity());
    }

}
