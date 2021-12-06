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
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * Manages communication to WorldGuar for the purposes of obtaining region
 * and flag information
 *
 * @author Eyrian, lokka30, stumper66
 * @since 2.4.0
 */
public class WorldGuardIntegration {
    private WorldGuardIntegration(){}

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
