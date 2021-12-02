/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling.strategy;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.levelling.LevelledMob;
import org.jetbrains.annotations.NotNull;

/**
 * @author lokka30
 * @since v4.0.0
 * This interface is used by internal (and even
 * external) Levelling Strategies that can be used
 * in LevelledMobs' Rules System. A Levelling Strategy
 * is code that determines what level a mob should be,
 * it can calculate numbers based off of conditions such
 * as the mob's location, it could be just a static number,
 * random numbers, and so on.
 */
public interface LevellingStrategy {

    /**
     * Requests the LevellingStrategy to calculate a mob's level
     *
     * @param mob the mob that is having their level calculated
     * @return the level calculated for specified mob
     */
    int calculateLevel(@NotNull LevelledMobs main, @NotNull LevelledMob mob);
}
