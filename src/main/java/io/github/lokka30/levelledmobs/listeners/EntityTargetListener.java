package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * @author stumper66
 * @contributors lokka30
 */
public class EntityTargetListener implements Listener {

    private final LevelledMobs instance;

    public EntityTargetListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    /**
     * This event is listened to update the nametag of a mob when they start targeting a player.
     * Should provide another band-aid for packets not appearing sometimes for mob nametags.
     *
     * @param event EntityTargetEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTarget(final EntityTargetEvent event) {

        // Must target a player and must be a living entity
        if (!(event.getTarget() instanceof Player) || !(event.getEntity() instanceof LivingEntity)) return;

        final LivingEntity livingEntity = (LivingEntity) event.getEntity();

        // Must be a levelled entity
        if (!livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING))
            return;

        // Update the nametag.
        instance.levelManager.updateNametag(livingEntity, instance.levelManager.getNametag(livingEntity, false), livingEntity.getWorld().getPlayers());
    }
}
