/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Baseclass for LivingEntityWrapper and LivingEntityPlaceholder to hold various information
 * about mobs
 *
 * @author stumper66
 * @since 3.1.0
 */
public class LivingEntityWrapperBase {

    public LivingEntityWrapperBase(final @NotNull LevelledMobs main, final @NotNull World world, final @NotNull Location location){
        this.world = world;
        this.location = location;
        this.main = main;
    }

    Double calculatedDistanceFromSpawn;
    @NotNull
    final World world;
    @NotNull
    final Location location;
    @NotNull
    final LevelledMobs main;

    public double getDistanceFromSpawn() {
        if (this.calculatedDistanceFromSpawn == null)
            this.calculatedDistanceFromSpawn = world.getSpawnLocation().distance(location);

        return calculatedDistanceFromSpawn;
    }

    @NotNull
    public LevelledMobs getMainInstance(){
        return this.main;
    }

    @NotNull
    public Location getLocation() {
        return this.location;
    }

    @NotNull
    public World getWorld() {
        return this.world;
    }

    @NotNull
    public String getWorldName(){
        return this.world.getName();
    }
}
