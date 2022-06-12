/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules.strategies;

import me.lokka30.levelledmobs.misc.LivingEntityWrapper;

/**
 * Interface for the various levelling systems
 *
 * @author lokka30
 * @since 3.0.0
 */
public interface LevellingStrategy {

    int generateLevel(final LivingEntityWrapper lmEntity, final int minLevel, final int maxLevel);

    void mergeRule(final LevellingStrategy levellingStrategy);

    LevellingStrategy cloneItem();
}