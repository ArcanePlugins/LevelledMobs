package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;

/**
 * TODO Describe...
 *
 * @author stumper66
 */
public class EntityTargetListener implements Listener {

    private final LevelledMobs main;

    public EntityTargetListener(final LevelledMobs main) {
        this.main = main;
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

        final LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) event.getEntity(), main);

        // Must be a levelled entity
        if (!lmEntity.isLevelled()) return;

        // Update the nametag.
        main.levelManager.updateNametag(lmEntity, main.levelManager.getNametag(lmEntity, false), lmEntity.getLivingEntity().getWorld().getPlayers());
    }
}
