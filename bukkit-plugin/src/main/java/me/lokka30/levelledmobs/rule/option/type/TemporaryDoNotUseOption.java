package me.lokka30.levelledmobs.rule.option.type;

import me.lokka30.levelledmobs.rule.Rule;
import me.lokka30.levelledmobs.rule.option.DefaultRuleOptionType;
import me.lokka30.levelledmobs.rule.option.RuleOption;
import org.jetbrains.annotations.NotNull;

public record TemporaryDoNotUseOption(
        @NotNull Rule parentRule
) implements RuleOption {

    @Override @NotNull
    public DefaultRuleOptionType type() {
        return DefaultRuleOptionType.TEMPORARY_DO_NOT_USE;
    }

    @Override @NotNull
    public RuleOption merge(@NotNull RuleOption other) {
        return other;
    }
}