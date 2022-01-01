/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling.strategy;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.levelling.LevelledMob;
import org.jetbrains.annotations.NotNull;

/**
 * This class generates a level for a mob
 * based upon their Y-coordinate. The administrator
 * can configure it through the Rules system to increase
 * the level as mobs are spawned deeper underground, or
 * vice-versa.
 *
 * @author lokka30
 * @see LevellingStrategy
 * @since v4.0.0
 */
public record YAxisLevellingStrategy(
        int start,
        int end,
        int period,
        boolean inverse
) implements LevellingStrategy {

    @Override
    public int calculateLevel(@NotNull LevelledMobs main, @NotNull LevelledMob mob) {
        //TODO
        return -1;
    }
}
