/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.bukkit.level.strategy;

import java.util.HashSet;
import me.lokka30.levelledmobs.plugin.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.plugin.bukkit.level.LevelledMob;
import me.lokka30.levelledmobs.plugin.bukkit.util.Point;
import org.jetbrains.annotations.NotNull;

/**
 * This class generates a level for a mob based upon their location relative to the world's
 * spawnpoint OR a specific set of coordinates provided by the strategy's configuration in the Rules
 * system.
 *
 * @author lokka30
 * @see LevellingStrategy
 * @since 4.0.0
 */
public record DistanceFromSpawnStrategy(
    int minLevel,
    int maxLevel,
    int startingDistance,
    int increaseLevelDistance,
    @NotNull HashSet<Point> spawnLocations
) implements LevellingStrategy {

    @Override
    @NotNull
    public String getName() {
        return "DistanceFromSpawn";
    }

    @Override
    public int calculateLevel(@NotNull LevelledMobs main, @NotNull LevelledMob mob) {
        //TODO
        return -1;
    }
}
