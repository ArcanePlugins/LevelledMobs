package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

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
        final LivingEntityWrapper lmEntity = new LivingEntityWrapper((LivingEntity) event.getEntity(), main);
        final Player player = (Player) event.getDamager();

        // Make sure the mob is levelled
        if (!lmEntity.isLevelled()) return;

        // Make sure the player has debug perm
        if (!player.hasPermission("levelledmobs.debug")) return;

        // Don't spam the player's chat
        if (delay.contains(player.getUniqueId())) return;

        /* Now send them the debug message! :) */

        send(player, "&8&m+---+&r Debug information for &b" + lmEntity.getTypeName() + "&r &8&m+---+&r");

        // Print non-attributes
        send(player, "&f&nGlobal Values:", false);
        send(player, "&8&m->&b Level: &7" + lmEntity.getMobLevel());
        send(player, "&8&m->&b Current Health: &7" + Utils.round(lmEntity.getLivingEntity().getHealth()), false);
        send(player, "&8&m->&b Nametag: &7" + lmEntity.getLivingEntity().getCustomName(), false);

        // Print attributes
        player.sendMessage(" ");
        send(player, "&f&nAttribute Values:", false);
        for (final Attribute attribute : Attribute.values()) {
            final AttributeInstance attributeInstance = lmEntity.getLivingEntity().getAttribute(attribute);
            if (attributeInstance == null) continue;
            final StringBuilder sb = new StringBuilder();
            sb.append("&8&m->&b ");
            sb.append(attribute.toString().replace("GENERIC_", ""));
            sb.append(": &7");
            sb.append(Utils.round(attributeInstance.getValue()));

            int count = 0;
            for (final AttributeModifier mod : attributeInstance.getModifiers()){
                if (count == 0) sb.append(" (");
                else sb.append(", ");
                if (mod.getOperation().equals(AttributeModifier.Operation.MULTIPLY_SCALAR_1))
                    sb.append("* ");
                else
                    sb.append("+ ");
                sb.append(Utils.round(mod.getAmount(), 5));

                count++;
            }
            if (count > 0) sb.append(")");

            send(player, sb.toString(), false);
        }

        if (lmEntity.getLivingEntity() instanceof Creeper) {
            // Print unique values (per-mob)
            player.sendMessage(" ");
            send(player, "&f&nUnique Values:", false);
            final Creeper creeper = (Creeper) lmEntity.getLivingEntity();
            send(player, "&8&m->&b Creeper Blast Radius: &7" + creeper.getExplosionRadius(), false);
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

    private void send(final Player player, final String message) {
        send(player, message, true);
    }

    private void send(final Player player, final String message, @NotNull final Boolean usePrefix) {
        if (usePrefix)
            player.sendMessage(MessageUtils.colorizeAll(main.configUtils.getPrefix() + "&7 " + message));
        else
            player.sendMessage(MessageUtils.colorizeAll(message));
    }
}
