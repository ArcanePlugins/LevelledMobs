package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

public class LEntityDamage implements Listener {

    private LevelledMobs instance = LevelledMobs.getInstance();

    // When the mob is damaged, try to update their nametag.
    @EventHandler
    public void onDamage(final EntityDamageEvent e) {
        instance.levelManager.updateTag(e.getEntity());
    }

    // Check for levelled ranged damage.
    @EventHandler
    public void onRangedDamage(final EntityDamageByEntityEvent e) {
        if (!e.isCancelled() && e.getDamager() instanceof Projectile) {
            final Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof LivingEntity) {
                final LivingEntity livingEntity = (LivingEntity) projectile.getShooter();
                if (instance.levelManager.isLevellable(livingEntity)) {
                    if (livingEntity.getPersistentDataContainer().get(instance.key, PersistentDataType.INTEGER) == null) { //if the entity doesn't contain a level, skip this.
                        return;
                    }

                    Number level = livingEntity.getPersistentDataContainer().get(instance.key, PersistentDataType.INTEGER);
                    if (level != null) {
                        final double baseAttackDamage = e.getDamage();
                        final double defaultAttackDamageAddition = instance.settings.get("fine-tuning.default-attack-damage-increase", 1.0F);
                        final double attackDamageMultiplier = instance.settings.get("fine-tuning.multipliers.ranged-attack-damage", 1.1F);
                        final double newAttackDamage = baseAttackDamage + defaultAttackDamageAddition + (attackDamageMultiplier * level.intValue());

                        e.setDamage(newAttackDamage);
                    }
                }
            }
        }
    }
}
