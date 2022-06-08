/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.lokka30.levelledmobs.LivingEntityInterface;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages communication to WorldGuar for the purposes of obtaining region and flag information
 *
 * @author Eyrian, lokka30, stumper66
 * @since 2.4.0
 */
public class WorldGuardIntegration {

    private WorldGuardIntegration() {
    }

    @NotNull
    public static List<String> getWorldGuardRegionsForLocation(
        @NotNull final LivingEntityInterface lmInterface) {
        final List<String> wg_Regions = new LinkedList<>();

        if (lmInterface.getWorld() == null) {
            return wg_Regions;
        }

        final Set<ProtectedRegion> regions = getRegionSet(lmInterface);
        if (regions == null) {
            return wg_Regions;
        }

        for (final ProtectedRegion region : regions) {
            wg_Regions.add(region.getId());
        }

        return wg_Regions;
    }

    @NotNull
    public static List<String> getWorldGuardRegionOwnersForLocation(
        @NotNull final LivingEntityInterface lmInterface) {
        final List<String> wg_Owners = new LinkedList<>();

        if (lmInterface.getWorld() == null) {
            return wg_Owners;
        }

        final Set<ProtectedRegion> regions = getRegionSet(lmInterface);
        if (regions == null) {
            return wg_Owners;
        }

        for (final ProtectedRegion region : regions) {
            if (region.getOwners().getPlayers() != null) {
                for (final UUID id : region.getOwners().getUniqueIds()) {
                    final Player player = Bukkit.getPlayer(id);
                    final String playerName = player == null ?
                        "unknown" : player.getName();
                    if (!wg_Owners.contains(playerName)) {
                        wg_Owners.add(playerName);
                    }
                }
            }
        }

        return wg_Owners;
    }

    @Nullable
    private static Set<ProtectedRegion> getRegionSet(
        @NotNull final LivingEntityInterface lmInterface) {
        final com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(lmInterface.getWorld());

        final BlockVector3 position = BlockVector3.at(
            lmInterface.getLocation().getBlockX(),
            lmInterface.getLocation().getBlockY(),
            lmInterface.getLocation().getBlockZ()
        );

        final RegionContainer container = WorldGuard.getInstance().getPlatform()
            .getRegionContainer();
        final RegionManager regions = container.get(world);
        if (regions == null) {
            return null;
        }

        return regions.getApplicableRegions(position).getRegions();
    }
}
