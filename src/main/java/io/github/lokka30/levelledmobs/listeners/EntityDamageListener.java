package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

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

            // we only need to update the tag if they are using health placeholders.  This is not by default
            if (!instance.configUtils.nametagContainsHealth()) return;

            //Make sure the mob is levelled
            if (!livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING))
                return;

            // Update their nametag with a 1 tick delay so that their health after the damage is shown
            instance.levelManager.updateNametagWithDelay(livingEntity, livingEntity.getWorld().getPlayers(), 1);
        }
    }

    // Check for levelled ranged damage.
    @EventHandler(ignoreCancelled = true)
    public void onRangedDamage(final EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Projectile) {
            final Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof LivingEntity) {
                final LivingEntity livingEntity = (LivingEntity) projectile.getShooter();
                if (instance.levelManager.isLevellable(livingEntity)) {
                    if (livingEntity.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER) == null) { //if the entity doesn't contain a level, skip this.
                        return;
                    }

                    Number level = livingEntity.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER);
                    if (level != null) {
                        final double baseAttackDamage = e.getDamage();
                        final double attackDamageMultiplier = instance.settingsCfg.getDouble("fine-tuning.additions.custom.ranged-attack-damage");
                        final double newAttackDamage = baseAttackDamage + (attackDamageMultiplier * level.intValue());

                        e.setDamage(newAttackDamage);
                    }
                }
            }
        }
    }
}
