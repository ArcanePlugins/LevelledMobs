/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.bukkit.rule;

import java.util.HashSet;
import java.util.Optional;
import me.lokka30.levelledmobs.plugin.bukkit.rule.action.RuleAction;
import me.lokka30.levelledmobs.plugin.bukkit.rule.condition.RuleCondition;
import me.lokka30.levelledmobs.plugin.bukkit.rule.option.RuleOption;
import org.jetbrains.annotations.NotNull;

public record Rule(
    boolean isPreset,
    @NotNull String identifier,
    @NotNull Optional<String> description,
    @NotNull HashSet<RuleCondition> conditions,
    @NotNull HashSet<RuleAction> actions,
    @NotNull HashSet<RuleOption> options,
    @NotNull HashSet<Rule> presets // Note: Preset rules can't have sub-presets!
) implements RuleEntry {

    // The otherRule has lower priority.
    @NotNull
    public Rule merge(final @NotNull Rule otherRule) {
        // merge conditions
        for(RuleCondition otherCondition : otherRule.conditions()) {
            final Optional<RuleCondition> existingCondition = conditions().stream()
                .filter(val -> val.id().equals(otherCondition.id())).findFirst();
            if(existingCondition.isPresent()) {
                existingCondition.get().merge(otherCondition);
            } else {
                conditions().add(otherCondition);
            }
        }

        // merge actions
        for(RuleAction otherAction : otherRule.actions()) {
            final Optional<RuleAction> existingAction = actions().stream()
                .filter(val -> val.id().equals(otherAction.id())).findFirst();
            if(existingAction.isPresent()) {
                existingAction.get().merge(otherAction);
            } else {
                actions().add(otherAction);
            }
        }

        // merge options
        for(RuleOption otherOption : otherRule.options()) {
            final Optional<RuleOption> existingOption = options().stream()
                .filter(val -> val.type() == otherOption.type()).findFirst();
            if(existingOption.isPresent()) {
                existingOption.get().merge(otherOption);
            } else {
                options().add(otherOption);
            }
        }

        // merge presets
        if(!isPreset()) {
            for(Rule otherPreset : otherRule.presets()) {
                final Optional<Rule> existingPreset = presets().stream()
                    .filter(val -> val.identifier().equals(otherPreset.identifier())).findFirst();
                if(existingPreset.isEmpty()) {
                    presets().add(otherPreset);
                }
            }
        }

        //TODO
        return null;
    }
}
