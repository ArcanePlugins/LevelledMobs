/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for when an entity regains health so the nametag can be updated accordingly
 *
 * @author konsolas, lokka30
 * @since 2.4.0
 */
public class EntityRegainHealthListener implements Listener {

    private final LevelledMobs main;

    public EntityRegainHealthListener(final LevelledMobs main) {
        this.main = main;
    }

    // When the mob regains health, try to update their nametag.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityRegainHealth(@NotNull final EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        // Make sure the mob is levelled
        if (!main.levelManager.isLevelled((LivingEntity) event.getEntity())) {
            return;
        }

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(
            (LivingEntity) event.getEntity(), main);

        main.levelManager.updateNametagWithDelay(lmEntity);
        lmEntity.free();
    }

}
