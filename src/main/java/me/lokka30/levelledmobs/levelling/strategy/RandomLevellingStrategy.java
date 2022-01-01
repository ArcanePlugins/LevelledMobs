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
import me.lokka30.microlib.maths.Randoms;

/**
 * This Levelling Strategy spits out a random number. That's it.
 * Administrators can set a min & max level of course.
 *
 * @author lokka30
 * @see LevellingStrategy
 * @since v4.0.0
 */
public class RandomLevellingStrategy implements LevellingStrategy {

    @Override
    public int calculateLevel(LevelledMobs main, LevelledMob mob) {

        int min = 1; //TODO Configurable
        int max = 100; //TODO Configurable

        return Randoms.generateRandomInt(min, max);
    }
}
