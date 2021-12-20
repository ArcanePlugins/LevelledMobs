/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.configuration.rules;

import me.lokka30.levelledmobs.file.configuration.rules.entry.RuleEntry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

public record RuleListener(
        @NotNull String listenerId,
        @NotNull Optional<String> description,
        @NotNull HashSet<String> listenFor,
        boolean ignoreCancelled,
        @NotNull ArrayList<RuleEntry> entries
) {}
