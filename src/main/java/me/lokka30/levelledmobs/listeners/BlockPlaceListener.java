package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

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
    private final LevelledMobs main;
    final public NamespacedKey keySpawner;
    final public NamespacedKey keySpawner_MinLevel;
    final public NamespacedKey keySpawner_MaxLevel;
    final public NamespacedKey keySpawner_CustomDropId;

    public BlockPlaceListener(final LevelledMobs main) {
        this.main = main;
        keySpawner = new NamespacedKey(main, "spawner");
        keySpawner_MinLevel = new NamespacedKey(main, "minlevel");
        keySpawner_MaxLevel = new NamespacedKey(main, "maxlevel");
        keySpawner_CustomDropId = new NamespacedKey(main, "customdropid");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlaceEvent(final BlockPlaceEvent event) {
        if (!event.getBlockPlaced().getType().equals(Material.SPAWNER)) return;
        if (!event.getItemInHand().getType().equals(Material.SPAWNER)) return;

        processMobSpawner(event.getItemInHand(), event.getBlockPlaced());
    }

    private void processMobSpawner(final ItemStack invItem, final Block blockPlaced){

        final ItemMeta meta = invItem.getItemMeta();
        if (meta == null) return;
        if (!meta.getPersistentDataContainer().has(keySpawner, PersistentDataType.INTEGER)) return;

        // transfer PDC items from inventory spawner to placed spawner

        final CreatureSpawner cs = (CreatureSpawner) blockPlaced.getState();
        final PersistentDataContainer targetPdc = cs.getPersistentDataContainer();
        final PersistentDataContainer sourcePdc = meta.getPersistentDataContainer();
        final List<NamespacedKey> keys = Arrays.asList(keySpawner_MinLevel, keySpawner_MaxLevel, keySpawner_CustomDropId);

        targetPdc.set(keySpawner, PersistentDataType.INTEGER, 1);
        for (int i = 0; i < keys.size(); i++){
            final NamespacedKey key = keys.get(i);
            if (i < 2){
                if (sourcePdc.has(key, PersistentDataType.INTEGER)){
                    final Integer valueInt = sourcePdc.get(key, PersistentDataType.INTEGER);
                    if (valueInt != null) targetPdc.set(key, PersistentDataType.INTEGER, valueInt);
                }
            } else if (sourcePdc.has(key, PersistentDataType.STRING)){
                final String valueStr = sourcePdc.get(key, PersistentDataType.STRING);
                if (valueStr != null) targetPdc.set(key, PersistentDataType.STRING, valueStr);
            }
        }

        cs.update();
    }
}
