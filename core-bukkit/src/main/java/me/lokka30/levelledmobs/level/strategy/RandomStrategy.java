/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.level.strategy;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.level.LevelledMob;
import me.lokka30.microlib.maths.Randoms;
import org.jetbrains.annotations.NotNull;

/**
 * This Levelling Strategy spits out a random number. That's it.
 * Administrators can set a min & max level of course.
 *
 * @author lokka30
 * @see LevellingStrategy
 * @since v4.0.0
 */
public record RandomStrategy(
        int minLevel,
        int maxLevel
) implements LevellingStrategy {

    @Override
    @NotNull
    public String getName() {
        return "RandomStrategy";
    }

    @Override
    public int calculateLevel(@NotNull LevelledMobs main, @NotNull LevelledMob mob) {
        return Randoms.generateRandomInt(minLevel, maxLevel);
    }
}
