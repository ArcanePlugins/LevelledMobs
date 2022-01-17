package me.lokka30.levelledmobs.rules.option;

import me.lokka30.levelledmobs.rules.Rule;
import org.jetbrains.annotations.NotNull;

public interface RuleOption {

    @NotNull
    DefaultRuleOptionType type();

    @NotNull
    Rule parentRule();

    @NotNull
    RuleOption merge(final @NotNull RuleOption other);
}
