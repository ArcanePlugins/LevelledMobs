/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
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
public record WeightedRandomLevellingStrategy(
        @NotNull WeightedRandomContainer<RangedInt> weightedRandomContainer
) implements LevellingStrategy {

    @Override
    public int calculateLevel(LevelledMobs main, LevelledMob mob) {
        return weightedRandomContainer
                .getRandom() // get a random element from the container
                .generateRandom(); // get a random level from the RangedInt
    }
}
