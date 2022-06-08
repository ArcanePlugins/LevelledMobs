/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

/**
 * Holds values parsed from rules.yml
 *
 * @author stumper66
 * @since 3.0.0
 */
public enum MobTamedStatus {
    NOT_SPECIFIED,  // default
    TAMED,          // Mob must be tamed for the rule to work
    NOT_TAMED,      // Mob must be not tamed for the rule to work
    EITHER          // Doesn't matter what the tamed status of the mob is
}
