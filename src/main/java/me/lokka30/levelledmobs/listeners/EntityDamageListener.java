/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.DebugManager;
import me.lokka30.levelledmobs.misc.Addition;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.QueueItem;
import me.lokka30.levelledmobs.rules.NametagVisibilityEnum;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for when an entity is damaged so LevelledMobs can apply a multiplier to the damage
 * amount
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
    public void onEntityDamageEvent(@NotNull final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        final boolean isCritical = event.getFinalDamage() == 0.0;

        if (event instanceof final EntityDamageByEntityEvent edee && isCritical &&
                edee.getDamager() instanceof Player player) {
            // this is so custom drops can associate the killer if the mob was
            // killed via a custom projectile such as magic
            main.entityDeathListener.damageMappings.put(edee.getEntity().getUniqueId(), player);
            return;
        }

        if (isCritical) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            if (!(event instanceof final EntityDamageByEntityEvent entityDamageByEntityEvent)) {
                return;
            }

            // if a mob hit a player then show the mob's nametag
            if (!(entityDamageByEntityEvent.getDamager() instanceof LivingEntity)
                || entityDamageByEntityEvent.getDamager() instanceof Player) {
                return;
            }

            final LivingEntityWrapper theHitter = LivingEntityWrapper.getInstance(
                (LivingEntity) entityDamageByEntityEvent.getDamager(), main);
            final List<NametagVisibilityEnum> nametagVisibilityEnums = main.rulesManager.getRuleCreatureNametagVisbility(
                theHitter);
            final long nametagVisibleTime = theHitter.getNametagCooldownTime();

            if (nametagVisibleTime > 0L &&
                nametagVisibilityEnums.contains(NametagVisibilityEnum.ATTACKED)) {
                if (theHitter.playersNeedingNametagCooldownUpdate == null) {
                    theHitter.playersNeedingNametagCooldownUpdate = new HashSet<>();
                }
                theHitter.playersNeedingNametagCooldownUpdate.add((Player) event.getEntity());
                main.levelManager.updateNametagWithDelay(theHitter);
            }
            theHitter.free();
            return;
        }

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(
            (LivingEntity) event.getEntity(), main);

        //Make sure the mob is levelled
        if (!lmEntity.isLevelled()) {
            // TODO: should the boolean on the line below be reversed?
            if (main.levelManager.entitySpawnListener.processMobSpawns) {
                lmEntity.free();
                return;
            }

            if (lmEntity.getMobLevel() < 0) {
                lmEntity.reEvaluateLevel = true;
            }
        }

        boolean wasDamagedByEntity = false;
        if (event instanceof final EntityDamageByEntityEvent entityDamageByEntityEvent){
            wasDamagedByEntity = true;
            if (entityDamageByEntityEvent.getDamager() instanceof Player player) {
                lmEntity.associatedPlayer = player;
            }
        }
        final List<NametagVisibilityEnum> nametagVisibilityEnums = main.rulesManager.getRuleCreatureNametagVisbility(
            lmEntity);
        final long nametagVisibleTime = lmEntity.getNametagCooldownTime();

        if (nametagVisibleTime > 0L && wasDamagedByEntity &&
            nametagVisibilityEnums.contains(NametagVisibilityEnum.ATTACKED)) {

            if (lmEntity.associatedPlayer != null) {
                if (lmEntity.playersNeedingNametagCooldownUpdate == null) {
                    lmEntity.playersNeedingNametagCooldownUpdate = new HashSet<>();
                }

                lmEntity.playersNeedingNametagCooldownUpdate.add(lmEntity.associatedPlayer);
            }
        }

        lmEntity.getPDC().set(main.namespacedKeys.lastDamageTime, PersistentDataType.LONG, Instant.now().toEpochMilli());

        // Update their nametag with a 1 tick delay so that their health after the damage is shown
        main.levelManager.updateNametagWithDelay(lmEntity);
        lmEntity.free();
    }

    // Check for levelled ranged damage.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDamageByEntityEvent(final @NotNull EntityDamageByEntityEvent event) {
        if (event.getFinalDamage() == 0.0) {
            return;
        }

        processRangedDamage(event);
        processOtherRangedDamage(event);
    }

    private void processRangedDamage(@NotNull final EntityDamageByEntityEvent event) {
        if (event.getDamager().getType() == EntityType.AREA_EFFECT_CLOUD) {
            // ender dragon breath
            final AreaEffectCloud aec = (AreaEffectCloud) event.getDamager();
            if (!(aec.getSource() instanceof EnderDragon)) {
                return;
            }
            final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(
                (LivingEntity) aec.getSource(), main);
            processRangedDamage2(lmEntity, event);
            lmEntity.free();
            return;
        }

        if (!(event.getDamager() instanceof final Projectile projectile)) {
            return;
        }

        if (projectile.getShooter() == null) {
            return;
        }

        if (projectile.getShooter() instanceof Player
            && event.getEntity() instanceof LivingEntity) {
            final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(
                (LivingEntity) event.getEntity(), main);
            if (lmEntity.isLevelled() && main.rulesManager.getRuleCreatureNametagVisbility(
                lmEntity).contains(NametagVisibilityEnum.ATTACKED)) {
                if (lmEntity.playersNeedingNametagCooldownUpdate == null) {
                    lmEntity.playersNeedingNametagCooldownUpdate = new HashSet<>();
                }
                lmEntity.playersNeedingNametagCooldownUpdate.add((Player) projectile.getShooter());
                main.levelManager.updateNametagWithDelay(lmEntity);
            }
            lmEntity.free();
            return;
        }

        if (!(projectile.getShooter() instanceof LivingEntity)) {
            return;
        }

        final LivingEntityWrapper shooter = LivingEntityWrapper.getInstance(
            (LivingEntity) projectile.getShooter(), main);
        processRangedDamage2(shooter, event);

        shooter.free();
    }

    private void processRangedDamage2(@NotNull final LivingEntityWrapper shooter,
        @NotNull final EntityDamageByEntityEvent event) {
        if (!shooter.getLivingEntity().isValid()) {
            return;
        }
        if (!shooter.isLevelled()) {
            if (main.levelManager.entitySpawnListener.processMobSpawns) {
                return;
            }

            main.mobsQueueManager.addToQueue(new QueueItem(shooter, event));
        }

        final float newDamage =
                (float) event.getDamage() + main.mobDataManager.getAdditionsForLevel(shooter,
                Addition.CUSTOM_RANGED_ATTACK_DAMAGE, (float) event.getDamage());
        DebugManager.log(DebugType.RANGED_DAMAGE_MODIFICATION, shooter, () -> String.format(
            "&7Source: &b%s&7 (lvl &b%s&7), damage: &b%s&7, new damage: &b%s&7",
            shooter.getNameIfBaby(), shooter.getMobLevel(), event.getDamage(), newDamage));
        event.setDamage(newDamage);
    }

    private void processOtherRangedDamage(@NotNull final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof final LivingEntity livingEntity)) {
            return;
        }

        if (
            !(livingEntity instanceof Guardian) &&
                !(livingEntity instanceof Ghast) &&
                !(livingEntity instanceof Wither)
        ) {
            return;
        }

        if (!livingEntity.isValid()) {
            return;
        }
        if (!main.levelInterface.isLevelled(livingEntity)) {
            return;
        }

        DebugManager.log(DebugType.RANGED_DAMAGE_MODIFICATION, livingEntity, () ->
                "Range attack damage modified for &b" + livingEntity.getName() + "&7:");
        DebugManager.log(DebugType.RANGED_DAMAGE_MODIFICATION, livingEntity, () ->
                "Previous guardianDamage: &b" + event.getDamage());

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(livingEntity, main);
        event.setDamage(
            main.mobDataManager.getAdditionsForLevel(lmEntity, Addition.CUSTOM_RANGED_ATTACK_DAMAGE,
                    (float) event.getDamage())); // use ranged attack damage value
        DebugManager.log(DebugType.RANGED_DAMAGE_MODIFICATION, livingEntity, () ->
                "New guardianDamage: &b" + event.getDamage());
        lmEntity.free();
    }
}
