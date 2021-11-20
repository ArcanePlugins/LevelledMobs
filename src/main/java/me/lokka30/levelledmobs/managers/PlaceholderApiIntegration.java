/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LastMobKilledInfo;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Manages communication to PlaceholderAPI (PAPI)
 *
 * @author stumper66
 * @since 3.0.0
 */
public class PlaceholderApiIntegration extends PlaceholderExpansion {

    public PlaceholderApiIntegration(final LevelledMobs main) {
        this.main = main;
        this.mobsByPlayerTracking = new TreeMap<>();
    }

    private final LevelledMobs main;
    private final Map<UUID, LastMobKilledInfo> mobsByPlayerTracking;

    public void putPlayerOrMobDeath(final @NotNull Player player, final @Nullable LivingEntityWrapper lmEntity) {
        LastMobKilledInfo mobInfo = this.mobsByPlayerTracking.get(player.getUniqueId());
        if (mobInfo == null){
            mobInfo = new LastMobKilledInfo();
            this.mobsByPlayerTracking.put(player.getUniqueId(), mobInfo);
        }

        mobInfo.entityLevel = lmEntity != null && lmEntity.isLevelled() ?
                lmEntity.getMobLevel() : null;

        mobInfo.entityName = lmEntity != null ?
                main.levelManager.getNametag(lmEntity, false) : null;
    }

    public void playedLoggedOut(final @NotNull Player player){
        this.mobsByPlayerTracking.remove(player.getUniqueId());
    }

    public void removePlayer(final @NotNull Player player){
        this.mobsByPlayerTracking.remove(player.getUniqueId());
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return main.getDescription().getName();
    }

    @Override
    public @NotNull String getAuthor() {
        return main.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return main.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(final Player player, final @NotNull String identifier){
        if (player == null) return "";

        if ("mob-lvl".equalsIgnoreCase(identifier))
            return getLevelFromPlayer(player);
        else if ("displayname".equalsIgnoreCase(identifier))
            return getDisplaynameFromPlayer(player);

        return null;
    }

    @NotNull
    private String getLevelFromPlayer(final @NotNull Player player){
        if (!this.mobsByPlayerTracking.containsKey(player.getUniqueId())) return "";

        final LastMobKilledInfo mobInfo = this.mobsByPlayerTracking.get(player.getUniqueId());
        return mobInfo.entityLevel == null ?
                "" : String.valueOf(mobInfo.entityLevel);
    }

    @NotNull
    private String getDisplaynameFromPlayer(final @NotNull Player player){
        if (!this.mobsByPlayerTracking.containsKey(player.getUniqueId())) return "";

        final LastMobKilledInfo mobInfo = this.mobsByPlayerTracking.get(player.getUniqueId());
        return mobInfo == null || mobInfo.entityName == null ?
            "" : mobInfo.entityName;
    }
}
