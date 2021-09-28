/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for when an entity is damaged so LevelledMobs can apply
 * a multiplier to the damage amount
 *
 * @author lokka30
 * @since 2.4.0
 */
public class EntityDamageListener implements Listener {

    private final LevelledMobs main;

    public EntityDamageListener(final LevelledMobs main) {
        this.main = main;
    }

    // When the mob is damaged, update their nametag.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDamage(@NotNull final EntityDamageEvent event) {
        if (event.getFinalDamage() == 0.0) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (event.getFinalDamage() == 0.0) return;

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance((LivingEntity) event.getEntity(), main);

        //Make sure the mob is levelled
        if (!lmEntity.isLevelled()){
            // TODO: should the boolean on the line below be reversed?
            if (main.levelManager.entitySpawnListener.processMobSpawns) {
                lmEntity.free();
                return;
            }

            if (lmEntity.getMobLevel() < 0) lmEntity.reEvaluateLevel = true;
        }

        boolean resetNametagTimer = false;
        if (main.nametagTimerResetTime > 0L && event instanceof EntityDamageByEntityEvent){
            final EntityDamageByEntityEvent entityDamageByEntityEvent = (EntityDamageByEntityEvent) event;
            resetNametagTimer = entityDamageByEntityEvent.getDamager() instanceof Player;
        }

        // Update their nametag with a 1 tick delay so that their health after the damage is shown
        main.levelManager.updateNametag_WithDelay(lmEntity, resetNametagTimer);
        lmEntity.free();
    }

    // Check for levelled ranged damage.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDamageByEntityEvent(final @NotNull EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() == 0.0) return;

        processRangedDamage(event);
        processOtherRangedDamage(event);
    }

    private void processRangedDamage(@NotNull final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile)) return;
        final Projectile projectile = (Projectile) event.getDamager();

        if (projectile.getShooter() == null) return;
        if (!(projectile.getShooter() instanceof LivingEntity)) return;

        final LivingEntityWrapper shooter = LivingEntityWrapper.getInstance((LivingEntity) projectile.getShooter(), main);
        processRangedDamage2(shooter, event);

        shooter.free();
    }

    private void processRangedDamage2(@NotNull final LivingEntityWrapper shooter, @NotNull final EntityDamageByEntityEvent event) {
        if (!shooter.getLivingEntity().isValid()) return;
        if (!shooter.isLevelled()) {
            if (main.levelManager.entitySpawnListener.processMobSpawns)
                return;

            main._mobsQueueManager.addToQueue(new QueueItem(shooter, event));
        }

        Utils.debugLog(main, DebugType.RANGED_DAMAGE_MODIFICATION, "Range attack damage modified for &b" + shooter.getLivingEntity().getName() + "&7:");
        Utils.debugLog(main, DebugType.RANGED_DAMAGE_MODIFICATION, "Previous rangedDamage: &b" + event.getDamage());

        final double newDamage = event.getDamage() + main.mobDataManager.getAdditionsForLevel(shooter, Addition.CUSTOM_RANGED_ATTACK_DAMAGE, event.getDamage());
        event.setDamage(newDamage);
        Utils.debugLog(main, DebugType.RANGED_DAMAGE_MODIFICATION, "New rangedDamage: &b" + newDamage);
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

        Utils.debugLog(main, DebugType.RANGED_DAMAGE_MODIFICATION, "Range attack damage modified for &b" + livingEntity.getName() + "&7:");
        Utils.debugLog(main, DebugType.RANGED_DAMAGE_MODIFICATION, "Previous guardianDamage: &b" + event.getDamage());

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(livingEntity, main);
        event.setDamage(main.mobDataManager.getAdditionsForLevel(lmEntity, Addition.CUSTOM_RANGED_ATTACK_DAMAGE, event.getDamage())); // use ranged attack damage value
        Utils.debugLog(main, DebugType.RANGED_DAMAGE_MODIFICATION, "New guardianDamage: &b" + event.getDamage());
        lmEntity.free();
    }
}
