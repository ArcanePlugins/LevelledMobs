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
import me.lokka30.levelledmobs.util.math.RangedInt;
import me.lokka30.levelledmobs.util.math.WeightedRandomContainer;
import org.jetbrains.annotations.NotNull;

/**
 * This class generates a random level, although
 * the numbers generated are biased towards the
 * minimum or maximum value, depending on however
 * the administrator has configured this strategy
 * in their Rules configuration.
 *
 * @author lokka30
 * @see LevellingStrategy
 * @since v4.0.0
 */
public record WeightedRandomStrategy(
        @NotNull WeightedRandomContainer<RangedInt> weightedRandomContainer
) implements LevellingStrategy {

    @Override
    @NotNull
    public String getName() {
        return "WeightedRandomStrategy";
    }

    @Override
    public int calculateLevel(@NotNull LevelledMobs main, @NotNull LevelledMob mob) {
        return weightedRandomContainer
                .getRandom() // get a random element from the container
                .generateRandom(); // get a random level from the RangedInt
    }
}
