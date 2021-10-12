/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.lokka30.levelledmobs.LivingEntityInterface;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Manages communication to WorldGuar for the purposes of obtaining region
 * and flag information
 *
 * @author Eyrian, lokka30, stumper66
 * @since 2.4.0
 */
public class WorldGuardIntegration {

    public WorldGuardIntegration() {

    }

    //Get all regions at an Entities' location.
    @NotNull
    public List<ProtectedRegion> getRegionSet(@NotNull final LivingEntityInterface lmInterface) {
        final List<ProtectedRegion> results = new LinkedList<>();
        final Location location = lmInterface.getLocation();

        final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(lmInterface.getWorld()));

        if (regionManager == null) return results;

        final ProtectedRegion globalRegion = regionManager.getRegion("__global__");
        if (location.getWorld() == null) {
            if (globalRegion != null) results.add(globalRegion);
            return results;
        }

        final BlockVector3 blockVector = BlockVector3.at(location.getX(), location.getY(), location.getZ());
        if (globalRegion != null) results.add(globalRegion);
        for (final ProtectedRegion region : regionManager.getApplicableRegions(blockVector))
            results.add(region);

        return results;
    }

    // Get all regions at a location
    @NotNull
    public List<ProtectedRegion> getRegionSet(@NotNull final Location location) {
        final List<ProtectedRegion> results = new LinkedList<>();
        if (location.getWorld() == null) return results;

        final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(location.getWorld()));

        if (regionManager == null) return results;

        final ProtectedRegion globalRegion = regionManager.getRegion("__global__");
        if (location.getWorld() == null) {
            if (globalRegion != null) results.add(globalRegion);
            return results;
        }

        final BlockVector3 blockVector = BlockVector3.at(location.getX(), location.getY(), location.getZ());
        final ApplicableRegionSet regionSet = regionManager.getApplicableRegions(blockVector);
        if (globalRegion != null) results.add(globalRegion);
        for (final ProtectedRegion region : regionSet)
            results.add(region);

        return results;
    }


    //Sorts a RegionSet by priority, lowest to highest.
    @Nullable
    public ProtectedRegion[] sortRegionsByPriority(final List<ProtectedRegion> regionSet) {
        if (regionSet == null) return null;

        ProtectedRegion[] protectedRegions = new ProtectedRegion[0];

        if (regionSet.size() == 0) {
            return protectedRegions;
        } else if (regionSet.size() == 1) {
            protectedRegions = new ProtectedRegion[1];
            return regionSet.toArray(protectedRegions);
        }

        final List<ProtectedRegion> protectedRegionList = new ArrayList<>(regionSet);
        protectedRegionList.sort(Comparator.comparingInt(ProtectedRegion::getPriority));

        return protectedRegionList.toArray(protectedRegions);
    }

    @NotNull
    public static List<String> getWorldGuardRegionsForLocation(@NotNull final LivingEntityInterface lmInterface) {
        final List<String> wg_Regions = new LinkedList<>();

        if (lmInterface.getWorld() == null) return wg_Regions;
        final com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(lmInterface.getWorld());

        final BlockVector3 position = BlockVector3.at(
                lmInterface.getLocation().getBlockX(),
                lmInterface.getLocation().getBlockY(),
                lmInterface.getLocation().getBlockZ()
        );

        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionManager regions = container.get(world);
        if (regions == null) return wg_Regions;

        final ApplicableRegionSet set = regions.getApplicableRegions(position);
        for (final ProtectedRegion region : set)
            wg_Regions.add(region.getId());

        return wg_Regions;
    }
}
