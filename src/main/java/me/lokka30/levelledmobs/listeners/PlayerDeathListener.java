/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Listens for when a player dies
 *
 * @author stumper66
 * @since 2.6.0
 */
public class PlayerDeathListener implements Listener {

    public PlayerDeathListener(final LevelledMobs main) {
        this.main = main;
        if (main.serverIsPaper)
            paperListener = new me.lokka30.levelledmobs.listeners.paper.PlayerDeathListener(main);
    }

    final private LevelledMobs main;
    private me.lokka30.levelledmobs.listeners.paper.PlayerDeathListener paperListener;

    /**
     * This listener handles death nametags so we can determine which mob killed
     * it and update the death message accordingly
     *
     * @param event PlayerDeathEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDeath(@NotNull final PlayerDeathEvent event) {
        // returns false if not a translatable component, in which case just use the old method
        // this can happen if another plugin has buthered the event by using the deprecated method (*cough* mythic mobs)
        if (!main.serverIsPaper || !paperListener.onPlayerDeathEvent(event))
            nonPaper_PlayerDeath(event);
    }

    private void nonPaper_PlayerDeath(@NotNull final PlayerDeathEvent event){
        final LivingEntityWrapper lmEntity = getPlayersKiller(event);

        if (main.placeholderApiIntegration != null){
            main.placeholderApiIntegration.putPlayerOrMobDeath(event.getEntity(), lmEntity);
            return;
        }

        if (lmEntity != null)
            lmEntity.free();
    }

    @Nullable
    private LivingEntityWrapper getPlayersKiller(@NotNull final PlayerDeathEvent event){
        // ignore any deprecated events.  they are not deprecated in spigot api
        if (event.getDeathMessage() == null) return null;

        final EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
        if (entityDamageEvent == null || entityDamageEvent.isCancelled() || !(entityDamageEvent instanceof EntityDamageByEntityEvent))
            return null;

        final Entity damager = ((EntityDamageByEntityEvent) entityDamageEvent).getDamager();
        LivingEntity killer;

        if (damager instanceof Projectile)
            killer = (LivingEntity) ((Projectile) damager).getShooter();
        else if (!(damager instanceof LivingEntity))
            return null;
        else
            killer = (LivingEntity) damager;

        if (killer == null || Utils.isNullOrEmpty(killer.getName()) || killer instanceof Player) return null;

        final LivingEntityWrapper lmKiller = LivingEntityWrapper.getInstance(killer, main);
        if (!lmKiller.isLevelled())
            return lmKiller;

        final String deathMessage = main.levelManager.getNametag(lmKiller, true);
        if (Utils.isNullOrEmpty(deathMessage) || "disabled".equalsIgnoreCase(deathMessage))
            return lmKiller;

        event.setDeathMessage(Utils.replaceEx(event.getDeathMessage(), killer.getName(), deathMessage));
        return lmKiller;
    }
}
