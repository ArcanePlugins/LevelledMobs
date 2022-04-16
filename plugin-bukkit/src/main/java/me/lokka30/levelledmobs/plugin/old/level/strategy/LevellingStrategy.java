/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.old.level.strategy;

import me.lokka30.levelledmobs.plugin.old.LevelledMobs;
import me.lokka30.levelledmobs.plugin.old.level.LevelledMob;
import org.jetbrains.annotations.NotNull;

/**
 * This interface is used by internal (and even external) Levelling Strategies that can be used in
 * LevelledMobs' Rules System. A Levelling Strategy is code that determines what level a mob should
 * be, it can calculate numbers based off of conditions such as the mob's location, it could be just
 * a static number, random numbers, and so on.
 *
 * @author lokka30
 * @since 4.0.0
 */
public interface LevellingStrategy {

    @NotNull
    String getName();

    /**
     * Requests the LevellingStrategy to calculate a mob's level
     *
     * @param mob the mob that is having their level calculated
     * @return the level calculated for specified mob
     * @since 4.0.0
     */
    int calculateLevel(@NotNull LevelledMobs main, @NotNull LevelledMob mob);
}
