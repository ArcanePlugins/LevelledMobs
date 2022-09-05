/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

import java.util.concurrent.atomic.AtomicInteger;
import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Baseclass for LivingEntityWrapper and LivingEntityPlaceholder to hold various information about
 * mobs
 *
 * @author stumper66
 * @since 3.1.0
 */
public abstract class LivingEntityWrapperBase {

    LivingEntityWrapperBase(final @NotNull LevelledMobs main) {
        this.main = main;
        this.inUseCount = new AtomicInteger();
    }

    private Double calculatedDistanceFromSpawn;
    private World world;
    private Location location;
    @NotNull final LevelledMobs main;
    Integer summonedLevel;
    Integer spawnedTimeOfDay;
    private boolean isPopulated;
    public final AtomicInteger inUseCount;

    void populateData(final @NotNull World world, final @NotNull Location location) {
        this.world = world;
        this.location = location;
        this.isPopulated = true;
    }

    void clearEntityData() {
        this.world = null;
        this.location = null;
        this.calculatedDistanceFromSpawn = null;
        this.spawnedTimeOfDay = null;
        this.inUseCount.set(0);
        this.isPopulated = false;
        this.summonedLevel = null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean getIsPopulated() {
        return isPopulated;
    }

    public double getDistanceFromSpawn() {
        if (this.calculatedDistanceFromSpawn == null) {
            this.calculatedDistanceFromSpawn = world.getSpawnLocation().distance(location);
        }

        return calculatedDistanceFromSpawn;
    }

    @NotNull public LevelledMobs getMainInstance() {
        return this.main;
    }

    @NotNull public Location getLocation() {
        if (this.location == null) {
            throw new NullPointerException("Location was null");
        }

        return this.location;
    }

    @NotNull public World getWorld() {
        if (this.world == null) {
            throw new NullPointerException("World was null");
        }

        return this.world;
    }

    @NotNull public String getWorldName() {
        if (this.world == null) {
            throw new NullPointerException("World was null");
        }

        return this.world.getName();
    }

    public void setSummonedLevel(final Integer summonedLevel) {
        this.summonedLevel = summonedLevel;
    }
}
