package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

public class EntityDamageListener implements Listener {

    private LevelledMobs instance;

    public EntityDamageListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    // When the mob is damaged, try to update their nametag.
    @EventHandler
    public void onDamage(final EntityDamageEvent e) {
    	if (instance.fileCache.SETTINGS_UPDATE_NAMETAG_HEALTH) {
            instance.levelManager.updateTag(e.getEntity());
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
                    if (livingEntity.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER) == null) { //if the entity doesn't contain a level, skip this.
                        return;
                    }

                    Number level = livingEntity.getPersistentDataContainer().get(instance.levelKey, PersistentDataType.INTEGER);
                    if (level != null) {
                        final double baseAttackDamage = e.getDamage();
                        final double defaultAttackDamageAddition = instance.fileCache.SETTINGS_FINE_TUNING_DEFAULT_ATTACK_DAMAGE_INCREASE;
                        final double attackDamageMultiplier = instance.fileCache.SETTINGS_FINE_TUNING_MULTIPLIERS_RANGED_ATTACK_DAMAGE;
                        final double newAttackDamage = baseAttackDamage + defaultAttackDamageAddition + (attackDamageMultiplier * level.intValue());

                        e.setDamage(newAttackDamage);
                    }
                }
            }
        }
    }
}
