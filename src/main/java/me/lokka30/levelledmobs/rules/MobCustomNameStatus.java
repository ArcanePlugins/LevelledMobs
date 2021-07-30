/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

/**
 * Holds values parsed from rules.yml
 *
 * @author stumper66
 */
public enum MobCustomNameStatus {
    NOT_SPECIFIED,  // default
    NAMETAGGED,     // Mob must be nametagged for the rule to work
    NOT_NAMETAGGED, // Mob must be not nametagged for the rule to work
    EITHER          // Doesn't matter what the nametag status of the mob is
}
