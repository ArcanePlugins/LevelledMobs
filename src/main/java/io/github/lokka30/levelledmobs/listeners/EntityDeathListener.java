package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
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

    @EventHandler(ignoreCancelled = true)
    public void onDeath(final EntityDeathEvent event) {
        if (bypassDrops.contains(event.getEntityType().toString())) {
            return;
        }

        final LivingEntity livingEntity = event.getEntity();

        if (livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING)) {

            // Set levelled item drops
            final List<ItemStack> drops = event.getDrops();
            event.getDrops().addAll(instance.levelManager.getLevelledItemDrops(livingEntity, drops));

            // Set levelled exp drops
            if (event.getDroppedExp() > 0) {
                event.setDroppedExp(instance.levelManager.getLevelledExpDrops(livingEntity, event.getDroppedExp()));
            }
        }
        else if (instance.settingsCfg.getBoolean("use-custom-item-drops-for-mobs")){
            final List<ItemStack> newDrops = new ArrayList<>();
            instance.levelManager.getCustomItemDrops(livingEntity, -1, newDrops, false);
            if (!newDrops.isEmpty()){
                event.getDrops().addAll(newDrops);
            }
        }
    }
}
