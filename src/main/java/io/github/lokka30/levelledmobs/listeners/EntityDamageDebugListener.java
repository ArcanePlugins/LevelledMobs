package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.Utils;
import me.lokka30.microlib.MicroUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * This class is used for debugging the plugin.
 */
public class EntityDamageDebugListener implements Listener {

    final List<UUID> delay = new ArrayList<>();
    private final LevelledMobs instance;

    public EntityDamageDebugListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    //This class is used to debug levellable mobs. It simply displays their current attributes, current health and current level.
    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        if (!e.isCancelled() && instance.settingsCfg.getBoolean("debug-entity-damage") && e.getEntity() instanceof LivingEntity && e.getDamager() instanceof Player) {
            final Player p = (Player) e.getDamager();
            final UUID uuid = p.getUniqueId();
            final LivingEntity ent = (LivingEntity) e.getEntity();

            if (p.isOp() && !delay.contains(uuid) && instance.levelManager.isLevellable(ent)) {
                p.sendMessage(MicroUtils.colorize("&b&lLevelledMobs: &7Debug information for &b" + ent.getType().toString() + "&7: "));

                writeDebugForAttribute(p, ent, Attribute.GENERIC_MAX_HEALTH);
                writeDebugForAttribute(p, ent, Attribute.GENERIC_MOVEMENT_SPEED);
                if (ent instanceof Creeper) {
                    final Creeper creeper = (Creeper) ent;
                    writeDebugForAttribute(p, "BLAST_RADIUS", creeper.getExplosionRadius(), 3);
                }
                writeDebugForAttribute(p, ent, Attribute.GENERIC_ATTACK_DAMAGE);

                writeDebugForAttribute(p, "Current Health", Utils.round(ent.getHealth()));
                writeDebugForAttribute(p, "Level", Objects.requireNonNull(ent.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER), "Level was null"));
                p.sendMessage(MicroUtils.colorize("&8 - &fCustomName &8= &b" + (ent.getCustomName() == null ? "N/A" : ent.getCustomName())));

                delay.add(uuid);
                new BukkitRunnable() {
                    public void run() {
                        delay.remove(uuid);
                    }
                }.runTaskLater(instance, 40L);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void writeDebugForAttribute(final Player p, final String attributeName, final double amount, final double defaultAmount) {

        writeDebugForAttribute(p, attributeName, amount);

        p.sendMessage(MicroUtils.colorize(String.format(
                "&8 - &f%s &7(DefaultValue) &8= &b%s", attributeName, Math.round(defaultAmount * 100.0) / 100.0)));
    }

    private void writeDebugForAttribute(final Player p, final String attributeName, final double amount) {
        p.sendMessage(MicroUtils.colorize(String.format(
                "&8 - &f%s &8= &b%s", attributeName, Math.round(amount * 100.0) / 100.0)));
    }

    private void writeDebugForAttribute(final Player p, final LivingEntity ent, final Attribute att) {
        AttributeInstance attInstance = ent.getAttribute(att);
        if (attInstance == null) return;

        String attName = att.name();

        p.sendMessage(MicroUtils.colorize(String.format(
                "&8 - &f%s &7(CurrentValue) &8= &b%s", attName, Math.round(attInstance.getBaseValue() * 100.0) / 100.0)));
        p.sendMessage(MicroUtils.colorize(String.format(
                "&8 - &f%s &7(DefaultValue) &8= &b%s", attName, instance.attributeManager.getDefaultValue(ent.getType(), att))));
    }
}
