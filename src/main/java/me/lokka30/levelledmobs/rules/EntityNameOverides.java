/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Holds rule information for entity names overrides
 *
 * @author stumper66
 * @since 3.1.0
 */
public class EntityNameOverides {
    public EntityNameOverides(final @NotNull String mobNameOrLevelRange) {
        this.mobNameOrLevelRange = mobNameOrLevelRange;
    }

    @NotNull
    public final String mobNameOrLevelRange;
    public List<LevelTierMatching> entityNames;
    public boolean isLevelRange;

}
