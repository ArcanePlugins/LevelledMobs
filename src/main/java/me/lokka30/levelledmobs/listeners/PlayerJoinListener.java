/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.NametagTimerChecker;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Listens for when a player joins, leaves or changes worlds so that
 * send messages as needed, update nametags or track the player
 *
 * @author lokka30, stumper66
 * @since 2.4.0
 */
public class PlayerJoinListener implements Listener {

    private final LevelledMobs main;

    public PlayerJoinListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull final PlayerJoinEvent event) {
        synchronized (NametagTimerChecker.nametagTimer_Lock){
            main.nametagTimerChecker.nametagCooldownQueue.put(event.getPlayer(), new WeakHashMap<>());
        }
        parseCompatibilityChecker(event.getPlayer());
        parseUpdateChecker(event.getPlayer());

        updateNametagsInWorldAsync(event.getPlayer(), event.getPlayer().getWorld().getEntities());

        if (main.migratedFromPre30 && event.getPlayer().isOp()){
            event.getPlayer().sendMessage(MessageUtils.colorizeStandardCodes("&b&lLevelledMobs: &cWARNING &7You have migrated from an older version.  All settings have been reverted.  Please edit rules.yml"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuitEvent(final PlayerQuitEvent event){
        if (main.placeholderApiIntegration != null)
            main.placeholderApiIntegration.playedLoggedOut(event.getPlayer());

        main.companion.spawner_CopyIds.remove(event.getPlayer().getUniqueId());
        main.companion.spawner_InfoIds.remove(event.getPlayer().getUniqueId());
        synchronized (NametagTimerChecker.nametagTimer_Lock) {
            main.nametagTimerChecker.nametagCooldownQueue.remove(event.getPlayer());
        }
        if (main.placeholderApiIntegration != null)
            main.placeholderApiIntegration.removePlayer(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChangeWorld(@NotNull final PlayerChangedWorldEvent event) {
        updateNametagsInWorldAsync(event.getPlayer(), event.getPlayer().getWorld().getEntities());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onTeleport(@NotNull final PlayerTeleportEvent event) {
        if (event.getTo() != null && event.getTo().getWorld() != null)
            updateNametagsInWorldAsync(event.getPlayer(), event.getTo().getWorld().getEntities());
    }

    private void updateNametagsInWorldAsync(final Player player, final List<Entity> entities) {
        final BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                updateNametagsInWorld(player, entities);
            }
        };

        runnable.runTaskAsynchronously(main);
    }

    private void updateNametagsInWorld(final Player player, @NotNull final List<Entity> entities) {
        final int currentPlayers = Bukkit.getOnlinePlayers().size();
        if (currentPlayers > main.maxPlayersRecorded)
            main.maxPlayersRecorded = currentPlayers;

        for (final Entity entity : entities) {
            if (!(entity instanceof LivingEntity)) continue;

            final LivingEntity livingEntity = (LivingEntity) entity;

            // mob must be alive
            if (!livingEntity.isValid()) continue;

            // mob must be levelled
            if (!main.levelManager.isLevelled(livingEntity)) continue;

            final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(livingEntity, main);

            main.levelManager.updateNametag(lmEntity, main.levelManager.getNametag(lmEntity, false), Collections.singletonList(player));
            lmEntity.free();
        }
    }

    void parseCompatibilityChecker(@NotNull final Player player) {
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

    void parseUpdateChecker(final Player player) {
        if (main.messagesCfg.getBoolean("other.update-notice.send-on-join", true) && player.hasPermission("levelledmobs.receive-update-notifications"))
            main.companion.updateResult.forEach(player::sendMessage);
    }
}
