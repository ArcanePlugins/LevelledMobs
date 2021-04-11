package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

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
    public void onRangedDamage(final EntityDamageByEntityEvent event) {
        processRangedDamage(event);
        processGuardianDamage(event);
    }

    private void processRangedDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)) return;
        final Projectile projectile = (Projectile) event.getDamager();

        if (projectile.getShooter() == null) return;
        if (!(projectile.getShooter() instanceof LivingEntity)) return;

        final LivingEntity shooter = (LivingEntity) projectile.getShooter();

        if (!shooter.isValid()) return;
        if (!main.levelInterface.isLevelled(shooter)) return;

        Utils.debugLog(main, "EntityDamageListener", "Range attack damage modified for " + shooter.getName() + ":");
        Utils.debugLog(main, "EntityDamageListener", "Previous rangedDamage: " + event.getDamage());
        final int level = main.levelInterface.getLevelOfMob(shooter);
        event.setDamage(event.getDamage() + main.mobDataManager.getAdditionsForLevel(shooter, Addition.CUSTOM_RANGED_ATTACK_DAMAGE, level));
        Utils.debugLog(main, "EntityDamageListener", "New rangedDamage: " + event.getDamage());
    }

    private void processGuardianDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Guardian)) return;
        final Guardian guardian = (Guardian) event.getDamager();

        if (!guardian.isValid()) return;
        if (!main.levelInterface.isLevelled(guardian)) return;

        Utils.debugLog(main, "EntityDamageListener", "Range attack damage modified for " + guardian.getName() + ":");
        Utils.debugLog(main, "EntityDamageListener", "Previous guardianDamage: " + event.getDamage());
        final int level = main.levelInterface.getLevelOfMob(guardian);
        event.setDamage(event.getDamage() + main.mobDataManager.getAdditionsForLevel(guardian, Addition.CUSTOM_RANGED_ATTACK_DAMAGE, level)); // use ranged attack damage value
        Utils.debugLog(main, "EntityDamageListener", "New guardianDamage: " + event.getDamage());
    }
}
