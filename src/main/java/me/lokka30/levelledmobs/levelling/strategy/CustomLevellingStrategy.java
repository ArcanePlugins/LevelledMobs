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

/**
 * This class generates a level for a mob
 * based upon a formula provided by the administrator
 * in the Rules system. This Strategy can even obtain
 * calculated levels from other Strategies and use it inside
 * its own formula. For example, `spawn distance level + 2`,
 * as a simple example of what is capable with this Strategy.
 *
 * @author lokka30
 * @see LevellingStrategy
 * @since v4.0.0
 */
public class CustomLevellingStrategy implements LevellingStrategy {

    @Override
    public int calculateLevel(LevelledMobs main, LevelledMob mob) {
        //TODO
        return -1;
    }
}
