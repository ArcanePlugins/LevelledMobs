/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.inventory.EntityEquipment;

import java.util.Arrays;
import java.util.List;

/**
 * Listens for when an entity combusts for the purpose of increasing
 * sunlight damage if desired
 *
 * @author stumper66
 * @since 2.4.0
 */
public class CombustListener implements Listener {

    public CombustListener(final LevelledMobs main){
        this.main = main;
    }

    private final LevelledMobs main;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCombust(final EntityCombustEvent event) {
        if (event instanceof EntityCombustByBlockEvent || event instanceof EntityCombustByEntityEvent) return;

        if (event.getEntity().getWorld().getEnvironment().equals(World.Environment.NETHER) ||
                event.getEntity().getWorld().getEnvironment().equals(World.Environment.THE_END))
            return;

        final List<EntityType> entityTypesCanBurnInSunlight2 = Arrays.asList(
                EntityType.SKELETON, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.STRAY, EntityType.DROWNED, EntityType.PHANTOM
        );
        if (!entityTypesCanBurnInSunlight2.contains(event.getEntity().getType()))
            return;

        if (event.getEntity() instanceof LivingEntity) {
            final EntityEquipment equipment = ((LivingEntity) event.getEntity()).getEquipment();

            if (equipment != null && equipment.getHelmet() != null && equipment.getHelmet().getType() != Material.AIR)
                return;
        }

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance((LivingEntity) event.getEntity(), main);
        double multiplier = main.rulesManager.getRule_SunlightBurnIntensity(lmEntity);
        if (multiplier == 0.0) {
            lmEntity.free();
            return;
        }

        double newHealth = lmEntity.getLivingEntity().getHealth() - multiplier;
        if (newHealth < 0.0) newHealth = 0.0;

        if (lmEntity.getLivingEntity().getHealth() <= 0.0) {
            lmEntity.free();
            return;
        }
        lmEntity.getLivingEntity().setHealth(newHealth);

        if (lmEntity.isLevelled())
            main.levelManager.updateNametag(lmEntity);

        lmEntity.free();
    }
}
