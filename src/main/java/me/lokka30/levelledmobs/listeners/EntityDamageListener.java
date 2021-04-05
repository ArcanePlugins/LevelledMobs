package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelInterface;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Addition;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

/**
 * @author lokka30
 */
public class EntityDamageListener implements Listener {

    private final LevelledMobs main;

    public EntityDamageListener(final LevelledMobs main) {
        this.main = main;
    }

    // When the mob is damaged, update their nametag.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDamage(final EntityDamageEvent event) {
        final Entity entity = event.getEntity();

        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;

            //Make sure the mob is levelled
            if (!main.levelInterface.isLevelled(livingEntity)) return;

            // Update their nametag with a 1 tick delay so that their health after the damage is shown
            main.levelManager.updateNametagWithDelay(livingEntity, livingEntity.getWorld().getPlayers(), 1);
        }
    }

    // Check for levelled ranged damage.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onRangedDamage(final EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Projectile) {
            final Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof LivingEntity) {
                final LivingEntity livingEntity = (LivingEntity) projectile.getShooter();
                if (main.levelInterface.getLevellableState(livingEntity) == LevelInterface.LevellableState.ALLOWED) {

                    //if the entity doesn't contain a level, skip this.
                    if (!main.levelInterface.isLevelled(livingEntity)) return;

                    //get their level
                    final int level = Objects.requireNonNull(livingEntity.getPersistentDataContainer().get(main.levelManager.levelKey, PersistentDataType.INTEGER));

                    //set their damage
                    e.setDamage(e.getDamage() + main.mobDataManager.getAdditionsForLevel(livingEntity, Addition.CUSTOM_RANGED_ATTACK_DAMAGE, level));
                }
            }
        }
    }
}
