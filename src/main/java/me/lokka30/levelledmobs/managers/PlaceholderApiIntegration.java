/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LastMobKilledInfo;
import me.lokka30.levelledmobs.misc.StringReplacer;
import me.lokka30.levelledmobs.util.MessageUtils;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        this.playerDeathInfo = new TreeMap<>();
    }

    private final LevelledMobs main;
    private final Map<UUID, LastMobKilledInfo> mobsByPlayerTracking;
    private final Map<UUID, LastMobKilledInfo> playerDeathInfo;

    public void putPlayerOrMobDeath(final @NotNull Player player,
        final @Nullable LivingEntityWrapper lmEntity, final boolean isPlayerDeath) {
        LastMobKilledInfo mobInfo = this.mobsByPlayerTracking.computeIfAbsent(
                player.getUniqueId(), k -> new LastMobKilledInfo());

        mobInfo.entityLevel = lmEntity != null && lmEntity.isLevelled() ?
            lmEntity.getMobLevel() : null;

        mobInfo.entityName = lmEntity != null ?
            main.levelManager.getNametag(lmEntity, false).getNametag() : null;

        if (isPlayerDeath)
            putPlayerKillerInfo(player, lmEntity);
    }

    public void putPlayerKillerInfo(final @NotNull Player player,
                                    final @Nullable LivingEntityWrapper lmEntity) {
        LastMobKilledInfo mobInfo = new LastMobKilledInfo();
        this.playerDeathInfo.put(player.getUniqueId(), mobInfo);

        mobInfo.entityLevel = lmEntity != null && lmEntity.isLevelled() ?
                lmEntity.getMobLevel() : null;

        mobInfo.entityName = lmEntity != null ?
                main.levelManager.getNametag(lmEntity, false).getNametag() : null;
    }

    public void playedLoggedOut(final @NotNull Player player) {
        this.mobsByPlayerTracking.remove(player.getUniqueId());
    }

    public void removePlayer(final @NotNull Player player) {
        this.mobsByPlayerTracking.remove(player.getUniqueId());
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
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
    public String onPlaceholderRequest(final Player player, final @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        if ("mob-lvl".equalsIgnoreCase(identifier)) {
            return getLevelFromPlayer(player);
        } else if ("displayname".equalsIgnoreCase(identifier)) {
            return getDisplaynameFromPlayer(player);
        } else if ("mob-target".equalsIgnoreCase(identifier)) {
            return getMobNametagWithinPlayerSight(player);
        }
        else if ("killed-by".equalsIgnoreCase(identifier)){
            return getKilledByInfo(player);
        }

        return null;
    }

    private @NotNull String getLevelFromPlayer(final @NotNull Player player) {
        if (!this.mobsByPlayerTracking.containsKey(player.getUniqueId())) {
            return "";
        }

        final LastMobKilledInfo mobInfo = this.mobsByPlayerTracking.get(player.getUniqueId());
        return mobInfo.entityLevel == null ?
            "" : String.valueOf(mobInfo.entityLevel);
    }

    private @NotNull String getDisplaynameFromPlayer(final @NotNull Player player) {
        if (!this.mobsByPlayerTracking.containsKey(player.getUniqueId())) {
            return "";
        }

        final LastMobKilledInfo mobInfo = this.mobsByPlayerTracking.get(player.getUniqueId());
        return mobInfo == null || mobInfo.entityName == null ?
            "" : mobInfo.entityName + "&r";
    }

    private @NotNull String getKilledByInfo(final @NotNull Player player) {
        if (!this.playerDeathInfo.containsKey(player.getUniqueId())) {
            return "";
        }

        final LastMobKilledInfo mobInfo = this.playerDeathInfo.get(player.getUniqueId());
        return mobInfo == null || mobInfo.entityName == null ?
                "" : MessageUtils.colorizeAll(mobInfo.entityName + "&r");
    }

    private @NotNull String getMobNametagWithinPlayerSight(final @Nullable Player player) {
        if (player == null) {
            return "";
        }

        final LivingEntity targetMob = getMobBeingLookedAt(player);
        if (targetMob == null) {
            return "";
        }

        final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(targetMob, main);
        String nametag = main.rulesManager.getRuleNametagPlaceholder(lmEntity);
        if (!Utils.isNullOrEmpty(nametag)) {
            final boolean useCustomNameForNametags = main.helperSettings.getBoolean(
                main.settingsCfg, "use-customname-for-mob-nametags");
            nametag = main.levelManager.updateNametag(lmEntity, new StringReplacer(nametag),
                    useCustomNameForNametags, null).getNametagNonNull();

            if ("disabled".equalsIgnoreCase(nametag)) {
                return "";
            }
        }

        if (Utils.isNullOrEmpty(nametag) && lmEntity.isLevelled()) {
            nametag = main.levelManager.getNametag(lmEntity, false).getNametag() + "&r";
        }

        lmEntity.free();

        if (nametag != null){
            if (nametag.endsWith("&r")){
                nametag = nametag.substring(0, nametag.length() - 2);
            }
            return nametag;
        }
        else{
            return "";
        }
    }

    private @Nullable LivingEntity getMobBeingLookedAt(final @NotNull Player player) {
        LivingEntity livingEntity = null;
        final Location eye = player.getEyeLocation();
        final int maxBlocks = main.helperSettings.getInt(main.settingsCfg,
            "nametag-placeholder-maxblocks", 30);

        for (final Entity entity : player.getNearbyEntities(maxBlocks, maxBlocks, maxBlocks)) {
            if (!(entity instanceof final LivingEntity le)) {
                continue;
            }

            final Vector toEntity = le.getEyeLocation().toVector().subtract(eye.toVector());
            final double dot = toEntity.normalize().dot(eye.getDirection());
            if (dot >= 0.975D) {
                livingEntity = le;
                break;
            }
        }

        return livingEntity;
    }
}
