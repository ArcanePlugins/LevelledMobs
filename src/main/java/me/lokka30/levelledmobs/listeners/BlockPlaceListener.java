/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Listens for blocks being placed
 * for the sole reason of transferring
 * PDC data to a placed LM spawner
 *
 * @author stumper66
 */
public class BlockPlaceListener implements Listener {
    final public NamespacedKey keySpawner;
    final public NamespacedKey keySpawner_MinLevel;
    final public NamespacedKey keySpawner_MaxLevel;
    final public NamespacedKey keySpawner_CustomDropId;
    final public NamespacedKey keySpawner_Delay;
    final public NamespacedKey keySpawner_MaxNearbyentities;
    final public NamespacedKey keySpawner_MinSpawnDelay;
    final public NamespacedKey keySpawner_MaxSpawnDelay;
    final public NamespacedKey keySpawner_RequiredPlayerRange;
    final public NamespacedKey keySpawner_SpawnCount;
    final public NamespacedKey keySpawner_SpawnType;
    final public NamespacedKey keySpawner_SpawnRange;

    public BlockPlaceListener(final LevelledMobs main) {
        keySpawner = new NamespacedKey(main, "spawner");
        keySpawner_MinLevel = new NamespacedKey(main, "minlevel");
        keySpawner_MaxLevel = new NamespacedKey(main, "maxlevel");
        keySpawner_CustomDropId = new NamespacedKey(main, "customdropid");
        keySpawner_Delay = new NamespacedKey(main, "delay");
        keySpawner_MaxNearbyentities = new NamespacedKey(main, "maxnearbyentities");
        keySpawner_MinSpawnDelay = new NamespacedKey(main, "minspawndelay");
        keySpawner_MaxSpawnDelay = new NamespacedKey(main, "maxspawndelay");
        keySpawner_RequiredPlayerRange = new NamespacedKey(main, "requiredplayerrange");
        keySpawner_SpawnCount = new NamespacedKey(main, "spawncount");
        keySpawner_SpawnType = new NamespacedKey(main, "spawntype");
        keySpawner_SpawnRange = new NamespacedKey(main, "spawnrangee");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlaceEvent(@NotNull final BlockPlaceEvent event) {
        if (!event.getBlockPlaced().getType().equals(Material.SPAWNER) ||
            !event.getItemInHand().getType().equals(Material.SPAWNER))
            return;

        processMobSpawner(event.getItemInHand(), event.getBlockPlaced());
    }

    private void processMobSpawner(@NotNull final ItemStack invItem, final Block blockPlaced){

        final ItemMeta meta = invItem.getItemMeta();
        if (meta == null) return;
        if (!meta.getPersistentDataContainer().has(keySpawner, PersistentDataType.INTEGER)) return;

        // transfer PDC items from inventory spawner to placed spawner

        final CreatureSpawner cs = (CreatureSpawner) blockPlaced.getState();
        final PersistentDataContainer targetPdc = cs.getPersistentDataContainer();
        final PersistentDataContainer sourcePdc = meta.getPersistentDataContainer();
        final List<NamespacedKey> keys = Arrays.asList(
                keySpawner_CustomDropId,
                keySpawner_SpawnType,
                keySpawner_MinLevel,
                keySpawner_MaxLevel,
                keySpawner_Delay,
                keySpawner_MaxNearbyentities,
                keySpawner_MinSpawnDelay,
                keySpawner_MaxSpawnDelay,
                keySpawner_RequiredPlayerRange,
                keySpawner_SpawnCount,
                keySpawner_SpawnRange
        );

        targetPdc.set(keySpawner, PersistentDataType.INTEGER, 1);
        for (int i = 0; i < keys.size(); i++){
            final NamespacedKey key = keys.get(i);
            if (i <= 1){
                if (sourcePdc.has(key, PersistentDataType.STRING)) {
                    final String valueStr = sourcePdc.get(key, PersistentDataType.STRING);
                    if (valueStr != null) {
                        if (i == 0)
                            targetPdc.set(key, PersistentDataType.STRING, valueStr);
                        else {
                            final EntityType entityType = EntityType.valueOf(valueStr);
                            cs.setSpawnedType(entityType);
                        }
                    }
                }
            } else if (sourcePdc.has(key, PersistentDataType.INTEGER)){
                final Integer valueInt = sourcePdc.get(key, PersistentDataType.INTEGER);
                if (i < 4 && valueInt != null)
                    targetPdc.set(key, PersistentDataType.INTEGER, valueInt);
                else if (valueInt != null) {
                    switch (i){
                        case 4: cs.setDelay(valueInt); break;
                        case 5: cs.setMaxNearbyEntities(valueInt); break;
                        case 6: cs.setMinSpawnDelay(valueInt); break;
                        case 7: cs.setMaxSpawnDelay(valueInt); break;
                        case 8: cs.setRequiredPlayerRange(valueInt); break;
                        case 9: cs.setSpawnCount(valueInt); break;
                        case 10: cs.setSpawnRange(valueInt); break;
                    }
                }
            }
        }

        cs.update();
    }
}
