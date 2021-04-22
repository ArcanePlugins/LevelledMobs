package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.customdrops.CustomDropResult;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * TODO Describe...
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
    public void onDeath(final EntityDeathEvent event) {
        if (bypassDrops.contains(event.getEntityType().toString()))
            return;

        final LivingEntity livingEntity = event.getEntity();

        if (main.levelInterface.isLevelled(livingEntity)) {

            // Set levelled item drops
            main.levelManager.setLevelledItemDrops(livingEntity, event.getDrops());

            // Set levelled exp drops
            if (event.getDroppedExp() > 0) {
                event.setDroppedExp(main.levelManager.getLevelledExpDrops(livingEntity, event.getDroppedExp()));
            }

            //Run commands
            main.levelManager.execCommands(livingEntity);

        } else if (main.settingsCfg.getBoolean("use-custom-item-drops-for-mobs")) {
            final List<ItemStack> drops = new ArrayList<>();
            final CustomDropResult result = main.customDropsHandler.getCustomItemDrops(livingEntity, -1, drops, false, false);
            if (result == CustomDropResult.HAS_OVERRIDE)
                main.levelManager.removeVanillaDrops(livingEntity, event.getDrops());

            event.getDrops().addAll(drops);
        }
    }
}
