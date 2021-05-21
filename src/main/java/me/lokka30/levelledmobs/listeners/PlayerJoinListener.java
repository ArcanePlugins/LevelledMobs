package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.List;

/**
 * TODO Describe...
 *
 * @author lokka30
 */
public class PlayerJoinListener implements Listener {

    private final LevelledMobs main;

    public PlayerJoinListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        parseCompatibilityChecker(event.getPlayer());
        parseUpdateChecker(event.getPlayer());

        updateNametagsInWorldAsync(event.getPlayer(), event.getPlayer().getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChangeWorld(final PlayerChangedWorldEvent event) {
        updateNametagsInWorldAsync(event.getPlayer(), event.getPlayer().getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(final PlayerTeleportEvent event) {
        if (event.getTo() != null && event.getTo().getWorld() != null)
            updateNametagsInWorldAsync(event.getPlayer(), event.getTo().getWorld());
    }

    private void updateNametagsInWorldAsync(final Player player, final World world) {
        final BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                updateNametagsInWorld(player, world);
            }
        };

        runnable.runTaskAsynchronously(main);
    }

    private void updateNametagsInWorld(final Player player, final World world) {
        for (final Entity entity : world.getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;

            final LivingEntity livingEntity = (LivingEntity) entity;

            // mob must be alive
            if (!livingEntity.isValid()) continue;

            // mob must be levelled
            final LivingEntityWrapper lmEntity = new LivingEntityWrapper(livingEntity, main);
            if (!lmEntity.isLevelled()) continue;

            // public void updateNametagWithDelay(final LivingEntityWrapper lmEntity, final List<Player> playerList, final long delay) {
            main.levelManager.updateNametag(lmEntity, main.levelManager.getNametag(lmEntity, false), Collections.singletonList(player));
        }
    }

    void parseCompatibilityChecker(Player player) {
        // Player must have permission
        if (!player.hasPermission("levelledmobs.compatibility-notice")) return;

        // There must be possible incompatibilities
        if (main.incompatibilitiesAmount == 0) return;

        // Must be enabled in messages cfg
        if (!main.messagesCfg.getBoolean("other.compatibility-notice.enabled")) return;

        List<String> messages = main.messagesCfg.getStringList("other.compatibility-notice.messages");
        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%incompatibilities%", main.incompatibilitiesAmount + "");
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(player::sendMessage);
    }

    void parseUpdateChecker(Player player) {
        if (main.messagesCfg.getBoolean("other.update-notice.send-on-join", true)) {
            if (player.hasPermission("levelledmobs.receive-update-notifications")) {
                main.companion.updateResult.forEach(player::sendMessage);
            }
        }
    }
}
