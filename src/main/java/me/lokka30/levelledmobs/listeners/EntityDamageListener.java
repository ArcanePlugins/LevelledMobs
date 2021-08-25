/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * @author lokka30
 * @since v4.0.0
 * TODO Edit Description Here
 */
public class EntityDamageListener implements Listener {

    private final LevelledMobs main;

    public EntityDamageListener(final LevelledMobs main) {
        this.main = main;
    }

    /*
    TODO
        lokka30: edit javadoc description
        lokka30: complete event handler
     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL) // TODO Adjust event priority (if required)
    public void onEntityDamage(final EntityDamageEvent event) {
        //TODO
    }

}
