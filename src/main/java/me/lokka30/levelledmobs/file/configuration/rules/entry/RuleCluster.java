/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.configuration.rules.entry;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;

public record RuleCluster(
        @NotNull String identifier,
        boolean enabled,
        @NotNull Optional<String> description,
        @NotNull ArrayList<RuleEntry> entries
) implements RuleEntry {}
