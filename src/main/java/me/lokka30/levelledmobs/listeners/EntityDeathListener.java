package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.customdrops.CustomDropResult;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

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

        final LivingEntityWrapper lmEntity = new LivingEntityWrapper(event.getEntity(), main);

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
