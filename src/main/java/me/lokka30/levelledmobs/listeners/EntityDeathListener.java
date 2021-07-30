/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.customdrops.CustomDropResult;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Listens for when an entity dies so it's drops can be multiplied, manipulated, etc
 *
 * @author lokka30
 */
public class EntityDeathListener implements Listener {

    private final LevelledMobs main;

    public EntityDeathListener(final LevelledMobs main) {
        this.main = main;
    }

    // These entities will be forced not to have levelled drops
    final HashSet<String> bypassDrops = new HashSet<>(Arrays.asList("ARMOR_STAND", "ITEM_FRAME", "DROPPED_ITEM", "PAINTING"));

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onDeath(@NotNull final EntityDeathEvent event) {
        if (bypassDrops.contains(event.getEntityType().toString()))
            return;

        final LivingEntityWrapper lmEntity = new LivingEntityWrapper(event.getEntity(), main);
        final EntityDamageEvent damage = lmEntity.getLivingEntity().getLastDamageCause();
        if (damage != null)
            lmEntity.deathCause = damage.getCause();

        if (lmEntity.getLivingEntity().getKiller() != null && main.papiManager != null)
            main.papiManager.putEntityDeath(lmEntity.getLivingEntity().getKiller(), lmEntity);

        if (lmEntity.isLevelled()) {

            // Set levelled item drops
            main.levelManager.setLevelledItemDrops(lmEntity, event.getDrops());

            // Set levelled exp drops
            if (event.getDroppedExp() > 0) {
                event.setDroppedExp(main.levelManager.getLevelledExpDrops(lmEntity, event.getDroppedExp()));
            }
        } else if (main.rulesManager.getRule_UseCustomDropsForMob(lmEntity).useDrops) {
            final List<ItemStack> drops = new LinkedList<>();
            final CustomDropResult result = main.customDropsHandler.getCustomItemDrops(lmEntity, drops, false);
            if (result == CustomDropResult.HAS_OVERRIDE)
                main.levelManager.removeVanillaDrops(lmEntity, event.getDrops());

            event.getDrops().addAll(drops);
        }
    }
}
