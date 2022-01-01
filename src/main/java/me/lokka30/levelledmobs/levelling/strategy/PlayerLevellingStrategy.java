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
 * TODO document this class.
 */
public record PlayerLevellingStrategy(
        int test //TODO
) implements LevellingStrategy {

    @Override
    public int calculateLevel(@NotNull LevelledMobs main, @NotNull LevelledMob mob) {
        return 0;
    }
}
