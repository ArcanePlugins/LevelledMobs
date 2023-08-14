/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * When in conjunction when a customdrops is being processed
 *
 * @author stumper66
 * @since 3.0.0
 */
public class CustomDropsRuleSet {

    public CustomDropsRuleSet() {
        this.useDropTableIds = new LinkedList<>();
    }

    public boolean useDrops;
    public ChunkKillOptions chunkKillOptions;
    public final @NotNull List<String> useDropTableIds;
}
