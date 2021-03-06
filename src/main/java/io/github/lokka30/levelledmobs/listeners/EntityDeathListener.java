package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.customdrops.CustomDropResult;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class EntityDeathListener implements Listener {

    private final LevelledMobs instance;

    public EntityDeathListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    // These entities will be forced not to have levelled drops
    final HashSet<String> bypassDrops = new HashSet<>(Arrays.asList("ARMOR_STAND", "ITEM_FRAME", "DROPPED_ITEM", "PAINTING"));

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onDeath(final EntityDeathEvent event) {
        if (bypassDrops.contains(event.getEntityType().toString())) {
            return;
        }

        final LivingEntity livingEntity = event.getEntity();

        if (livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING)) {

            // Set levelled item drops
            instance.levelManager.getLevelledItemDrops(livingEntity, event.getDrops());

            // Set levelled exp drops
            if (event.getDroppedExp() > 0) {
                event.setDroppedExp(instance.levelManager.getLevelledExpDrops(livingEntity, event.getDroppedExp()));
            }
        }
        else if (instance.settingsCfg.getBoolean("use-custom-item-drops-for-mobs")){
            final List<ItemStack> drops = new ArrayList<>();
            final CustomDropResult result = instance.customDropsHandler.getCustomItemDrops(livingEntity, -1, drops, false, false);
            if (result == CustomDropResult.HAS_OVERRIDE){
                event.getDrops().clear();
                event.getDrops().addAll(drops);
            }
            else if (!drops.isEmpty())
                event.getDrops().addAll(drops);
        }
    }
}
