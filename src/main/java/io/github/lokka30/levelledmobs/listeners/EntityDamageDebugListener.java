package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.Utils;
import me.lokka30.microlib.MicroUtils;
import org.bukkit.Sound;
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

public class EntityDamageDebugListener implements Listener {

    final List<UUID> delay = new ArrayList<>();
    private final LevelledMobs instance;

    public EntityDamageDebugListener(final LevelledMobs instance) {
        this.instance = instance;
    }

    //This class is used to debug levellable mobs. It simply displays their current attributes, current health and current level.
    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        if (!e.isCancelled() && e.getEntity() instanceof LivingEntity && e.getDamager() instanceof Player && instance.settingsCfg.getBoolean("debug")) {
            final Player p = (Player) e.getDamager();
            final UUID uuid = p.getUniqueId();
            final LivingEntity ent = (LivingEntity) e.getEntity();

            if (p.isOp() && !delay.contains(uuid) && instance.levelManager.isLevellable(ent)) {
                p.sendMessage(MicroUtils.colorize("&a&lLevelledMobs: &7Debug information for &a" + ent.getType().toString() + "&7: "));

                writeDebugForAttribute(p, ent, Attribute.GENERIC_MAX_HEALTH);
                writeDebugForAttribute(p, ent, Attribute.GENERIC_MOVEMENT_SPEED);
                writeDebugForAttribute(p, ent, Attribute.GENERIC_MAX_HEALTH);
                if (ent instanceof Creeper) {
                    final Creeper creeper = (Creeper) ent;
                    writeDebugForAttribute(p, "BLAST_RADIUS", creeper.getExplosionRadius(), 3);
                }
                writeDebugForAttribute(p, ent, Attribute.GENERIC_ATTACK_DAMAGE);
                writeDebugForAttribute(p, ent, Attribute.GENERIC_FLYING_SPEED);

                writeDebugForAttribute(p, "Current Health", Utils.round(ent.getHealth(), 1));
                writeDebugForAttribute(p, "Level", Objects.requireNonNull(ent.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER), "Level was null"));
                writeDebugForAttribute(p, "isLevelled", Objects.requireNonNull(ent.getPersistentDataContainer().get(instance.levelManager.isLevelledKey, PersistentDataType.STRING), "isLevelled was null"));
                writeDebugForAttribute(p, "CustomName", ent.getCustomName() != null ? ent.getCustomName() : "(null)");

                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);

                delay.add(uuid);
                new BukkitRunnable() {
                    public void run() {
                        delay.remove(uuid);
                        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PUT, 1.0F, 1.0F);
                    }
                }.runTaskLater(instance, 40L);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void writeDebugForAttribute(final Player p, final String attributeName, final double amount, final double defaultAmount) {

        writeDebugForAttribute(p, attributeName, amount);

        p.sendMessage(MicroUtils.colorize(String.format(
                "&8 - &f%s Default = &a%s", attributeName, Math.round(defaultAmount * 100.0) / 100.0)));
    }

    private void writeDebugForAttribute(final Player p, final String attributeName, final double amount) {
        p.sendMessage(MicroUtils.colorize(String.format(
                "&8 - &f%s = &a%s", attributeName, Math.round(amount * 100.0) / 100.0)));
    }
    
    private void writeDebugForAttribute(final Player p, final String attributeName, final String msg) {
        p.sendMessage(MicroUtils.colorize(String.format(
                "&8 - &f%s = &a%s", attributeName, msg)));
    }
    
    private void writeDebugForAttribute(final Player p, final LivingEntity ent, final Attribute att) {
        AttributeInstance attInstance = ent.getAttribute(att);
        if (attInstance == null) return;

        String attName = att.name();

        p.sendMessage(MicroUtils.colorize(String.format(
                "&8 - &f%s = &a%s", attName, Math.round(attInstance.getBaseValue() * 100.0) / 100.0)));
        p.sendMessage(MicroUtils.colorize(String.format(
                "&8 - &f%s Default = &a%s", attName, Math.round(attInstance.getDefaultValue() * 100.0 / 100.0))));
    }
}
