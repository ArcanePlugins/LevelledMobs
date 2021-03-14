package me.lokka30.levelledmobs.listeners;

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

    private final LevelledMobs instance;

    public EntityDamageListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    // When the mob is damaged, update their nametag.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDamage(final EntityDamageEvent event) {
        final Entity entity = event.getEntity();

        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;

            //Make sure the mob is levelled
            if (!livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING))
                return;

            // Update their nametag with a 1 tick delay so that their health after the damage is shown
            instance.levelManager.updateNametagWithDelay(livingEntity, livingEntity.getWorld().getPlayers(), 1);
        }
    }

    // Check for levelled ranged damage.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onRangedDamage(final EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Projectile) {
            final Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof LivingEntity) {
                final LivingEntity livingEntity = (LivingEntity) projectile.getShooter();
                if (instance.levelManager.isLevellable(livingEntity)) {

                    //if the entity doesn't contain a level, skip this.
                    if (livingEntity.getPersistentDataContainer().get(instance.levelManager.isLevelledKey, PersistentDataType.STRING) == null) {
                        return;
                    }

                    //get their level
                    final int level = Objects.requireNonNull(livingEntity.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER));

                    //set their damage
                    e.setDamage(e.getDamage() + instance.mobDataManager.getAdditionsForLevel(livingEntity, Addition.CUSTOM_RANGED_ATTACK_DAMAGE, level));
                }
            }
        }
    }
}
