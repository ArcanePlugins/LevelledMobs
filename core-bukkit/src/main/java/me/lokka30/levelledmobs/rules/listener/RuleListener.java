/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules.listener;

import me.lokka30.levelledmobs.rules.RuleEntry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

public record RuleListener(
        @NotNull String                 listenerId,
        @NotNull Optional<String>       description,
        @NotNull HashSet<String>        listenFor,
        boolean                         ignoreCancelled,
        @NotNull ArrayList<RuleEntry>   entries
) {}
