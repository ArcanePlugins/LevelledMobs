/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.rules.action.RuleAction;
import me.lokka30.levelledmobs.rules.condition.RuleConditionHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;

public record Rule(
        @NotNull String                 identifier,
        @NotNull Optional<String>       description,
        @NotNull HashSet<RuleConditionHandler> conditions,
        @NotNull HashSet<RuleAction>    actions,
        @NotNull HashSet<Rule>          presets,
        boolean                         onceProcessedStopProcessing
) implements RuleEntry {}
