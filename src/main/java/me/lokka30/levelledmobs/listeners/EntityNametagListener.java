/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.rules.MobCustomNameStatus;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens when a nametag is placed on an entity so LevelledMobs
 * can apply various rules around nametagged entities
 *
 * @author lokka30
 */
public class EntityNametagListener implements Listener {

    private final LevelledMobs main;

    public EntityNametagListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onNametag(@NotNull final PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof LivingEntity) {
            final Player player = event.getPlayer();

            // Must have name tag in main hand / off-hand
            if (!(player.getInventory().getItemInMainHand().getType() == Material.NAME_TAG || player.getInventory().getItemInOffHand().getType() == Material.NAME_TAG))
                return;

            final LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) event.getRightClicked(), main);

            // Must be a levelled mob
            if (!lmEntity.isLevelled()) return;

            if (main.rulesManager.getRule_MobCustomNameStatus(lmEntity) == MobCustomNameStatus.NOT_NAMETAGGED) {
                main.levelInterface.removeLevel(lmEntity);
                return;
            }

            main.levelManager.updateNametag(lmEntity);
        }
    }
}
