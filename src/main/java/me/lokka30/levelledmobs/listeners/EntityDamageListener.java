package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

/**
 * TODO Describe...
 *
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
        if (!(event.getEntity() instanceof LivingEntity)) return;

        final LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) event.getEntity(), main);

        //Make sure the mob is levelled
        if (!lmEntity.isLevelled()) return;

        // Update their nametag with a 1 tick delay so that their health after the damage is shown
        main.levelManager.updateNametagWithDelay(lmEntity, 1);
    }

    // Check for levelled ranged damage.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onRangedDamage(final EntityDamageByEntityEvent event) {
        processRangedDamage(event);
        processOtherRangedDamage(event);
    }

    private void processRangedDamage(@NotNull final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)) return;
        final Projectile projectile = (Projectile) event.getDamager();

        if (projectile.getShooter() == null) return;
        if (!(projectile.getShooter() instanceof LivingEntity)) return;

        final LivingEntityWrapper shooter = new LivingEntityWrapper((LivingEntity) projectile.getShooter(), main);

        if (!shooter.getLivingEntity().isValid()) return;
        if (!shooter.isLevelled()) return;

        Utils.debugLog(main, DebugType.RANGED_DAMAGE_MODIFICATION, "Range attack damage modified for " + shooter.getLivingEntity().getName() + ":");
        Utils.debugLog(main, DebugType.RANGED_DAMAGE_MODIFICATION, "Previous rangedDamage: " + event.getDamage());
        //final int level = shooter.getMobLevel();
        event.setDamage(event.getDamage() + main.mobDataManager.getAdditionsForLevel(shooter, Addition.CUSTOM_RANGED_ATTACK_DAMAGE));
        Utils.debugLog(main, DebugType.RANGED_DAMAGE_MODIFICATION, "New rangedDamage: " + event.getDamage());
    }

    private void processOtherRangedDamage(@NotNull final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity)) return;
        final LivingEntity livingEntity = (LivingEntity) event.getDamager();

        if (
                !(livingEntity instanceof Guardian) &&
                !(livingEntity instanceof Ghast) &&
                !(livingEntity instanceof Wither)
            )
            return;

        if (!livingEntity.isValid()) return;
        if (!main.levelInterface.isLevelled(livingEntity)) return;

        Utils.debugLog(main, DebugType.RANGED_DAMAGE_MODIFICATION, "Range attack damage modified for " + livingEntity.getName() + ":");
        Utils.debugLog(main, DebugType.RANGED_DAMAGE_MODIFICATION, "Previous guardianDamage: " + event.getDamage());
        //final int level = main.levelInterface.getLevelOfMob(guardian);
        final LivingEntityWrapper lmEntity = new LivingEntityWrapper(livingEntity, main);
        event.setDamage(event.getDamage() + main.mobDataManager.getAdditionsForLevel(lmEntity, Addition.CUSTOM_RANGED_ATTACK_DAMAGE)); // use ranged attack damage value
        Utils.debugLog(main, DebugType.RANGED_DAMAGE_MODIFICATION, "New guardianDamage: " + event.getDamage());
    }
}
