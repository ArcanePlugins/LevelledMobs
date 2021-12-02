/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling.strategy;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.levelling.LevelledMob;

/**
 * @author lokka30
 * @see LevellingStrategy
 * @since v4.0.0
 * This class generates a level for a mob
 * based upon their Y-coordinate. The administrator
 * can configure it through the Rules system to increase
 * the level as mobs are spawned deeper underground, or
 * vice-versa.
 */
public class YAxisLevellingStrategy implements LevellingStrategy {

    @Override
    public int calculateLevel(LevelledMobs main, LevelledMob mob) {
        //TODO
        return -1;
    }
}
