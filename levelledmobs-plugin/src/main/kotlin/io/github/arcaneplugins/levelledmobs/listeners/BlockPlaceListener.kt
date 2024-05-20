package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.CreatureSpawner
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * Listens for blocks being placed for the sole reason of transferring PDC data to a placed LM
 * spawner
 *
 * @author stumper66
 * @since 3.1.2
 */
class BlockPlaceListener : Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onBlockPlaceEvent(event: BlockPlaceEvent) {
        if (event.blockPlaced.type != Material.SPAWNER ||
            event.itemInHand.type != Material.SPAWNER
        ) {
            return
        }

        processMobSpawner(event.itemInHand, event.blockPlaced)
    }

    private fun processMobSpawner(invItem: ItemStack, blockPlaced: Block) {
        val meta = invItem.itemMeta ?: return
        if (!meta.persistentDataContainer
                .has(NamespacedKeys.keySpawner, PersistentDataType.INTEGER)
        ) {
            return
        }

        // transfer PDC items from inventory spawner to placed spawner
        val cs = blockPlaced.state as CreatureSpawner
        val targetPdc = cs.persistentDataContainer
        val sourcePdc = meta.persistentDataContainer
        val keys = listOf(
            NamespacedKeys.keySpawnerCustomDropId,        /* 0  */
            NamespacedKeys.keySpawnerSpawnType,           /* 1  */
            NamespacedKeys.keySpawnerCustomName,          /* 2  */
            NamespacedKeys.keySpawnerLore,                /* 3  */
            NamespacedKeys.keySpawnerMinLevel,            /* 4  */
            NamespacedKeys.keySpawnerMaxLevel,            /* 5  */
            NamespacedKeys.keySpawnerDelay,               /* 6  */
            NamespacedKeys.keySpawnerMaxNearbyEntities,   /* 7  */
            NamespacedKeys.keySpawnerMinSpawnDelay,       /* 8  */
            NamespacedKeys.keySpawnerMaxSpawnDelay,       /* 9  */
            NamespacedKeys.keySpawnerRequiredPlayerRange, /* 10 */
            NamespacedKeys.keySpawnerSpawnCount,          /* 11 */
            NamespacedKeys.keySpawnerSpawnRange           /* 12 */
        )

        targetPdc.set(NamespacedKeys.keySpawner, PersistentDataType.INTEGER, 1)
        for (i in keys.indices) {
            val key = keys[i]
            if (i <= 3) {
                if (sourcePdc.has(key, PersistentDataType.STRING)) {
                    val valueStr = sourcePdc.get(key, PersistentDataType.STRING)
                    if (valueStr != null) {
                        if (i == 1) {
                            val entityType = EntityType.valueOf(valueStr)
                            cs.spawnedType = entityType
                        } else {
                            targetPdc.set(key, PersistentDataType.STRING, valueStr)
                        }
                    }
                }
            } else if (sourcePdc.has(key, PersistentDataType.INTEGER)) {
                val valueInt = sourcePdc.get(key, PersistentDataType.INTEGER)
                if (i < 6 && valueInt != null) {
                    targetPdc.set(key, PersistentDataType.INTEGER, valueInt)
                } else if (valueInt != null) {
                    when (i) {
                        6 -> cs.delay = valueInt
                        7 -> cs.maxNearbyEntities = valueInt
                        8 -> {
                            if (cs.maxSpawnDelay < valueInt) {
                                cs.maxSpawnDelay = valueInt
                            }
                            cs.minSpawnDelay = valueInt
                        }

                        9 -> {
                            if (cs.minSpawnDelay > valueInt) {
                                cs.minSpawnDelay = valueInt
                            }
                            cs.maxSpawnDelay = valueInt
                        }

                        10 -> cs.requiredPlayerRange = valueInt
                        11 -> cs.spawnCount = valueInt
                        12 -> cs.spawnRange = valueInt
                    }
                }
            }
        }

        cs.update()
    }
}