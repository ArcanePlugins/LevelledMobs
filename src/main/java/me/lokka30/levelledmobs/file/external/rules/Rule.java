/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external.rules;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;

public record Rule(
        @NotNull String identifier,
        @NotNull Optional<String> description,
        @NotNull HashSet<RuleCondition> conditions,
        @NotNull HashSet<RuleAction> actions,
        @NotNull HashSet<RulePreset> presets,
        boolean onceProcessedStopProcessing
) implements RuleEntry {}
