package io.github.lokka30.levelledmobs.listeners;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
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
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        if (instance.settingsCfg.getBoolean("debug-entity-damage") && e.getEntity() instanceof LivingEntity && e.getDamager() instanceof Player) {
            final Player player = (Player) e.getDamager();
            final UUID uuid = player.getUniqueId();
            final LivingEntity livingEntity = (LivingEntity) e.getEntity();

            if (player.isOp() && !delay.contains(uuid) && livingEntity.getPersistentDataContainer().has(instance.levelManager.isLevelledKey, PersistentDataType.STRING)) {
                player.sendMessage(MessageUtils.colorizeAll("&b&lLevelledMobs: &7Debug information for &b" + livingEntity.getType().toString() + "&7: "));

                writeDebugForAttribute(player, livingEntity, Attribute.GENERIC_MAX_HEALTH);
                writeDebugForAttribute(player, livingEntity, Attribute.GENERIC_MOVEMENT_SPEED);
                if (livingEntity instanceof Creeper) {
                    final Creeper creeper = (Creeper) livingEntity;
                    writeDebugForAttribute(player, "BLAST_RADIUS", creeper.getExplosionRadius(), 3);
                }
                writeDebugForAttribute(player, livingEntity, Attribute.GENERIC_ATTACK_DAMAGE);

                writeDebugForAttribute(player, "Current Health", Utils.round(livingEntity.getHealth()));
                Object levelTemp = (livingEntity.getPersistentDataContainer().get(instance.levelManager.levelKey, PersistentDataType.INTEGER));

                writeDebugForAttribute(player, "Level", levelTemp == null ? 0 : (int) levelTemp);
                player.sendMessage(MessageUtils.colorizeAll("&8 - &fCustomName &8= &b" + (livingEntity.getCustomName() == null ? "N/A" : livingEntity.getCustomName())));

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

        p.sendMessage(MessageUtils.colorizeAll(String.format(
                "&8 - &f%s &7(DefaultValue) &8= &b%s", attributeName, Utils.round(defaultAmount))));
    }

    private void writeDebugForAttribute(final Player p, final String attributeName, final double amount) {
        p.sendMessage(MessageUtils.colorizeAll(String.format(
                "&8 - &f%s &8= &b%s", attributeName, Utils.round(amount))));
    }

    private void writeDebugForAttribute(final Player p, final LivingEntity ent, final Attribute att) {
        AttributeInstance attInstance = ent.getAttribute(att);
        if (attInstance == null) return;

        String attName = att.name();

        p.sendMessage(MessageUtils.colorizeAll(String.format(
                "&8 - &f%s &7(CurrentValue) &8= &b%s", attName, Utils.round(attInstance.getBaseValue()))));
        p.sendMessage(MessageUtils.colorizeAll(String.format(
                "&8 - &f%s &7(DefaultValue) &8= &b%s",
                attName, Utils.round((double) instance.mobDataManager.getAttributeDefaultValue(ent.getType(), att)))));
    }
}
