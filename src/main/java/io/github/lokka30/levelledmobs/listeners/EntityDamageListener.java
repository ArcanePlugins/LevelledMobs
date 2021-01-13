package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
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
    @EventHandler
    public void onDamage(final EntityDamageEvent event) {
        final Entity entity = event.getEntity();

        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;

            instance.levelManager.updateNametagWithDelay(livingEntity);
        }
    }

    // Check for levelled ranged damage.
    @EventHandler
    public void onRangedDamage(final EntityDamageByEntityEvent e) {
        if (!e.isCancelled() && e.getDamager() instanceof Projectile) {
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
