package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * This class is used for debugging the plugin.
 * When an entity is punched, a player with permission
 * will receive a bunch of data about the mob.
 *
 * @author lokka30
 */
public class EntityDamageDebugListener implements Listener {

    private final LevelledMobs main;

    public EntityDamageDebugListener(final LevelledMobs main) {
        this.main = main;
    }

    private final List<UUID> delay = new LinkedList<>();

    //This class is used to debug levellable mobs. It simply displays their current attributes, current health and current level.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        // Make sure debug entity damage is enabled
        if (!main.settingsCfg.getBoolean("debug-entity-damage")) return;

        // Make sure the mob is a LivingEntity and the attacker is a Player
        if (!(event.getEntity() instanceof LivingEntity) || !(event.getDamager() instanceof Player)) return;
        final LivingEntity livingEntity = (LivingEntity) event.getEntity();
        final Player player = (Player) event.getDamager();

        // Make sure the mob is levelled
        if (!main.levelInterface.isLevelled(livingEntity)) return;

        // Make sure the player has debug perm
        if (!player.hasPermission("levelledmobs.debug")) return;

        // Don't spam the player's chat
        if (delay.contains(player.getUniqueId())) return;

        /* Now send them the debug message! :) */

        send(player, "&8&m+---+&r Debug information for &b" + livingEntity.getName() + "&r &8&m+---+&r");

        // Print non-attributes
        send(player, "&f&nGlobal Values:");
        send(player, "&8&m->&b Level: &7" + livingEntity.getPersistentDataContainer().get(main.levelManager.levelKey, PersistentDataType.INTEGER));
        send(player, "&8&m->&b Current Health: &7" + Utils.round(livingEntity.getHealth()));
        send(player, "&8&m->&b Nametag: &7" + livingEntity.getCustomName());

        // Print attributes
        player.sendMessage(" ");
        send(player, "&f&nAttribute Values:");
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance attributeInstance = livingEntity.getAttribute(attribute);
            if (attributeInstance == null) continue;
            send(player, "&8&m->&b Attribute." + attribute + ": &7" + Utils.round(attributeInstance.getBaseValue()));
        }

        // Print unique values (per-mob)
        player.sendMessage(" ");
        send(player, "&f&nUnique Values:");
        if (livingEntity instanceof Creeper) {
            final Creeper creeper = (Creeper) livingEntity;
            send(player, "&8&m->&b Creeper Blast Radius: &7" + creeper.getExplosionRadius());
        }

        player.sendMessage(" ");

        // Add them to a delay, and remove them after 2 seconds (40 ticks)
        delay.add(player.getUniqueId());
        new BukkitRunnable() {
            public void run() {
                delay.remove(player.getUniqueId());
            }
        }.runTaskLater(main, 40L);
    }

    private void send(Player player, String message) {
        player.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + "&7 " + message));
    }
}
