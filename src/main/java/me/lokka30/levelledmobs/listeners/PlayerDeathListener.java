/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.util.SpigotUtils;
import me.lokka30.microlib.other.VersionUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for when a player dies
 *
 * @author stumper66
 * @since 2.6.0
 */
public class PlayerDeathListener implements Listener {

    public PlayerDeathListener(final LevelledMobs main) {
        this.main = main;
        if (VersionUtils.isRunningPaper()) {
            paperListener = new me.lokka30.levelledmobs.listeners.paper.PlayerDeathListener(main);
        }
    }

    final private LevelledMobs main;
    private me.lokka30.levelledmobs.listeners.paper.PlayerDeathListener paperListener;

    /**
     * This listener handles death nametags so we can determine which mob killed it and update the
     * death message accordingly
     *
     * @param event PlayerDeathEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDeath(@NotNull final PlayerDeathEvent event) {
        // returns false if not a translatable component, in which case just use the old method
        // this can happen if another plugin has buthered the event by using the deprecated method (*cough* mythic mobs)
        if (!VersionUtils.isRunningPaper() || !paperListener.onPlayerDeathEvent(event)) {
            nonPaper_PlayerDeath(event);
        }
    }

    private void nonPaper_PlayerDeath(@NotNull final PlayerDeathEvent event) {
        final LivingEntityWrapper lmEntity = SpigotUtils.getPlayersKiller(event, main);

        if (main.placeholderApiIntegration != null) {
            main.placeholderApiIntegration.putPlayerOrMobDeath(event.getEntity(), lmEntity);
            return;
        }

        if (lmEntity != null) {
            lmEntity.free();
        }
    }
}
